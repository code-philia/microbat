package microbat.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import microbat.codeanalysis.runtime.Condition;
import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.StepLimitException;
import microbat.evaluation.junit.TestCaseAnalyzer;
import microbat.instrumentation.output.RunningInfo;
import microbat.log.Log;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.preference.AnalysisScopePreference;
import microbat.tracerecov.TraceRecovUtils;
import microbat.tracerecov.TraceRecoverer;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import sav.strategies.dto.AppJavaClassPath;
import sav.common.core.Pair;

public class ComponentEvaluationHandler extends StartDebugHandler {
	private int matchedNum;
	private int notPredictedNum;
	private int wrongPredictionNum;
	
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job(DebugPilotHandler.JOB_FAMALY_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				execute();
				return Status.OK_STATUS;
			}

			@Override
			public boolean belongsTo(Object family) {
				return this.getName().equals(family);
			}
		};
		job.schedule();
		return null;
	}

	protected void execute() {
		Log.printMsg(getClass(), "");
		Log.printMsg(getClass(), "=====================================");
		Log.printMsg(getClass(), "Component Evaluation of Tracerecov");
		Log.printMsg(getClass(), "=====================================");
		Log.printMsg(getClass(), "");

		final AppJavaClassPath appClassPath = MicroBatUtil.constructClassPaths();
		if (Settings.isRunTest) {
			appClassPath.setOptionalTestClass(Settings.launchClass);
			appClassPath.setOptionalTestMethod(Settings.testMethod);
			appClassPath.setLaunchClass(TestCaseAnalyzer.TEST_RUNNER);
			appClassPath.setTestCodePath(MicroBatUtil.getSourceFolder(Settings.launchClass, Settings.projectName));
		}
		List<String> srcFolders = MicroBatUtil.getSourceFolders(Settings.projectName);
		appClassPath.setSourceCodePath(appClassPath.getTestCodePath());
		for (String srcFolder : srcFolders) {
			if (!srcFolder.equals(appClassPath.getTestCodePath())) {
				appClassPath.getAdditionalSourceFolders().add(srcFolder);
			}
		}

		Trace trace = this.generateTrace(appClassPath, null);

		String className = Settings.launchClass.substring(Settings.launchClass.indexOf(".") + 1);
		ObjectMapper objectMapper = new ObjectMapper();
		Path bmPath = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_benchmark", className + ".json");
//		Path gtPath = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_groundtruth", className + ".json");

		List<Map<String, String>> benchmark = null;
		Map<String, List<String>> groundtruth = null;
		try {
			benchmark = objectMapper.readValue(new File(bmPath.toString()),
					new TypeReference<List<Map<String, String>>>() {
					});
//			groundtruth = objectMapper.readValue(new File(gtPath.toString()),
//					new TypeReference<Map<String, List<String>>>() {
//					});
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (benchmark == null) {
			Log.printMsg(getClass(), "ERROR: load benchmark or groundtruth failed");
			return;
		}

		matchedNum = 0;
		notPredictedNum = 0;
		wrongPredictionNum = 0;
		
		evaluateComponent(appClassPath,trace, benchmark, groundtruth);

	}

	protected Trace generateTrace(final AppJavaClassPath appClassPath, Condition condition) {
		List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
		List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
		InstrumentationExecutor executor;
		if (condition == null) {
			executor = new InstrumentationExecutor(appClassPath, generateTraceDir(appClassPath), "trace",
					includedClassNames, excludedClassNames);
		} else {
			executor = new InstrumentationExecutor(appClassPath, generateTraceDir(appClassPath), "trace",
					includedClassNames, excludedClassNames, condition);
		}

		try {
			final RunningInfo result = executor.run();
			if (result == null) {
				return null;
			}
			Trace trace = result.getMainTrace();
			trace.setAppJavaClassPath(appClassPath);
			return trace;
		} catch (StepLimitException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected void evaluateComponent(final AppJavaClassPath appClassPath ,Trace trace, List<Map<String, String>> benchmark,
			Map<String, List<String>> groundtruth) {
		// var-expansion
		Set<String> checkedVar = new HashSet<String>();
		List<Double> precisionList = new ArrayList<Double>();
		List<Double> recallList = new ArrayList<Double>();

		// def-inference
		Map<Integer, Set<Integer>> predictedDataDep = new HashMap<>();
		int succ = 0;

		// for each selected data dependency to be recovered
		for (Map<String, String> element : benchmark) {
			int currentOrder = Integer.valueOf(element.get("Order"));
			TraceNode currentStep = trace.getExecutionList().get(currentOrder - 1);

			// variable expansion score
			VarValue readVar = null;
			for (VarValue var : currentStep.getReadVariables()) {
				if (var.getVarName().equals(element.get("Name"))) {
					String gtExpansion = element.get("Expansion");
					String prediction = null;

					ExecutionSimulator simulator = new ExecutionSimulator();
					try {
						Log.printMsg(getClass(), "Getting expansion prediction at var: "+var.toString()+", on step "+currentStep.getOrder());
						prediction = simulator.expandVariable(var, currentStep, null);
					} catch (IOException e) {
						e.printStackTrace();
					}

					if (prediction == null) {
						Log.printMsg(getClass(), "ERROR: no proper prediction!");
						break;
					}
					Pair<Double, Double> pr = calculatePR(gtExpansion, prediction);
					precisionList.add(pr.first());
					recallList.add(pr.second());
					checkedVar.add(var.getVarName() + ":" + var.getStringValue());

					readVar = var;
					break;
				}
			}

			// definition inference precision & score(pr)
			if (!predictedDataDep.containsKey(currentOrder)) {
				TraceRecoverer traceRecoverer = new TraceRecoverer();
				Set<Integer> dataDominatorsAfterRecovery = new HashSet<>();
				
				Condition condition = new Condition(readVar.getVarName(), readVar.getType(), readVar.getStringValue(), "null");
				System.out.println("Re-execution of condition: "+condition);
				Trace newTrace = generateTrace(appClassPath, condition);
				VarValue varExpanded = condition.getMatchedGroundTruthVar(newTrace);
				if(varExpanded == null) {
					continue;
				}

				for (VarValue targetVar : varExpanded.getAllDescedentChildren()) {
					traceRecoverer.recoverDataDependency(currentStep, targetVar, readVar);
					TraceNode dataDominator = trace.findProducer(readVar, currentStep);
					if (dataDominator != null) {
						dataDominatorsAfterRecovery.add(dataDominator.getOrder());
					}
				}

				predictedDataDep.put(currentOrder, dataDominatorsAfterRecovery);

			}
			Set<Integer> dds = predictedDataDep.get(currentOrder);
			if (dds.contains(Integer.valueOf(element.get("Dependency")))) {
				succ += 1;
			}
		}
		
		StringBuilder result = new StringBuilder();
		result.append("Checked var_value number:"+checkedVar.size()+"\n");
		result.append("Variable expansion precision list:"+precisionList.toString()+"\n");
		result.append("Variable expansion average precision:"+getAvg(precisionList)+"\n");
		result.append("Variable expansion recall list:"+recallList.toString()+"\n");
		result.append("Variable expansion average recall:"+getAvg(recallList)+"\n");
		
		int total = benchmark.size();
		double rate = (double)succ/total;
		result.append("Checked data dependency number:"+total+"\n");
		result.append("Definition inference successfull number:"+succ+"\n");
		result.append("Definition inference successfull rate:"+rate+"\n");
		
		System.out.println(result.toString());
	}

	protected Pair<Double, Double> calculatePR(String groundtruth, String prediction) {
		// parse to json object
		System.out.println("Original string: ");
		System.out.println("groundtruth: "+groundtruth);
		System.out.println("prediction: "+prediction);
		
		JSONObject groundtruthJson = processResponse(groundtruth);
		JSONObject predictionJson = processResponse(prediction);
		
//		Object expansion1 = null;
//		for(String key : groundtruthJson.keySet()) {
//			expansion1 = groundtruthJson.get(key);
//			break; // only contains one (root var)
//		}
//		
//		Object expansion2 = null;
//		for(String key : predictionJson.keySet()) {
//			expansion2 = predictionJson.get(key);
//			break;
//		}
		
		matchJsonTree(groundtruthJson,predictionJson);
		
		double precision = 0;
		if (matchedNum + wrongPredictionNum != 0) {
			precision = (double) matchedNum / (matchedNum + wrongPredictionNum);
		}
		double recall = 0.0;
		if (matchedNum + notPredictedNum != 0) {
			recall = (double) matchedNum / (matchedNum + notPredictedNum);
		}
		
		System.out.println("PR: "+precision+", "+recall );
		
		return Pair.of(precision, recall);
	}
	
	
	protected JSONObject processResponse(String response) {
		int begin = response.indexOf("{");
		int end = response.lastIndexOf("}");
		response = response.substring(begin, end + 1);
		JSONObject variable = new JSONObject(response);
		return variable;
	}

	protected void matchJsonTree(Object json1, Object json2) {
		if(json1 instanceof JSONObject && json2 instanceof JSONObject) {
			Set<String> keySet1 = ((JSONObject)json1).keySet();
			Set<String> keySet2 = ((JSONObject)json2).keySet();
			Set<String> matched1 = new HashSet<String>();
			Set<String> matched2 = new HashSet<String>();

			for(String key1 : keySet1) {
				for(String key2 : keySet2) {
					if(matched2.contains(key2)) {
						continue;
					}
					// successfully matched
					if(nameTypeMatch(key1,key2)) {
						matchedNum+=1;
						matched1.add(key1);
						matched2.add(key2);
						matchJsonTree(((JSONObject)json1).get(key1),((JSONObject)json2).get(key2));
						break;
					}
				}
			}
			
			for(String key1 : keySet1) {
				if(!matched1.contains(key1)) {
					notPredictedNum+=1;
					processNotMatchedNode(((JSONObject)json1).get(key1), true);
				}
			}
			for(String key2 : keySet2) {
				if(!matched2.contains(key2)) {
					wrongPredictionNum+=1;
					processNotMatchedNode(((JSONObject)json2).get(key2), false);
				}
			}
			
		}
		else if(json1 instanceof JSONObject) {
			notPredictedNum += ((JSONObject)json1).keySet().size();
			for(String key : ((JSONObject)json1).keySet()) {
				processNotMatchedNode(((JSONObject)json1).get(key),true);
			}
		}
		else if(json2 instanceof JSONObject) {
			wrongPredictionNum += ((JSONObject)json2).keySet().size();
			for(String key : ((JSONObject)json2).keySet()) {
				processNotMatchedNode(((JSONObject)json2).get(key),false);
			}
		}
		
	}
	
	protected double getAvg(List<Double> precisionList) {
		double sum = 0.0;
        if (!precisionList.isEmpty()) {
            for (Double number : precisionList) {
                sum += number;
            }
            double average = sum / precisionList.size();
            return average;
        } else {
            return 0.0;
        }
	}
	
	protected boolean nameTypeMatch(String key1, String key2) {
		String[] name_type1 = key1.split("|");
		String[] name_type2 = key2.split("|");
		
		String name1 = name_type1[0];
		String type1 = name_type1.length==2?name_type1[1]:"null";
		String name2 = name_type2[0];
		String type2 = name_type2.length==2?name_type2[1]:"null";
		
		boolean nameMatch = (name1.equals(name2) || name1.contains(name2));
		boolean typeMatch = (type1.equals(type2) || type1.contains(type2));
		
		return nameMatch && typeMatch;
	}
	
	protected void processNotMatchedNode(Object child, boolean isOnGt) {
		if(!(child instanceof JSONObject)) {
			return;
		}
		if(isOnGt) {
			notPredictedNum+=((JSONObject)child).keySet().size();
		}
		else {
			wrongPredictionNum+=((JSONObject)child).keySet().size();
		}
		for(String key : ((JSONObject)child).keySet()) {
			processNotMatchedNode(((JSONObject)child).get(key),isOnGt);
		}
	}
	
}
