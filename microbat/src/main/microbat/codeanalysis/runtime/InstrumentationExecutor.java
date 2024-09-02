package microbat.codeanalysis.runtime;

import java.awt.desktop.SystemSleepEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.gson.Gson;

import microbat.agent.TraceAgentRunner;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.AgentParams.LogType;
import microbat.instrumentation.dataflowrecovery.DependencyRecoveryInfo;
import microbat.instrumentation.filter.CodeRangeEntry;
import microbat.instrumentation.filter.JdkFilter;
import microbat.instrumentation.output.RunningInfo;
import microbat.instrumentation.precheck.PrecheckInfo;
import microbat.model.BreakPoint;
import microbat.model.ClassLocation;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
import microbat.preference.DatabasePreference;
import microbat.preference.ExecutionRangePreference;
import microbat.preference.MicrobatPreference;
import microbat.sql.DBSettings;
import microbat.sql.DbService;
import microbat.util.JavaUtil;
import microbat.util.MinimumASTNodeFinder;
import microbat.util.Settings;
import sav.common.core.SavException;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.vm.VMConfiguration;
import sav.strategies.vm.VMRunner;

public class InstrumentationExecutor {
	public final static String TRACE_DUMP_FILE_SUFFIX = ".exec";
	
	private AppJavaClassPath appPath;
	private PreCheckInformation precheckInfo;
	private String traceExecFilePath;
	private TraceAgentRunner agentRunner;
	private long timeout = VMRunner.NO_TIME_OUT;
	
	private List<String> includeLibs = Collections.emptyList();
	private List<String> excludeLibs = Collections.emptyList();
	private Set<String> extClasses = null;
	
	public InstrumentationExecutor(AppJavaClassPath appPath, String traceDir, String traceName, 
			List<String> includeLibs, List<String> excludeLibs) {
		this(appPath, generateTraceFilePath(traceDir, traceName), includeLibs, excludeLibs);
		agentRunner = createTraceAgentRunner();
	}
	
	public InstrumentationExecutor(AppJavaClassPath appPath, String traceExecFilePath, 
			List<String> includeLibs, List<String> excludeLibs) {
		this.appPath = appPath;
		this.traceExecFilePath = traceExecFilePath;
		this.includeLibs = includeLibs;
		this.excludeLibs = excludeLibs;
		
		agentRunner = createTraceAgentRunner();
	}
	
	private TraceAgentRunner createTraceAgentRunner() {
		
		if(DBSettings.USE_DB.equals("true")) {
			try {
				DbService.getConnection();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		String jarPath = appPath.getAgentLib();
		VMConfiguration config = new VMConfiguration();
		TraceAgentRunner agentRunner = new TraceAgentRunner(jarPath, config);
//		agentRunner.setVmDebugPort(9595);
		config.setNoVerify(true);
		config.setJavaHome(appPath.getJavaHome());
		config.setClasspath(appPath.getClasspaths());
		config.setLaunchClass(appPath.getLaunchClass());
		config.setWorkingDirectory(appPath.getWorkingDirectory());
				
		if (appPath.getOptionalTestClass() != null) {
			config.addProgramArgs(appPath.getOptionalTestClass());
			config.addProgramArgs(appPath.getOptionalTestMethod());
			agentRunner.addAgentParam(AgentParams.OPT_LAUNCH_CLASS, appPath.getOptionalTestClass());
		} else {
			agentRunner.addAgentParam(AgentParams.OPT_ENTRY_POINT,
					appPath.getLaunchClass() + "." + "main([Ljava/lang/String;)V");
		}
		
		agentRunner.addAgentParam(AgentParams.OPT_JAVA_HOME, config.getJavaHome());
		agentRunner.addAgentParams(AgentParams.OPT_CLASS_PATH, config.getClasspaths());
		agentRunner.addAgentParam(AgentParams.OPT_WORKING_DIR, config.getWorkingDirectory());
		/* build includes & excludes params */
		agentRunner.addIncludesParam(this.includeLibs);
		agentRunner.addExcludesParam(this.excludeLibs);
		agentRunner.addAgentParam(AgentParams.OPT_VARIABLE_LAYER, MicrobatPreference.getVariableValue());
		agentRunner.addAgentParam(AgentParams.OPT_STEP_LIMIT, MicrobatPreference.getStepLimit());
		agentRunner.addAgentParams(AgentParams.OPT_LOG, Arrays.asList(
				LogType.printProgress, 
//				LogType.info,
//				LogType.debug, 
				LogType.error));
		agentRunner.addAgentParam(AgentParams.OPT_REQUIRE_METHOD_SPLITTING,
				MicrobatPreference.getValue(MicrobatPreference.REQUIRE_METHOD_SPLITTING));
		agentRunner.addAgentParam(AgentParams.OPT_AVOID_TO_STRING_OF_PROXY_OBJ, true);
		agentRunner.setTimeout(timeout);
		// FIXME Xuezhi [2]
		List<CodeRangeEntry> entries = ExecutionRangePreference.getCodeRangeEntrys();
		agentRunner.addAgentParams(AgentParams.OPT_CODE_RANGE, entries); 
		return agentRunner;
	}
	
	public RunningInfo run() throws StepLimitException {
		try {
//			System.out.println("first precheck..");
//			agentRunner.precheck(null);
//			PrecheckInfo firstPrecheckInfo = agentRunner.getPrecheckInfo();
//			System.out.println(firstPrecheckInfo);
//			System.out.println("second precheck..");
			
			agentRunner.getConfig().setDebug(Settings.isRunWtihDebugMode);
			agentRunner.getConfig().setPort(9000);
			
			System.out.println("precheck..");
			agentRunner.precheck(null);
			PrecheckInfo info = agentRunner.getPrecheckInfo();
			System.out.println(info);
			PreCheckInformation precheckInfomation = new PreCheckInformation(info.getThreadNum(), info.getStepTotal(),
					info.isOverLong(), new ArrayList<>(info.getVisitedLocs()), info.getExceedingLimitMethods(), 
					info.getLoadedClasses(), info.getLibraryCalls());
			precheckInfomation.setPassTest(agentRunner.isTestSuccessful());
//			precheckInfomation.setUndeterministic(firstPrecheckInfo.getStepTotal() != precheckInfomation.getStepTotal());
			this.setPrecheckInfo(precheckInfomation);
			System.out.println("the trace length is: " + precheckInfomation.getStepNum());
			if (precheckInfomation.isUndeterministic()) {
				System.out.println("undeterministic!!");
			}
			if (info.isOverLong() /*&& !precheckInfomation.isUndeterministic() */) {
				throw new StepLimitException();
			}
			if (!info.getExceedingLimitMethods().isEmpty()) {
				agentRunner.addAgentParams(AgentParams.OPT_OVER_LONG_METHODS, info.getExceedingLimitMethods());
			}
			
//			agentRunner.getConfig().setDebug(Settings.isRunWtihDebugMode);
//			agentRunner.getConfig().setPort(8000);
//			DependencyRecoveryInfo dataFlowInfo = collectLibraryCalls(precheckInfomation);
			
			RunningInfo ori_rInfo = execute(precheckInfomation);
			Trace trace1 = ori_rInfo.getMainTrace();
			
			boolean EXT_INS = true;
			
			if(EXT_INS) {
				extClasses = new HashSet<String>();
				for(String s: precheckInfomation.getLibraryCalls()) {
			        int index = s.indexOf("#");
			        if (index == -1) {
			        	continue;
			        }
			        s = s.substring(0, index);
			        if(!JdkFilter.filter(s) || !PkgFilter.filter(s)) {
			        	continue;
			        }
			        extClasses.add(s.substring(0, index));
			        addMoreType(s.substring(0, index));
				}
			
				System.out.println("Library calls:");
				System.out.println(extClasses);
				agentRunner.addIncludesParam(new ArrayList<>(extClasses));
				agentRunner.addAgentParam(AgentParams.OPT_FULL_INSTRUMENTATION, "true");			
				RunningInfo exhaustive_rInfo = execute(precheckInfomation);
				Trace trace2 = exhaustive_rInfo.getMainTrace();
				
				System.out.println("trace1.length : "+trace1.size());
				System.out.println("trace2.length : "+trace2.size());
				
//				Map<Integer, Integer> map = traceAlignment(trace1,trace2);
//				migrateDataDependency(trace1, trace2, map);
				return exhaustive_rInfo;
			}
			else {
				agentRunner.addAgentParam(AgentParams.OPT_FULL_INSTRUMENTATION, "false");	
			}
//			recordDataDependency(trace1);
			
			return ori_rInfo;
			
		} catch (SavException e1) {
			e1.printStackTrace();
		}
		
		return null;
	}
	
	// =========== add for recovering data dependency through extra instrumentation =========
	public Map<Integer,Integer> traceAlignment(Trace ori_trace, Trace ex_trace) {
		List<TraceNode> ori_trace_node = ori_trace.getExecutionList();
		List<TraceNode> ex_trace_node = ex_trace.getExecutionList();
		Map<Integer,Integer> map = new HashMap<Integer,Integer>();
		
		int i = 0, j = 0;
		while(i < ori_trace_node.size() && j < ex_trace_node.size()) {
			TraceNode oNode = ori_trace_node.get(i);
			TraceNode eNode = ex_trace_node.get(j);
			if(!containExtMethod(eNode.getInvokingMethod()) && expandedStep(oNode) == expandedStep(eNode)) {
				map.put(j+1, i+1);
				i++;
				j++;
			}
			else {
				if(!invokeMethod(oNode)) { // original node doesn't expand the method
					int currentLine = ex_trace_node.get(j).getLineNumber();
					while(j < ex_trace_node.size() && 
							(containExtMethod(ex_trace_node.get(j).getInvokingMethod()) || (expandedStep(ex_trace_node.get(j))&& !expandedStep(ori_trace_node.get(i))))){
						eNode = ex_trace_node.get(j);
						int end = eNode.getStepOverNext()==null?ex_trace_node.size():eNode.getStepOverNext().getOrder()-1;
						
						for(int k = j ;k < end; k++) {
							map.put(k+1, i+1);
							
							TraceNode tmpNode = ex_trace_node.get(k);
							
							List<VarValue> oNodeRead = oNode.getReadVariables();
							List<VarValue> oNodeWrite = oNode.getWrittenVariables();
							
							for(VarValue v:tmpNode.getReadVariables()) {
								if(!containVariable(oNodeRead,v)) {
									oNodeRead.add(v);
								}
							}
							for(VarValue v:tmpNode.getWrittenVariables()) {
								if(!containVariable(oNodeWrite,v)) {
									oNodeWrite.add(v);
								}
							}
						}
						j = end;
					}
					
					
					if(j < ex_trace_node.size() && ex_trace_node.get(j).getLineNumber() == currentLine) {// write return value step
						map.put(j+1, i+1);
						eNode = ex_trace_node.get(j);
						
						List<VarValue> oNodeRead = oNode.getReadVariables();
						List<VarValue> oNodeWrite = oNode.getWrittenVariables();
						
						for(VarValue v:eNode.getReadVariables()) {
							if(!containVariable(oNodeRead,v)) {
								oNodeRead.add(v);
							}
						}
						for(VarValue v:eNode.getWrittenVariables()) {
							if(!containVariable(oNodeWrite,v)) {
								oNodeWrite.add(v);
							}
						}
					}
					else {
						i++;
					}
				}
				else {
					int end = eNode.getStepOverNext()==null?ex_trace_node.size():eNode.getStepOverNext().getOrder()-1;
					for(int k = j ;k < eNode.getStepOverNext().getOrder()-1; k++) {
						map.put(k+1, i+1);
						TraceNode tmpNode = ex_trace_node.get(k);
						
						List<VarValue> oNodeRead = oNode.getReadVariables();
						List<VarValue> oNodeWrite = oNode.getWrittenVariables();
						
						for(VarValue v:tmpNode.getReadVariables()) {
							if(!containVariable(oNodeRead,v)) {
								oNodeRead.add(v);
							}
						}
						for(VarValue v:tmpNode.getWrittenVariables()) {
							if(!containVariable(oNodeWrite,v)) {
								oNodeWrite.add(v);
							}
						}
					}
					j = end;
				}
			}
		}
		
		System.out.println("finish align traces, "+(i)+":"+(j));
		return map;
	}
	
	public boolean containVariable(List<VarValue> vars, VarValue var) {
		if(vars.contains(var)) {
			return true;
		}
		for(VarValue v:vars) {
			if(v.getVarName().equals(var.getVarName())) {
				if(v.getVarID()!=null&&var.getVarID()!=null&&v.getVarID().equals(var.getVarID())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void recordDataDependency(Trace trace) {
		Map<Integer,ArrayList<Integer>> map = new HashMap<>();
		List<TraceNode> nodeList = trace.getExecutionList();
		int count = 0;
		for(int i = nodeList.size()-1;i >= 0;i--) {
			TraceNode currentNode = nodeList.get(i);
			ArrayList<Integer> dds = new ArrayList<>();
			for(VarValue v: currentNode.getReadVariables()) {
//				TraceNode dd = trace.findDataDependency(currentNode, v);
//				if(dd!=null && !dds.contains(dd.getOrder())) {
//					dds.add(dd.getOrder());
//					count++;
//				}
				findAllDataDependency(trace,currentNode,v,dds);
			}
			count+=dds.size();
			map.put(currentNode.getOrder(), dds);
		}
		map.put(0, new ArrayList<Integer>(Arrays.asList(count)));
		
		String className = nodeList.get(0).getClassCanonicalName();
		className = className.substring(className.lastIndexOf(".")+1);
		
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter("C:\\Users\\Kwy\\Desktop\\RQ2_gt\\"+className+".json")) {
            gson.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public void migrateDataDependency(Trace trace1, Trace trace2, Map<Integer,Integer> map) {
		System.out.println(map);
		Map<Integer,Set<String>> result = new HashMap<>();
		List<TraceNode> nodeList2 = trace2.getExecutionList();
		for(int i = nodeList2.size()-1;i >= 0;i--) {
			TraceNode currentNode = nodeList2.get(i);
			for(VarValue v: currentNode.getReadVariables()) {
				TraceNode dd = trace2.findDataDependencyNew(currentNode, v);
				if(dd!=null && map.get(dd.getOrder())!=map.get(currentNode.getOrder())) {
					if(!result.containsKey(map.get(currentNode.getOrder()))){
						Set<String> tmpList = new HashSet<>();
						result.put(map.get(currentNode.getOrder()), tmpList);
					}					
					result.get(map.get(currentNode.getOrder())).add(map.get(dd.getOrder())+":"+v.getVarName());
				}
			}
		}
		String className = nodeList2.get(0).getClassCanonicalName();
		className = className.substring(className.lastIndexOf(".")+1);
		
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter("C:\\Users\\Kwy\\Desktop\\RQ2_groundtruth\\"+className+".json")) {
            gson.toJson(result, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public void findAllDataDependency(Trace trace, TraceNode currentNode, VarValue var, List<Integer> collectDD){
		TraceNode dd = trace.findDataDependencyNew(currentNode, var);
		if(dd!=null && !collectDD.contains(dd.getOrder())) {
			collectDD.add(dd.getOrder());
		}
	}

	public boolean isDataDependency(TraceNode pre, TraceNode cur, Set<String> allReadVarID, Set<String> allReadVarAliasID) {
		Set<String> allWrittenVarID = new HashSet<String>(pre.collectWrittenVarID);
		Set<String> allWrittenVarAliasID = new HashSet<String>(pre.collectWrittenVarAliasID);

		for(VarValue v : pre.getWrittenVariables()) {
			allWrittenVarID.add(v.getVarID());
			allWrittenVarAliasID.add(v.getAliasVarID());
		}
		
		boolean flag = false;
		
		allWrittenVarID.retainAll(allReadVarID);
		if(!allWrittenVarID.isEmpty()) {
			allReadVarID.removeAll(allWrittenVarID);
			flag = true;
		}
		
		allWrittenVarAliasID.retainAll(allReadVarAliasID);
		if(!allWrittenVarAliasID.isEmpty()) {
			allReadVarAliasID.removeAll(allWrittenVarAliasID);
			flag = true;
		}
		
		return flag;
	}
	
	public boolean isExtMethod(String methodSig) {
		if(methodSig == null || methodSig.indexOf("#")==-1) {
			return false;
		}
		return extClasses.contains(methodSig.substring(0,methodSig.indexOf("#")))
				|| "java.lang.String#charAt(I)C".equals(methodSig);
	}
	
	public boolean containExtMethod(String invokeMethod) {
		String[] parts = invokeMethod.split("\\%");
		for(String methodSig : parts) {
			if(isExtMethod(methodSig)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean expandedStep(TraceNode node) {
		TraceNode next = node.getStepOverNext();
		if(next == null) {
			return false;
		}
		return next.getLineNumber()==node.getLineNumber();
	}
	
	public boolean invokeMethod(TraceNode node){
		if("%".equals(node.getInvokingMethod())){
			return true;
		}
		TraceNode next = node.getStepOverNext();
		if(next == null) {
			return false;
		}
		return next.getOrder()!=node.getOrder()+1;
	}
	
	public void addMoreType(String interfaceType) {
		if("java.util.List".equals(interfaceType)) {
			extClasses.add("java.util.ArrayList");
			extClasses.add("java.util.LinkedList");
		}
		else if("java.util.Set".equals(interfaceType)) {
			extClasses.add("java.util.HashSet");
			extClasses.add("java.util.LinkedHashSet");
			extClasses.add("java.util.TreeSet");
			extClasses.add("java.util.EnumSet");
		}
		else if("java.util.Map".equals(interfaceType)) {
			extClasses.add("java.util.HashMap");
			extClasses.add("java.util.LinkedHashMap");
			extClasses.add("java.util.TreeMap");
			extClasses.add("java.util.HashTable");
		}
		else if("java.util.Queue".equals(interfaceType)) {
			extClasses.add("java.util.LinkedList");
			extClasses.add("java.util.PriorityQueue");
		}
		else if("java.util.Deque".equals(interfaceType)) {
			extClasses.add("java.util.ArrayDeque");
			extClasses.add("java.util.LinkedList");
		}
		else if("java.util.Stack".equals(interfaceType)) {
			extClasses.add("java.util.ArrayDeque");
		}
	}
	
	// =========== [End] add for recovering data dependency through extra instrumentation =========

	
	public PreCheckInformation runPrecheck(String dumpFile, int stepLimit) {
		try {
			/* test stepLimit */
			agentRunner.addAgentParam(AgentParams.OPT_STEP_LIMIT, stepLimit);
			if (!agentRunner.precheck(dumpFile)) {
				precheckInfo = new PreCheckInformation();
				precheckInfo.setTimeout(true);
				return precheckInfo;
			}
			PrecheckInfo info = agentRunner.getPrecheckInfo();
//			System.out.println(info);
			System.out.println("isPassTest: " + agentRunner.isTestSuccessful());
			PreCheckInformation result = new PreCheckInformation(info.getThreadNum(), info.getStepTotal(), info.isOverLong(),
					new ArrayList<>(info.getVisitedLocs()), info.getExceedingLimitMethods(), info.getLoadedClasses(), info.getLibraryCalls());
			result.setPassTest(agentRunner.isTestSuccessful());
			result.setTimeout(agentRunner.isUnknownTestResult());
			this.setPrecheckInfo(result);
			return precheckInfo;
		} catch (SavException e1) {
			e1.printStackTrace();
		}
		return new PreCheckInformation(-1, -1, false, new ArrayList<ClassLocation>(), new ArrayList<String>(), new ArrayList<String>(), new HashSet<String>());
	}
	
	public RunningInfo execute(PreCheckInformation info) {
		try {
			long start = System.currentTimeMillis();
//			agentRunner.getConfig().setPort(8888);
			agentRunner.addAgentParam(AgentParams.OPT_EXPECTED_STEP, info.getStepNum());
			agentRunner.run(DatabasePreference.getReader());
			
			// agentRunner.runWithSocket();
			RunningInfo result = agentRunner.getRunningInfo();
//			System.out.println(result);
			System.out.println("isExpectedStepsMet? " + result.isExpectedStepsMet());
			System.out.println("trace length: " + result.getMainTrace() == null ? "0" : result.getMainTrace().size());
			System.out.println("isTestSuccessful? " + agentRunner.isTestSuccessful());
			System.out.println("testFailureMessage: " + agentRunner.getTestFailureMessage());
			System.out.println("finish!");
			agentRunner.removeAgentParam(AgentParams.OPT_EXPECTED_STEP);
			
			Trace trace = result.getMainTrace();
			trace.setAppJavaClassPath(appPath);
//			trace.setMultiThread(info.getThreadNum()!=1);
			
			appendMissingInfo(trace, appPath);
			trace.setConstructTime((int) (System.currentTimeMillis() - start));
			
			return result;
		} catch (SavException e1) {
			e1.printStackTrace();
		}

		return null;
	}
	
	public DependencyRecoveryInfo collectLibraryCalls(PreCheckInformation info) {
		try {
			agentRunner.addAgentParam(AgentParams.OPT_EXPECTED_STEP, info.getStepNum());
			String filePath = TraceAgentRunner.getlibCallsPath();
			agentRunner.recoverDependency(filePath);
			return agentRunner.getDependencyRecoveryInfo();
		} catch (SavException e1) {
			e1.printStackTrace();
		}
		
		return null;
	}
	

	public static void appendMissingInfo(Trace trace, AppJavaClassPath appPath) {
		Map<String, String> classNameMap = new HashMap<>();
		Map<String, String> pathMap = new HashMap<>();
		
		for(TraceNode node: trace.getExecutionList()){
			BreakPoint point = node.getBreakPoint();
			if(point.getFullJavaFilePath()==null){
				attachFullPathInfo(point, appPath, classNameMap, pathMap);				
			}
			
			if(!node.getInvocationChildren().isEmpty() && 
					node.getReadVariables().isEmpty() && node.getDeclaringCompilationUnitName() != null) {
				//check AST completeness
				CompilationUnit cu = JavaUtil.findCompilationUnitInProject(
						node.getDeclaringCompilationUnitName(), appPath);
				MinimumASTNodeFinder finder = new MinimumASTNodeFinder(
						node.getLineNumber(), cu);
				cu.accept(finder);
				ASTNode astNode = finder.getMinimumNode();
				
				if(astNode!=null) {
					int start = cu.getLineNumber(astNode.getStartPosition());
					int end = cu.getLineNumber(astNode.getStartPosition()+astNode.getLength());
					
					TraceNode stepOverPrev = node.getStepOverPrevious();
					while(stepOverPrev!=null && 
							start<=stepOverPrev.getLineNumber() &&
							stepOverPrev.getLineNumber()<=end) {
						List<VarValue> readVars = stepOverPrev.getReadVariables();
						for(VarValue readVar: readVars) {
							if(!node.getReadVariables().contains(readVar)) {
								node.getReadVariables().add(readVar);
							}
						}
						stepOverPrev = stepOverPrev.getStepOverPrevious();
					}
				}
				
			}
		}
	}
	
	public static void attachFullPathInfo(BreakPoint point, AppJavaClassPath appClassPath, 
			Map<String, String> classNameMap, Map<String, String> pathMap){
		if(point.getDeclaringCompilationUnitName() == null) {
			return;
		}
		
		String relativePath = point.getDeclaringCompilationUnitName().replace(".", File.separator) + ".java";
		List<String> candidateSourceFolders = appClassPath.getAllSourceFolders();
		for(String candidateSourceFolder: candidateSourceFolders){
			String filePath = candidateSourceFolder + File.separator + relativePath;
			if(new File(filePath).exists()){
				point.setFullJavaFilePath(filePath);
			}
		}
		
		//indicate the declaring compilation name is not correct
		if (point.getFullJavaFilePath() == null) {
			String fullPath = pathMap.get(point.getDeclaringCompilationUnitName());
			if (fullPath != null) {
				point.setFullJavaFilePath(fullPath);
			}
		}
		if(point.getFullJavaFilePath()==null){
			String canonicalClassName = point.getClassCanonicalName(); 
			String declaringCompilationUnitName = classNameMap.get(canonicalClassName);
			String fullPath = pathMap.get(canonicalClassName);
			
			if(declaringCompilationUnitName==null){
				String packageName = point.getPackageName();
				String packageRelativePath = packageName.replace(".", File.separator);
				for(String candidateSourceFolder: candidateSourceFolders){
					String packageFullPath = candidateSourceFolder + File.separator + packageRelativePath;
					declaringCompilationUnitName = findDeclaringCompilationUnitName(packageFullPath, canonicalClassName);
					if(declaringCompilationUnitName!=null){
						fullPath = candidateSourceFolder + File.separator + 
								declaringCompilationUnitName.replace(".", File.separator) + ".java";
						break;
					}
				}
			}
			
			classNameMap.put(canonicalClassName, declaringCompilationUnitName);
			pathMap.put(canonicalClassName, fullPath);
			
			point.setDeclaringCompilationUnitName(declaringCompilationUnitName);
			point.setFullJavaFilePath(fullPath);
			
			if(fullPath==null){
				System.err.println("cannot find the source code file for " + point);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static String findDeclaringCompilationUnitName(String packagePath, String canonicalClassName) {
		File packageFolder = new File(packagePath);
		
		if(!packageFolder.exists()){
			return null;
		}
		
		Collection javaFiles = FileUtils.listFiles(packageFolder, new String[]{"java"}, false);;
		for(Object javaFileObject: javaFiles){
			String javaFile = ((File)javaFileObject).getAbsolutePath();
			CompilationUnit cu = JavaUtil.parseCompilationUnit(javaFile);
			TypeNameFinder finder = new TypeNameFinder(cu, canonicalClassName);
			cu.accept(finder);
			if(finder.isFind){
				return JavaUtil.getFullNameOfCompilationUnit(cu);
			}
		}
		
		return null;
	}

	static class TypeNameFinder extends ASTVisitor{
		CompilationUnit cu;
		boolean isFind = false;
		String canonicalClassName;

		public TypeNameFinder(CompilationUnit cu, String canonicalClassName) {
			super();
			this.cu = cu;
			this.canonicalClassName = canonicalClassName;
		}
		
		public boolean visit(TypeDeclaration type){
			String simpleName = canonicalClassName;
			if(canonicalClassName.contains(".")){
				simpleName = canonicalClassName.substring(
						canonicalClassName.lastIndexOf(".")+1, canonicalClassName.length());
			}
			if(type.getName().getFullyQualifiedName().equals(simpleName)){
				this.isFind = true;
			}
			
			return false;
		}
		
	}
	
	public static String generateTraceFilePath(String traceDir, String traceFileName) {
		return new StringBuilder(traceDir).append(File.separator).append(traceFileName).append(TRACE_DUMP_FILE_SUFFIX)
				.toString();
	}

	public AppJavaClassPath getAppPath() {
		return appPath;
	}

	public void setAppPath(AppJavaClassPath appPath) {
		this.appPath = appPath;
	}

	public PreCheckInformation getPrecheckInfo() {
		return precheckInfo;
	}


	public void setPrecheckInfo(PreCheckInformation precheckInfo) {
		this.precheckInfo = precheckInfo;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	public String getTraceExecFilePath() {
		return traceExecFilePath;
	}

	public List<String> getIncludeLibs() {
		return includeLibs;
	}

	public void setIncludeLibs(List<String> includeLibs) {
		this.includeLibs = includeLibs;
	}

	public List<String> getExcludeLibs() {
		return excludeLibs;
	}

	public void setExcludeLibs(List<String> excludeLibs) {
		this.excludeLibs = excludeLibs;
	}
}
