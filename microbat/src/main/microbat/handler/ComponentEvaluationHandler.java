package microbat.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
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
import microbat.tracerecov.TraceRecoverer;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;
import microbat.tracerecov.executionsimulator.ExecutionSimulatorFactory;
import microbat.tracerecov.executionsimulator.SimulatorConstants;
import microbat.tracerecov.executionsimulator.VariableExpansionUtils;
import microbat.util.MicroBatUtil;
import microbat.util.Settings;
import microbat.views.MicroBatViews;
import microbat.views.TraceView;
import sav.strategies.dto.AppJavaClassPath;
import sav.common.core.Pair;

public class ComponentEvaluationHandler extends StartDebugHandler {
	private int matchedNum;
	private int notPredictedNum;
	private int wrongPredictionNum;

	private static ArrayList<String> classes = new ArrayList<>(Arrays.asList(
            "leetbugs.ANAGRAM",
            "leetbugs.ANAGRAMS",
            "leetbugs.BINARY",
            "leetbugs.BUCKETSORT",
            "leetbugs.CAMAL",
            "leetbugs.COUNT_SAY",
            "leetbugs.DECODE",
            "leetbugs.DETECT_CYCLE",
            "leetbugs.DISTINCT",
            "leetbugs.DUPLICATE",
            "leetbugs.EVAL_RPN",
            "leetbugs.FREQUENCY",
            "leetbugs.GET_FACTORS",
            "leetbugs.INTERSECT",
            "leetbugs.ISOMORPHIC",
            "leetbugs.KHEAPSORT",
            "leetbugs.LETTERS",
            "leetbugs.LEVEL",
            "leetbugs.LIS",
            "leetbugs.MERGE_SIMILAR_ITEM",
            "leetbugs.MISSING_NUM",
            "leetbugs.PASCAL",
            "leetbugs.PATH",
            "leetbugs.POWERSET",
            "leetbugs.SPIRAL",
            "leetbugs.SUBARRAY",
            "leetbugs.THREENUM",
            "leetbugs.UGLY_NUM"
        ));

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Job job = new Job(DebugPilotHandler.JOB_FAMALY_NAME) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
		        for(String launchClass:classes) {
		        	execute(launchClass);
		        }
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

	protected void execute(String launchClass) {
		Log.printMsg(getClass(), "");
		Log.printMsg(getClass(), "=====================================");
		Log.printMsg(getClass(), "Launch Class: "+launchClass);
		Log.printMsg(getClass(), "=====================================");
		Log.printMsg(getClass(), "");
		
		Log.printMsg(getClass(), "Using model: "+SimulatorConstants.getSelectedModel());

		Settings.launchClass = launchClass;
		
		final AppJavaClassPath appClassPath = MicroBatUtil.constructClassPaths();
		List<String> srcFolders = MicroBatUtil.getSourceFolders(Settings.projectName);
		appClassPath.setSourceCodePath(MicroBatUtil.getSourceFolder(Settings.launchClass, Settings.projectName));

		for (String srcFolder : srcFolders) {
			if (!srcFolder.equals(appClassPath.getTestCodePath())) {
				appClassPath.getAdditionalSourceFolders().add(srcFolder);
			}
		}

		Trace trace = this.generateTrace(appClassPath, null);
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				TraceView traceView = MicroBatViews.getTraceView();
				traceView.setMainTrace(trace);
				traceView.updateData();
			}
		});

		// String className = Settings.launchClass.substring(Settings.launchClass.indexOf(".") + 1);
		// ObjectMapper objectMapper = new ObjectMapper();
		// Path bmPath = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_benchmark", className + ".json");

		// List<Map<String, String>> benchmark = null;
		// Map<String, List<String>> groundtruth = null;
		// try {
		// 	benchmark = objectMapper.readValue(new File(bmPath.toString()),
		// 			new TypeReference<List<Map<String, String>>>() {
		// 			});
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		// if (benchmark == null) {
		// 	Log.printMsg(getClass(), "ERROR: load benchmark or groundtruth failed");
		// 	return;
		// }

		// matchedNum = 0;
		// notPredictedNum = 0;
		// wrongPredictionNum = 0;

		// String result = evaluateComponent(appClassPath, trace, benchmark, groundtruth);

		String className = Settings.launchClass.substring(Settings.launchClass.indexOf(".") + 1);
		ObjectMapper objectMapper = new ObjectMapper();
		Path bmPath1 = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_benchmark_def", className + ".json");
        Path bmPath2 = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_benchmark_def", className + "_Neg.json");

		List<Map<String, String>> benchmark_positive = null;
		List<Map<String, String>> benchmark_negtive = null;
		try {
			benchmark_positive = objectMapper.readValue(new File(bmPath1.toString()),
					new TypeReference<List<Map<String, String>>>() {
					});
			benchmark_negtive = objectMapper.readValue(new File(bmPath2.toString()),
					new TypeReference<List<Map<String, String>>>() {
					});
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (benchmark_positive == null || benchmark_negtive == null) {
			Log.printMsg(getClass(), "ERROR: load benchmark or groundtruth failed");
			return;
		}
        benchmark_positive.addAll(benchmark_negtive);
        String result = evaluateDefInference(appClassPath, trace, benchmark_positive);

		String filePath = Paths.get("C:\\Users\\Kwy\\Desktop\\RQ2\\RQ2_result_def", className + ".txt").toString();
		try (FileWriter writer = new FileWriter(filePath)) {
			writer.write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

    protected String evaluateDefInference(final AppJavaClassPath appClassPath, Trace trace, List<Map<String, String>> benchmark){
		// def-inference
        ArrayList<Map<String,String>> failed = new ArrayList<>();
        ArrayList<Map<String,String>> skipped = new ArrayList<>();
		int def_succ = 0;

		ExecutionSimulator simulator = ExecutionSimulatorFactory.getExecutionSimulator();
		System.out.println(simulator.getClass());
		
		// for each selected data dependency to be recovered
		for (Map<String, String> element : benchmark) {
			int currentOrder = Integer.valueOf(element.get("Order"));
			TraceNode currentStep = trace.getExecutionList().get(currentOrder - 1);
			
            // find root var
            VarValue rootVar = null;
            for(VarValue var : currentStep.getAllVariables()){
                if(var.getVarName().equals(element.get("RootVarName"))){
                    rootVar = var;
                    break;
                }
            }
            if(rootVar == null){
                element.put("SkipReason","root var is null");
                skipped.add(element);
                continue;
            }

			// assume variable expansion is correct
			Condition condition = new Condition(rootVar.getVarName(), rootVar.getType(), rootVar.getStringValue(),"null");
			System.out.println("Re-execution of condition: " + condition);
			Trace newTrace = generateTrace(appClassPath, condition);
			JSONObject varExpandedJson = condition.getMatchedGroundTruth(newTrace);
			if (varExpandedJson == null) {
				continue;
			}
			VariableExpansionUtils.processResponse(rootVar, varExpandedJson.toString());

            List<VarValue> criticalVariables = new ArrayList<>();
            // find target var
            VarValue targetVar = rootVar;
            String[] varNames = element.get("FieldName").split("\\.");
            if(!rootVar.getVarName().equals(varNames[0])){
                element.put("SkipReason","root var name not match");
                skipped.add(element);
                continue;
            }

            criticalVariables.add(rootVar);
            int i;
            for(i = 1;i<varNames.length;i++){
                boolean found = false;
                for(VarValue var : targetVar.getChildren()){
                    if(var.getVarName().equals(varNames[i])){
                        targetVar = var;
                        criticalVariables.add(var);
                        found = true;
                        break;
                    }
                }
                if(!found){
                    break;
                }
            }
            if(i < varNames.length){
                element.put("SkipReason","field var name not match");
                skipped.add(element);
                continue;
            }

            // predict and pending
            boolean def = simulator.inferDefinition(currentStep, rootVar, targetVar, criticalVariables);
            boolean gt = element.get("Written").equals("T")?true:false;
            if(def == gt){
                def_succ += 1;
            }
            else{
                failed.add(element);
            }
		}

		StringBuilder result = new StringBuilder();
        result.append("Total: "+benchmark.size()+"\n");
        result.append("Skipped: "+skipped.size()+"\n");
        result.append("Failed: "+failed.size()+"\n");
        result.append("Successfull: "+def_succ+"\n");

        double def_infer_rate = (double) def_succ / (benchmark.size()-skipped.size());
        result.append("Successfull rate: "+ def_infer_rate+"\n");

        result.append("[Skipped]: "+skipped.toString()+"\n");
        result.append("[Failed]: "+failed.toString()+"\n");

		System.out.println(result.toString());
		return result.toString();
    }


	protected String evaluateComponent(final AppJavaClassPath appClassPath, Trace trace,
			List<Map<String, String>> benchmark, Map<String, List<String>> groundtruth) {
		// var-expansion
		HashMap<String,String> checkedVar = new HashMap<String,String>();
		List<Double> precisionList = new ArrayList<Double>();
		List<Double> recallList = new ArrayList<Double>();

		// def-inference
		Map<Integer, Set<Integer>> predictedDataDep = new HashMap<>();
		int var_succ = 0;
		int def_succ = 0;

        ArrayList<HashMap<String,String>> failed = new ArrayList<>();
		
		ExecutionSimulator simulator = ExecutionSimulatorFactory.getExecutionSimulator();
		System.out.println(simulator.getClass());
		
		// for each selected data dependency to be recovered
		for (Map<String, String> element : benchmark) {
			int currentOrder = Integer.valueOf(element.get("Order"));
			TraceNode currentStep = trace.getExecutionList().get(currentOrder - 1);

			// 1. Variable expansion score (P&R)
			VarValue readVar = null;
			for (VarValue var : currentStep.getReadVariables()) {
				if (var.getVarName().equals(element.get("Name"))) {
					readVar = var;
					if (!checkedVar.containsKey(var.getType() + "|" + var.getVarName() + ":" + var.getStringValue())
							&& !var.isExpanded()) {
						String gtExpansion = element.get("Expansion");
						String prediction = null;
						try {
							Log.printMsg(getClass(), "Getting expansion prediction at var: " + var.toString()
									+ ", on step " + currentStep.getOrder());
							// prediction = simulator.expandVariable(var, currentStep, null);
							prediction = simulator.expandVariable(var, currentStep, null, false);
							var.setExpanded(true);
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
						checkedVar.put(var.getType() + "|" + var.getVarName() + ":" + var.getStringValue(), prediction);
					}
					break;
				}
			}

			// 2. Variable expansion score(Recovery rate)
			String prediction = checkedVar.get(readVar.getType() + "|" + readVar.getVarName() + ":" + readVar.getStringValue());
			if(successfullyRecovField(prediction, element.get("Field"))) {
				var_succ += 1;
			}
			
			// 3. Definition inference precision
//			Set<Integer> dataDominatorsAfterRecovery;
//			if (predictedDataDep.containsKey(currentOrder)) {
//				dataDominatorsAfterRecovery = predictedDataDep.get(currentOrder);
//				if (dataDominatorsAfterRecovery.contains(Integer.valueOf(element.get("Dependency")))) {
//					def_succ += 1;
//					continue;
//				}
//			} else {
//				dataDominatorsAfterRecovery = new HashSet<>();
//			}
//
//			// assume variable is correct
//			Condition condition = new Condition(readVar.getVarName(), readVar.getType(), readVar.getStringValue(),"null");
//			System.out.println("Re-execution of condition: " + condition);
//			Trace newTrace = generateTrace(appClassPath, condition);
//			JSONObject varExpandedJson = condition.getMatchedGroundTruth(newTrace);
//			if (varExpandedJson == null) {
//				continue;
//			}
//			VariableExpansionUtils.processResponse(readVar, varExpandedJson.toString());
//			
//			TraceRecoverer traceRecoverer = new TraceRecoverer();
//			for (VarValue targetVar : readVar.getAllDescedentChildren()) {
//				if (targetVar.getVarName().equals(element.get("Field"))) {
//					traceRecoverer.recoverDataDependency(currentStep, targetVar, readVar);
//					TraceNode dataDominator = trace.findProducer(targetVar,currentStep);
//					TraceNode dataDominator1 = trace.findProducer(readVar, currentStep);
//					if (dataDominator != null) {
//						dataDominatorsAfterRecovery.add(dataDominator.getOrder());
//					}
//					if(dataDominator1 != null){
//						dataDominatorsAfterRecovery.add(dataDominator1.getOrder());
//					}
//				}
//			}
//			predictedDataDep.put(currentOrder, dataDominatorsAfterRecovery);
//			if (dataDominatorsAfterRecovery.contains(Integer.valueOf(element.get("Dependency")))) {
//				def_succ += 1;
//			}
		}

		StringBuilder result = new StringBuilder();
		result.append("Checked variable:\n");
		result.append(checkedVar + "\n");
		result.append("Predicted data dependency:\n");
		result.append(predictedDataDep + "\n");
		result.append("************ Result ***********\n");
		result.append("Checked var_value number:" + checkedVar.size() + "\n");
		result.append("Variable expansion precision list:" + precisionList.toString() + "\n");
		result.append("Variable expansion average precision:" + getAvg(precisionList) + "\n");
		result.append("Variable expansion recall list:" + recallList.toString() + "\n");
		result.append("Variable expansion average recall:" + getAvg(recallList) + "\n");
		int total = benchmark.size();
		double var_recov_rate = (double) var_succ / total;
		result.append("Variable expansion recovery successfull number: " + var_succ + "\n");
		result.append("Variable expansion recovery successfull rate: " + var_recov_rate + "\n");
		
//		double def_infer_rate = (double) def_succ / total;
//		result.append("Checked data dependency number:" + total + "\n");
//		result.append("Definition inference successfull number:" + def_succ + "\n");
//		result.append("Definition inference successfull rate:" + def_infer_rate + "\n");

		System.out.println(result.toString());
		return result.toString();
	}

	protected Pair<Double, Double> calculatePR(String groundtruth, String prediction) {
		// parse to json object
		System.out.println("Original string: ");
		System.out.println("groundtruth: " + groundtruth);
		System.out.println("prediction: " + prediction);

		JSONObject groundtruthJson = processResponse(groundtruth);
		JSONObject predictionJson = processResponse(prediction);

		matchJsonTree(groundtruthJson, predictionJson);

		double precision = 0;
		if (matchedNum + wrongPredictionNum != 0) {
			precision = (double) matchedNum / (matchedNum + wrongPredictionNum);
		}
		double recall = 0.0;
		if (matchedNum + notPredictedNum != 0) {
			recall = (double) matchedNum / (matchedNum + notPredictedNum);
		}

		System.out.println("PR: " + precision + ", " + recall);

		return Pair.of(precision, recall);
	}

	protected JSONObject processResponse(String response) {
		int begin = response.indexOf("{");
		int end = response.lastIndexOf("}");
		response = response.substring(begin, end + 1);
		response = response.replace("\\n", "");
		JSONObject variable = new JSONObject(response);
		return variable;
	}

	protected void matchJsonTree(Object json1, Object json2) {
		if (json1 instanceof JSONObject && json2 instanceof JSONObject) {
			Set<String> keySet1 = ((JSONObject) json1).keySet();
			Set<String> keySet2 = ((JSONObject) json2).keySet();
			Set<String> matched1 = new HashSet<String>();
			Set<String> matched2 = new HashSet<String>();

			for (String key1 : keySet1) {
				if(isAllUpperCase(key1.split("\\|")[0])) {
					continue;
				}
				for (String key2 : keySet2) {
					if(isAllUpperCase(key2.split("\\|")[0])) {
						continue;
					}
					if (matched2.contains(key2)) {
						continue;
					}
					// successfully matched
					if (nameTypeMatch(key1, key2)) {
						Object child1 = ((JSONObject) json1).get(key1);
						Object child2 = ((JSONObject) json2).get(key2);
						if (child1 instanceof JSONObject && child2 instanceof JSONObject) {
							matchedNum += 1;
							matched1.add(key1);
							matched2.add(key2);
						} else if (valueMatch(child1, child2)) {
							matchedNum += 1;
							matched1.add(key1);
							matched2.add(key2);
						}
						matchJsonTree(child1, child2);
						break;
					}
				}
			}

			for (String key1 : keySet1) {
				if (!matched1.contains(key1) && !isAllUpperCase(key1.split("\\|")[0])) {
					notPredictedNum += 1;
					processNotMatchedNode(((JSONObject) json1).get(key1), true);
				}
			}
			for (String key2 : keySet2) {
				if (!matched2.contains(key2) && !isAllUpperCase(key2.split("\\|")[0])) {
					wrongPredictionNum += 1;
					processNotMatchedNode(((JSONObject) json2).get(key2), false);
				}
			}

		} else if (json1 instanceof JSONObject) {
			notPredictedNum += ((JSONObject) json1).keySet().size();
			for (String key : ((JSONObject) json1).keySet()) {
				processNotMatchedNode(((JSONObject) json1).get(key), true);
			}
		} else if (json2 instanceof JSONObject) {
			wrongPredictionNum += ((JSONObject) json2).keySet().size();
			for (String key : ((JSONObject) json2).keySet()) {
				processNotMatchedNode(((JSONObject) json2).get(key), false);
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
		String[] name_type1 = key1.split("\\|");
		String[] name_type2 = key2.split("\\|");

		String name1 = name_type1[0];
		String type1 = name_type1.length == 2 ? name_type1[1] : "null";
		String name2 = name_type2[0];
		String type2 = name_type2.length == 2 ? name_type2[1] : "null";

		boolean nameMatch = (name1.equals(name2) || name1.contains(name2)) || name2.contains(name1);
		boolean typeMatch = (type1.equals(type2) || type1.contains(type2)) || type2.contains(type1);

		return nameMatch && typeMatch;
	}

	protected boolean valueMatch(Object value1, Object value2) {
		if(value1.toString().equals("null") ||  calStringSim(value1.toString(),value2.toString())>0.75) {
			return true;
		}
		if (value1 instanceof JSONObject || value2 instanceof JSONObject) {
			return false;
		} else if (value1 instanceof JSONArray && value2 instanceof JSONArray) {
			return calJSONArraySim((JSONArray) value1, (JSONArray) value2) > 0.75;
		} else if ((!(value1 instanceof JSONArray)) && (!(value2 instanceof JSONArray))) {
			return calStringSim(value1.toString(), value2.toString()) > 0.75;
		}
		return false;
	}

	protected void processNotMatchedNode(Object child, boolean isOnGt) {
		if (!(child instanceof JSONObject)) {
			return;
		}
		
		for (String key : ((JSONObject) child).keySet()) {
			if (!isAllUpperCase(key.split("\\|")[0])) {
				if(isOnGt) {
					notPredictedNum += 1;
				}
				else {
					wrongPredictionNum += 1;
				}
			}
		}
		for (String key : ((JSONObject) child).keySet()) {
			processNotMatchedNode(((JSONObject) child).get(key), isOnGt);
		}
	}

	public boolean isAllUpperCase(String str) {
		return str.chars().filter(Character::isLetter).allMatch(Character::isUpperCase);
	}

	public double calJSONArraySim(JSONArray array1, JSONArray array2) {
		if(array1.toString().contains(array2.toString()) || array2.toString().contains(array1.toString())) {
			return 1.0;
		}
		int matchCount = 0;
		for (int i = 0; i < array1.length(); i++) {
			Object element = array1.get(i);
			if (array2.toList().contains(element)) {
				matchCount++;
			}
		}
		int maxSize = Math.max(array1.length(), array2.length());
		return (double) matchCount / maxSize;
	}

	public double calStringSim(String str1, String str2) {
		if(str1.contains(str2) || str2.contains(str1)) {
			return 1.0;
		}
		LevenshteinDistance levenshtein = new LevenshteinDistance();
		int distance = levenshtein.apply(str1, str2);

		int maxLength = Math.max(str1.length(), str2.length());
		return 1.0 - (double) distance / maxLength;
	}
	
	public boolean successfullyRecovField(String prediction, String field) {
		int begin = prediction.indexOf("{");
		int end = prediction.lastIndexOf("}");
		prediction = prediction.substring(begin, end + 1);
		prediction = prediction.replace("\\n", "");
		JSONObject jsonObj = new JSONObject(prediction);
		
		return successfullyRecovFieldRecur(jsonObj,field);
	}
	
	public boolean successfullyRecovFieldRecur(JSONObject jsonObj, String field) {
		for(String key : jsonObj.keySet()) {
			String name = null;
			if(key.contains("|")) {
				name = key.split("\\|")[0];
			}
			else {
				name = key;
			}
			if(name.contains(field)) {
				return true;
			}
		}
		
		for(String key : jsonObj.keySet()) {
			Object value = jsonObj.get(key);
			if(value instanceof JSONObject) {
				if(successfullyRecovFieldRecur((JSONObject)value,field)) {
					return true;
				}
			}
		}
		
		return false;
	}
}
