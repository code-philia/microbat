package microbat.mutation.trace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Repository;
import org.eclipse.jdi.TimeoutException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import microbat.codeanalysis.bytecode.ByteCodeParser;
import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.codeanalysis.runtime.RunningInformation;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.mutation.mutation.ControlDominatedMutationVisitor;
import microbat.mutation.mutation.MutationType;
import microbat.mutation.mutation.TraceMutationVisitor;
import microbat.mutation.trace.dto.AnalysisParams;
import microbat.mutation.trace.dto.AnalysisTestcaseParams;
import microbat.mutation.trace.dto.BackupClassFiles;
import microbat.mutation.trace.dto.MutationTrace;
import microbat.mutation.trace.dto.SingleMutation;
import microbat.mutation.trace.dto.TraceExecutionInfo;
import microbat.mutation.trace.report.IMutationCaseChecker;
import microbat.mutation.trace.report.IMutationExperimentMonitor;
import microbat.preference.AnalysisScopePreference;
import microbat.util.BreakpointUtils;
import microbat.util.IResourceUtils;
import microbat.util.JTestUtil;
import microbat.util.JavaUtil;
import microbat.util.Settings;
import mutation.mutator.MutationVisitor;
import mutation.mutator.Mutator;
import sav.common.core.SavException;
import sav.common.core.utils.ClassUtils;
import sav.common.core.utils.FileUtils;
import sav.strategies.dto.AppJavaClassPath;
import sav.strategies.dto.ClassLocation;
import sav.strategies.mutanbug.MutationResult;
import sav.strategies.vm.JavaCompiler;
import sav.strategies.vm.VMConfiguration;
import tregression.SimulationFailException;
import tregression.empiricalstudy.DeadEndCSVWriter;
import tregression.empiricalstudy.DeadEndRecord;
import tregression.empiricalstudy.DeadEndReporter;
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.Regression;
import tregression.empiricalstudy.RegressionUtil;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.Simulator;
import tregression.empiricalstudy.solutionpattern.PatternIdentifier;
import tregression.empiricalstudy.training.DED;
import tregression.empiricalstudy.training.DeadEndData;
import tregression.empiricalstudy.training.TrainingDataTransfer;
import tregression.model.PairList;
import tregression.model.Trial;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;

public class MutationExperimentator {
	private static final int STEP_LIMIT = 10000;
	private static final long EXECUTOR_TIMEOUT = 30000l;
	private static boolean DEBUG = true;
	
	private boolean useSliceBreaker;
	private int breakerLimit;
	
	public MutationExperimentator(boolean useSliceBreaker, int breakerLimit) {
		super();
		this.useSliceBreaker = useSliceBreaker;
		this.breakerLimit = breakerLimit;
	}

	public void runEvaluation(IPackageFragment pack, AnalysisParams analysisParams,
			IMutationExperimentMonitor monitor) throws JavaModelException {
		IMutationCaseChecker checker = monitor.getMutationCaseChecker();
		String testSourceFolder = null;
		for (IJavaElement javaElement : pack.getChildren()) {
			if (javaElement instanceof IPackageFragment) {
				runEvaluation((IPackageFragment) javaElement, analysisParams, monitor);
			} else if (javaElement instanceof ICompilationUnit) {
				ICompilationUnit icu = (ICompilationUnit) javaElement;
				CompilationUnit cu = JavaUtil.convertICompilationUnitToASTNode(icu);
				List<MethodDeclaration> testingMethods = JTestUtil.findTestingMethod(cu);
				if (testingMethods.isEmpty()) {
					continue;
				}
				String className = JavaUtil.getFullNameOfCompilationUnit(cu);
				if (!checker.accept(className)) {
					continue;
				}
				for (MethodDeclaration testingMethod : testingMethods) {
					String methodName = testingMethod.getName().getIdentifier();
					if (monitor.isCanceled()) {
						return;
					}
					if (!checker.accept(className, methodName)) {
						continue;
					}
					try {
						AnalysisTestcaseParams tcParams = new AnalysisTestcaseParams(
								pack.getJavaProject().getElementName(), className, methodName, analysisParams, null);
						if (analysisParams.getIgnoredTestCaseFiles().contains(tcParams.getTestcaseName())) {
							continue;
						}
						if (testSourceFolder == null) {
							testSourceFolder = IResourceUtils.getSourceFolderPath(tcParams.getProjectName(), className);
						}
						tcParams.setTestSourceFolder(testSourceFolder);
						TraceExecutionInfo correctTrace = executeTestcase(tcParams);
						runSingleTestcase(correctTrace, tcParams, monitor);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (monitor.isCanceled()) {
				System.out.println("Process cancel!");
				return;
			}
		}
		System.out.println("Finish evaluation all!");
	}
	
	public TraceExecutionInfo executeTestcase(AnalysisTestcaseParams params) {
		AppJavaClassPath testcaseConfig = MuRegressionUtils.createProjectClassPath(params);
		List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
		List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
		String outputFolder = params.getAnalysisOutputFolder();
		InstrumentationExecutor executor = new InstrumentationExecutor(testcaseConfig, outputFolder, "fix",
				includedClassNames, excludedClassNames);
		executor.setTimeout(EXECUTOR_TIMEOUT);
		PreCheckInformation precheckInfo = executor.runPrecheck(null, STEP_LIMIT);

		if (!precheckInfo.isPassTest() || precheckInfo.isOverLong()) {
			System.out.println(params.getTestcaseName() + " is failed to get build trace: " + precheckInfo.toString());
			params.getAnalysisParams().updateIgnoredTestcase(params.getTestcaseName());
			return null;
		}

		System.out.println(params.getTestcaseName() + " is a passed test case");
		RunningInformation info = executor.execute(precheckInfo);
		if (!info.isExpectedStepsMet()) {
			return null;
		}

		Trace correctTrace = info.getTrace();
		Regression.fillMissingInfo(correctTrace, testcaseConfig);
		return new TraceExecutionInfo(precheckInfo, correctTrace, executor.getTraceExecFilePath(), null);
	}

	public boolean runSingleTestcase(TraceExecutionInfo correctTrace, AnalysisTestcaseParams params,
			IMutationExperimentMonitor monitor) throws JavaModelException {
		if (correctTrace == null) {
			return false;
		}
		IMutationCaseChecker checker = monitor.getMutationCaseChecker();
		AnalysisParams analysisParams = params.getAnalysisParams();
		String testCaseName = params.getTestcaseName();
		
		System.out.println("mutating the tested methods of " + testCaseName);
		List<SingleMutation> mutations = mutate(correctTrace.getTrace(), params);
		System.out.println("mutation done for " + testCaseName);
		if (mutations.isEmpty()) {
			System.out.println("What a pity, no proper mutants generated for " + testCaseName);
		}
		System.out.println("Start executing mutants for  " + testCaseName);
		System.out.println("===========the mutation is start=================");
		int thisTrialNum = 0;
		for (SingleMutation mutation : mutations) {
			if (monitor.isCanceled()) {
				return false;
			}
			if (!checker.accept(mutation.getMutationBugId(), MutationType.valueOf(mutation.getMutationType()))) {
				continue;
			}
			Trial tmpTrial = new Trial();
			tmpTrial.setTestCaseName(testCaseName);
			tmpTrial.setMutatedFile(mutation.getFile().toString());
			tmpTrial.setMutatedLineNumber(mutation.getLine());
			tmpTrial.setMutationType(mutation.getMutationType());
			if (monitor.isCanceled()) {
				return false;
			}
			MutationExecutionResult experimentResult = runSingleMutationTrial(mutation, params, correctTrace, monitor);
			if (!experimentResult.isLoopEffective && experimentResult.isValid) {
				continue;
			}

			if (experimentResult.isValid && analysisParams.isLimitTrialNum()) {
				thisTrialNum++;
				if (thisTrialNum >= analysisParams.getTrialNumPerTestCase()) {
					break;
				}
			}
		}
		
		System.out.println("===========all mutation is done==================");
		return false;
	}
	
	public MutationExecutionResult runSingleMutationTrial(SingleMutation mutation,
			AnalysisTestcaseParams testcaseParams, IMutationExperimentMonitor experimentMonitor) {
		TraceExecutionInfo correctTrace = executeTestcase(testcaseParams);
		return runSingleMutationTrial(mutation, testcaseParams, correctTrace, experimentMonitor);
	}

	public class CheckResult{
		List<EmpiricalTrial> trials;
		boolean foundRootCause;
		public CheckResult(List<EmpiricalTrial> trials, boolean foundRootCause) {
			super();
			this.trials = trials;
			this.foundRootCause = foundRootCause;
		}
	}
	
	private CheckResult checkRootCause(SingleMutation mutation, String orgFilePath, String mutationFilePath, 
			Trace killingMutatantTrace, Trace correctTrace, AnalysisTestcaseParams params, 
			PreCheckInformation buggyPrecheck, PreCheckInformation correctPrecheck,
			boolean useSliceBreaker, int breakLimit, IMutationExperimentMonitor monitor) throws SimulationFailException {
		
		int trialLimit = 10;
		int trialNum = 0;
		boolean isDataFlowComplete = false;
		List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
		List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
		while(!isDataFlowComplete && trialNum<trialLimit){
			trialNum++;
			
			killingMutatantTrace = generateMutatedTrace(params.getBkClassFiles(), mutation, killingMutatantTrace.getAppJavaClassPath(),
					buggyPrecheck, includedClassNames, excludedClassNames);
			correctTrace = generateCorrectTrace(params, correctTrace.getAppJavaClassPath(), correctPrecheck, 
					includedClassNames, excludedClassNames);
			
			DiffMatcher diffMatcher = new MuDiffMatcher(mutation.getSourceFolder(), orgFilePath, mutationFilePath);
			diffMatcher.matchCode();
			
			AppJavaClassPathWrapper.wrapAppClassPath(killingMutatantTrace, correctTrace, params.getBkClassFiles());
			
			long start = System.currentTimeMillis();
			ControlPathBasedTraceMatcher traceMatcher = new ControlPathBasedTraceMatcher();
			PairList pairList = traceMatcher.matchTraceNodePair(killingMutatantTrace, correctTrace, diffMatcher); 
			int matchTime = (int) (System.currentTimeMillis() - start);
			
			Simulator simulator = new Simulator(useSliceBreaker, false, breakLimit);
			simulator.prepare(killingMutatantTrace, correctTrace, pairList, diffMatcher);
			RootCauseFinder rootcauseFinder = new RootCauseFinder();
			rootcauseFinder.checkRootCause(simulator.getObservedFault(), killingMutatantTrace, correctTrace, pairList, diffMatcher);
			TraceNode rootCause = rootcauseFinder.retrieveRootCause(pairList, diffMatcher, killingMutatantTrace, correctTrace);
			
			if(rootCause==null){
				System.out.println("[Search Lib Class] Cannot find the root cause, I am searching for library classes...");
				
				List<TraceNode> buggySteps = rootcauseFinder.getStopStepsOnBuggyTrace();
				List<TraceNode> correctSteps = rootcauseFinder.getStopStepsOnCorrectTrace();
				
				List<String> newIncludedClassNames = new ArrayList<>();
				List<String> newIncludedBuggyClassNames = RegressionUtil.identifyIncludedClassNames(buggySteps, 
						buggyPrecheck, rootcauseFinder.getRegressionNodeList());
				List<String> newIncludedCorrectClassNames = RegressionUtil.identifyIncludedClassNames(correctSteps, 
						correctPrecheck, rootcauseFinder.getCorrectNodeList());
				newIncludedClassNames.addAll(newIncludedBuggyClassNames);
				newIncludedClassNames.addAll(newIncludedCorrectClassNames);
				boolean includedClassChanged = false;
				for(String name: newIncludedClassNames){
					if(!includedClassNames.contains(name)){
						includedClassNames.add(name);
						includedClassChanged = true;
					}
				}
				
				if(!includedClassChanged) {
					trialNum = trialLimit + 1;
				}
				else {
					continue;						
				}
			}
			
			isDataFlowComplete = true;
			boolean foundRootCause = rootCause!=null;
			
			
			boolean[] enableRandoms = new boolean[]{false, true};
			int[] breakLimits = new int[]{1, 3, 5};
			
			simulator.setUseSliceBreaker(true);
			List<EmpiricalTrial> returnTrials = new ArrayList<>();
			for(int i=0; i<enableRandoms.length; i++){
				for(int j=0; j<breakLimits.length; j++){
					simulator.setEnableRandom(enableRandoms[i]);
					simulator.setBreakerTrialLimit(breakLimits[j]);
					
					String fileName = "random-" + enableRandoms[i] + "-limit-" + breakLimits[j];
					
					List<EmpiricalTrial> trials = simulator.detectMutatedBug(killingMutatantTrace, correctTrace, diffMatcher, 0);
					
					if(i==0 && j==0){
						returnTrials = trials;
					}
					
					for (EmpiricalTrial t : trials) {
						t.setTraceMatchTime(matchTime);
						t.setBuggyTrace(killingMutatantTrace);
						t.setFixedTrace(correctTrace);
						t.setPairList(pairList);
						t.setDiffMatcher(diffMatcher);
						
						PatternIdentifier identifier = new PatternIdentifier();
						identifier.identifyPattern(t);
					}
					try {
						monitor.reportEmpiralTrial(fileName, trials, params, mutation);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			return new CheckResult(returnTrials, foundRootCause);
		}
		
		return new CheckResult(null, false);
	}
	
	private Trace generateCorrectTrace(AnalysisTestcaseParams params, AppJavaClassPath testcaseConfig,
			PreCheckInformation correctPrecheck, List<String> includedClassNames, List<String> excludedClassNames) {
		String outputFolder = params.getAnalysisOutputFolder();
		params.getBkClassFiles().restoreOrgClassFile();
		InstrumentationExecutor executor = new InstrumentationExecutor(testcaseConfig, outputFolder, "fix",
				includedClassNames, excludedClassNames);
		executor.setTimeout(EXECUTOR_TIMEOUT);
		RunningInformation info = executor.execute(correctPrecheck);
		return info.getTrace();
	}

	private Trace generateMutatedTrace(BackupClassFiles backupClassFiles, SingleMutation mutation, AppJavaClassPath testcaseConfig,
			PreCheckInformation buggyPrecheck, List<String> includedClassNames, List<String> excludedClassNames) {
		String traceDir = mutation.getMutationOutputFolder();
		backupClassFiles.restoreMutatedClassFile();
		InstrumentationExecutor executor = new InstrumentationExecutor(testcaseConfig, traceDir, "bug",
				includedClassNames, excludedClassNames);
		executor.setTimeout(EXECUTOR_TIMEOUT);
		RunningInformation runningInfo = executor.execute(buggyPrecheck);
		return runningInfo.getTrace();
	}

	public MutationExecutionResult runSingleMutationTrial(SingleMutation mutation, AnalysisTestcaseParams params,
			TraceExecutionInfo correctTraceInfo, IMutationExperimentMonitor monitor) {
		MutationExecutionResult result = new MutationExecutionResult();
		Trace correctTrace = correctTraceInfo.getTrace();
		result.correctTrace = correctTrace;
		MutationTrace muTrace = null;
		List<EmpiricalTrial> trials = Collections.emptyList();
		try {
			muTrace = generateMutatedTrace(correctTrace.getAppJavaClassPath(), params, mutation);
			
			if (muTrace != null) {
				result.bugTrace = muTrace.getTrace();
			}
			if (muTrace != null && muTrace.isTimeOut()) {
				System.out.println("Timeout, mutated file: " + mutation.getFile());
				System.out.println("skip Time Out test case: " + params.getTestcaseName());
			} else if (muTrace != null && muTrace.getTrace() != null && muTrace.getTrace().size() > 1) {
				ICompilationUnit iunit = JavaUtil.findNonCacheICompilationUnitInProject(mutation.getMutatedClass(),
						params.getProjectName());
				String orgFilePath = IResourceUtils.getAbsolutePathOsStr(iunit.getPath());
				String mutationFilePath = mutation.getFile().getAbsolutePath();
				
				CheckResult checkResult = checkRootCause(mutation, orgFilePath, mutationFilePath, 
						muTrace.getTrace(), correctTrace, params, muTrace.getTraceExecInfo().getPrecheckInfo(), 
						correctTraceInfo.getPrecheckInfo(), 
						useSliceBreaker, breakerLimit, monitor);
				trials = checkResult.trials;
				boolean foundRootCause = checkResult.foundRootCause;
				
				EmpiricalTrial trial = trials.get(0);
				String backupJFile = orgFilePath.replace(".java", "_bk.java");
				FileUtils.copyFile(orgFilePath, backupJFile, true);
				try {
					FileUtils.copyFile(mutationFilePath, orgFilePath, true);
					Settings.iCompilationUnitMap.remove(mutation.getMutatedClass());
					Settings.compilationUnitMap.remove(mutation.getMutatedClass());
					if(!trial.getDeadEndRecordList().isEmpty()){
						Repository.clearCache();
						DeadEndRecord record = trial.getDeadEndRecordList().get(0);
						String muBugId = mutation.getMutationBugId();
						DED datas = record.getTransformedData(trial.getBuggyTrace());
//						DED datas = new TrainingDataTransfer().transfer(record, trial.getBuggyTrace());
						setTestCase(datas, trial.getTestcase());						
//						new DeadEndReporter().export(datas.getAllData(), params.getProjectName(), muBugId);
						new DeadEndCSVWriter().export(datas.getAllData(), params.getProjectName(), muBugId);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					FileUtils.copyFile(backupJFile, orgFilePath, true);
					Settings.iCompilationUnitMap.remove(mutation.getMutatedClass());
					Settings.compilationUnitMap.remove(mutation.getMutatedClass());
					new File(backupJFile).delete();
					recoverOrgClassFile(params);
				}
				
//				monitor.reportTrial(params, correctTraceInfo, muTrace.getTraceExecInfo(), mutation, foundRootCause);
//				monitor.reportEmpiralTrial(null, trials, params, mutation);
				if (!foundRootCause && !DEBUG && muTrace.getTraceExecFile() == null) {
//					mutation.remove();
				} 
				result.isValid = foundRootCause;
			}
		} catch (Throwable e) {
			System.err.println("test case has exception when generating trace:");
			e.printStackTrace();
			try {
				monitor.reportTrial(params, correctTraceInfo, muTrace.getTraceExecInfo(), mutation, false);
				monitor.reportEmpiralTrial(null, trials, params, mutation);
				recoverOrgClassFile(params);
			} catch (Throwable e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} 
		if (result.bugTrace == null) {
//			mutation.remove();
		}
		return result;
	}

	private void recoverOrgClassFile(AnalysisTestcaseParams params) {
		BackupClassFiles bkClassFiles = params.getBkClassFiles();
		if (bkClassFiles != null) {
			FileUtils.copyFile(bkClassFiles.getOrgClassFilePath(), bkClassFiles.getClassFilePath(),
					true);
//						new File(bkClassFiles.getMutatedClassFilePath()).delete();
//						new File(bkClassFiles.getOrgClassFilePath()).delete();
		}
	}
	
	private List<ClassLocation> getAllExecutedLocations(Trace fixTrace) {
		Map<String, List<Integer>> locationMap = fixTrace.getExecutedLocation();
		List<ClassLocation> locations = new ArrayList<>();
		for (String compilationUnit : locationMap.keySet()) {
			for (Integer line : locationMap.get(compilationUnit)) {
				locations.add(new ClassLocation(compilationUnit, null, line));
			}
		}
		return locations;
	}
	
	private List<ClassLocation> findStaticMutationLocation(String junitClassName, List<ClassLocation> mutationLocs,
			AppJavaClassPath testcaseConfig) {
		Map<String, List<microbat.model.ClassLocation>> class2PointMap = BreakpointUtils.initBrkpsMap(toMicrobatClassLocation(mutationLocs));
		List<microbat.model.ClassLocation> matchingLocations = new ArrayList<>();
		for(String className: class2PointMap.keySet()){
			LineVisitor visitor = new LineVisitor(class2PointMap.get(className));
			ByteCodeParser.parse(className, visitor, testcaseConfig);
			matchingLocations.addAll(visitor.getResult());
		}
		return convertClassLocation(matchingLocations);
	}

	private List<microbat.model.ClassLocation> toMicrobatClassLocation(List<ClassLocation> locs) {
		List<microbat.model.ClassLocation> result = new ArrayList<>(locs.size());
		for (ClassLocation loc : locs) {
			result.add(new microbat.model.ClassLocation(loc.getClassCanonicalName(), loc.getMethodSign(), loc.getLineNo()));
		}
		return result;
	}

	private List<ClassLocation> convertClassLocation(List<microbat.model.ClassLocation> visitedLocations) {
		List<ClassLocation> locs = new ArrayList<>(visitedLocations.size());
		for (microbat.model.ClassLocation loc : visitedLocations) {
			locs.add(new ClassLocation(loc.getClassCanonicalName(), loc.getMethodSign(), loc.getLineNumber()));
		}
		return locs;
	}

	private void setTestCase(DED datas, String tc) {
		if(datas.getTrueData()!=null){
			datas.getTrueData().testcase = tc;					
		}
		for(DeadEndData data: datas.getFalseDatas()){
			data.testcase = tc;
		}
	}

	private MutationTrace executeTestcaseWithMutation(AppJavaClassPath testcaseConfig, String testCaseName,
			SingleMutation mutation) {
		MutationTrace muTrace = new MutationTrace();
		try{
			String traceDir = mutation.getMutationOutputFolder();
			List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
			List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
			InstrumentationExecutor executor = new InstrumentationExecutor(testcaseConfig, traceDir, "bug",
					includedClassNames, excludedClassNames);
			executor.setTimeout(EXECUTOR_TIMEOUT);
			PreCheckInformation precheck = executor.runPrecheck(null, STEP_LIMIT);
			
			muTrace.setTimeOut(precheck.isTimeout());
			muTrace.setKill(!precheck.isPassTest() && !precheck.isTimeout()); 
			
			String testMethod = testcaseConfig.getOptionalTestClass() + "#" + testcaseConfig.getOptionalTestMethod();
			
			if(muTrace.isKill()){
				System.out.println("KILLED: Now generating trace for " + testMethod + " (mutation: " + mutation.getFile() + ")");
				if(precheck.isOverLong()){
					System.out.println("The trace is over long for " + testMethod + " (mutation: " + mutation.getFile() + ")");
					muTrace.setTooLong(true);
				} else{
					System.out.println("A valid trace of " + precheck.getStepNum() + 
							" steps is to be generated for " + testMethod + " (mutation: " + mutation.getFile() + ")");
					long t1 = System.currentTimeMillis();
					RunningInformation info = executor.execute(precheck);
					if(info.isExpectedStepsMet()){
						Trace trace = info.getTrace();
						long t2 = System.currentTimeMillis();
						int time = (int) ((t2-t1)/1000);
						trace.setConstructTime(time);
						/* filling up trace */
						MuRegressionUtils.fillMuBkpJavaFilePath(trace, mutation.getFile().getAbsolutePath(),
								mutation.getMutatedClass());
						Regression.fillMissingInfo(trace, testcaseConfig);
						muTrace.setTrace(new TraceExecutionInfo(precheck, trace, executor.getTraceExecFilePath(), null));
					}
				}
			} else {
				System.out.println("FAIL TO KILL: " + testMethod + " (mutation: " + mutation.getFile() + ")");
			}
			
		}
		catch(TimeoutException e){
			e.printStackTrace();
			muTrace.setTimeOut(true);
		}
		return muTrace;
	}
	
	
	
	private MutationTrace generateMutatedTrace(AppJavaClassPath testcaseConfig, AnalysisTestcaseParams params,
			SingleMutation mutation) throws Exception {
//		Settings.compilationUnitMap.clear();
//		Settings.iCompilationUnitMap.clear();
		ICompilationUnit iunit = JavaUtil.findNonCacheICompilationUnitInProject(mutation.getMutatedClass(), params.getProjectName());
		CompilationUnit unit = JavaUtil.convertICompilationUnitToASTNode(iunit);
		Settings.iCompilationUnitMap.put(mutation.getMutatedClass(), iunit);
		Settings.compilationUnitMap.put(mutation.getMutatedClass(), unit);
		
		/* compile mutation file */
		// backup original .class file
		String targetFolder = IResourceUtils.getAbsolutePathOsStr(iunit.getJavaProject().getOutputLocation());
		String classFilePath = ClassUtils.getClassFilePath(targetFolder, mutation.getMutatedClass());
		String mutatedClassSimpleName = ClassUtils.getSimpleName(mutation.getMutatedClass());
		String bkOrgClassFilePath = ClassUtils.getClassFilePath(params.getAnalysisOutputFolder(),
				mutatedClassSimpleName);
		FileUtils.copyFile(classFilePath, bkOrgClassFilePath, true);
		String bkMutatedClassFilePath = null;
		try {
			JavaCompiler javaCompiler = new JavaCompiler(new VMConfiguration(testcaseConfig));
			javaCompiler.compile(targetFolder, mutation.getFile());

			/* generate trace */
			MutationTrace mutateInfo = executeTestcaseWithMutation(testcaseConfig, params.getTestcaseName(), mutation);
			bkMutatedClassFilePath = ClassUtils.getClassFilePath(mutation.getMutationOutputFolder(), mutatedClassSimpleName);
			FileUtils.copyFile(classFilePath, bkMutatedClassFilePath, true);
			return mutateInfo;
		} catch (SavException e) {
			System.out.println("Compilation error: " + e.getMessage());
			System.out.println();
		} finally {
			if (bkMutatedClassFilePath != null) {
				params.setBkClassFiles(new BackupClassFiles(classFilePath, bkOrgClassFilePath, bkMutatedClassFilePath));
			}
			/* revert */
			FileUtils.copyFile(bkOrgClassFilePath, classFilePath, true);
			
		}
		return null;
	}
	
	private List<ClassLocation> findMutationLocation(String junitClassName, List<ClassLocation> executingStatements,
			AppJavaClassPath appPath) {
		List<ClassLocation> locations = new ArrayList<>();
		for (ClassLocation point : executingStatements) {
			if (junitClassName.equals(point.getClassCanonicalName())) {
				continue; // ignore junitClass
			}
			ClassLocation location = new ClassLocation(point.getClassCanonicalName(), null, point.getLineNo());
			locations.add(location);
		}

		return locations;
	}
	
	private List<SingleMutation> mutate(Trace correctTrace, AnalysisTestcaseParams params) {
		AppJavaClassPath testcaseConfig = correctTrace.getAppJavaClassPath();
		List<ClassLocation> executingStatements = getAllExecutedLocations(correctTrace);
		List<ClassLocation> muLocations = findMutationLocation(params.getJunitClassName(), executingStatements, testcaseConfig);
		List<ClassLocation> staticCandidates = findStaticMutationLocation(params.getJunitClassName(), muLocations, testcaseConfig);
		
		if (muLocations.isEmpty() && staticCandidates.isEmpty()) {
			return Collections.emptyList();
		}
		
		filterLocationsInTestPackage(params.getTestSourceFolder(), muLocations);
		filterLocationsInTestPackage(params.getTestSourceFolder(), staticCandidates);
		
		ClassLocation cl = muLocations.isEmpty() ? staticCandidates.get(0) : muLocations.get(0);
		String cName = cl.getClassCanonicalName();
		String sourceFolderPath = MuRegressionUtils.getSourceFolder(cName, params.getProjectName());
		Mutator mutator = new Mutator(sourceFolderPath, params.getAnalysisOutputFolder(),
				params.getAnalysisParams().getMuTotal());
		MutationVisitor visitor = new TraceMutationVisitor(params.getAnalysisParams().getMutationTypes());
		Map<String, MutationResult> mutations = mutator.mutate(muLocations, visitor);
		
		if (params.getAnalysisParams().getMutationTypes().contains(MutationType.NEGATE_IF_CONDITION)) {
			visitor = new ControlDominatedMutationVisitor();
			Map<String, MutationResult> cdMutations = mutator.mutate(staticCandidates, visitor);
			MutationResult.merge(mutations, cdMutations);
		}
		
		List<SingleMutation> result = SingleMutation.from(mutations, params.getJunitClassName(),
				params.getTestMethod());
		return result;
	}

	private void filterLocationsInTestPackage(String testSrcFolderPath,
			List<ClassLocation> locationList) {
		Iterator<ClassLocation> iterator = locationList.iterator();
		while(iterator.hasNext()){
			ClassLocation location = iterator.next();
			String className = location.getClassCanonicalName();
			String fileName  = ClassUtils.getJFilePath(testSrcFolderPath, className);
			File file = new File(fileName);
			/* if location's owner class is in test source folder, then remove it */
			if(file.exists()){
				iterator.remove();
			}
		}
	}

	public static class MutationExecutionResult {
		Trace correctTrace;
		Trace bugTrace;
		boolean isLoopEffective;
		boolean isValid;

		public Trace getCorrectTrace() {
			return correctTrace;
		}

		public Trace getBugTrace() {
			return bugTrace;
		}

		public boolean isLoopEffective() {
			return isLoopEffective;
		}

		public boolean isValid() {
			return isValid;
		}
	}
}
