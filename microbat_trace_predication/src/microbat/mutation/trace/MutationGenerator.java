package microbat.mutation.trace;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import microbat.instrumentation.output.RunningInfo;
import microbat.model.trace.Trace;
import microbat.model.trace.TraceNode;
import microbat.mutation.mutation.OperationMutationVisitor;
import microbat.mutation.mutation.ControlDominatedMutationVisitor;
import microbat.mutation.mutation.MutationType;
import microbat.mutation.mutation.NodeIterator;
import microbat.mutation.mutation.TraceMutationVisitor;
import microbat.mutation.mutation.ValueMutationVisitor;
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
import microbat.util.IProjectUtils;
import microbat.util.IResourceUtils;
import microbat.util.JTestUtil;
import microbat.util.JavaUtil;
import microbat.util.Settings;
import mutation.mutator.MutationVisitor;
import mutation.mutator.Mutator;
import microbat.mutation.trace.AppJavaClassPathWrapper;
import microbat.mutation.trace.LineVisitor;
import microbat.mutation.trace.MuDiffMatcher;
import microbat.mutation.trace.MuRegressionUtils;
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
import tregression.empiricalstudy.EmpiricalTrial;
import tregression.empiricalstudy.Regression;
import tregression.empiricalstudy.RegressionUtil;
import tregression.empiricalstudy.RootCauseFinder;
import tregression.empiricalstudy.RootCauseNode;
import tregression.empiricalstudy.Simulator;
import tregression.empiricalstudy.TrialGenerator1;
import tregression.empiricalstudy.config.ProjectConfig;
import tregression.empiricalstudy.solutionpattern.PatternIdentifier;
import tregression.empiricalstudy.training.DED;
import tregression.model.PairList;
import tregression.separatesnapshots.DiffMatcher;
import tregression.tracematch.ControlPathBasedTraceMatcher;
import tregression.util.DebuggingTraceRecorder;

public class MutationGenerator {
	public static final int VALID_MU_LIMIT_PROJECT = 100; // mutants limit for each project
	public static final int VALID_MU_LIMIT_METHOD = 20; // mutants limit for each method
	
	public final String mutationBaseFolder = "D:\\MutationProjects";
	public final String tempFolder = "D:\\MutationProjects\\tmp";
	public final String outputFolder = "D:\\MutationProjects\\data\\debuggingTrace.txt";
	public String projectName;
	public int validMutationNum_proj;
	public int validMutationNum_method;
	ProjectConfig projectConfig;
	public Path mutationProjectFolder;
	public Path projectFolder;

	
	public MutationGenerator(String projectName,ProjectConfig config,Path mutationProjFolder,Path projFolder) {
		super();
		this.projectName = projectName;
		this.projectConfig = config;
		this.validMutationNum_proj = 0;
		this.mutationProjectFolder = mutationProjFolder;
		this.projectFolder = projFolder;
	}

	public void generateMutations(IPackageFragment pack, AnalysisParams analysisParams,
			IMutationExperimentMonitor monitor) throws JavaModelException {
		IMutationCaseChecker checker = monitor.getMutationCaseChecker();
		String testSourceFolder = null;
		String projectFolder = IProjectUtils.getProjectFolder(pack.getJavaProject().getProject());

		// for each package
		for (IJavaElement javaElement : pack.getChildren()) {
			if(validMutationNum_proj >= VALID_MU_LIMIT_PROJECT) {
				break;
			}
			System.out.println("--INFO-- generate mutation for class: "+javaElement.getElementName());
			if (javaElement instanceof IPackageFragment) {
				// for inner package
				generateMutations((IPackageFragment) javaElement, analysisParams, monitor);
			}
			else if (javaElement instanceof ICompilationUnit) {
				ICompilationUnit icu = (ICompilationUnit) javaElement;
				CompilationUnit cu = JavaUtil.convertICompilationUnitToASTNode(icu);
		        
				List<MethodDeclaration> testingMethods = JTestUtil.findTestingMethod(cu);
				if (testingMethods.isEmpty()) {
					System.out.println("--INFO-- testing methods is empty...");
					continue;
				}
				String className = JavaUtil.getFullNameOfCompilationUnit(cu);
				System.out.println("--INFO-- className: "+className);
				if (!checker.accept(className)) {
					System.out.println("--ERROR-- not accept...");
					continue;
				}
				
				validMutationNum_method = 0;
				// for each method
				for (MethodDeclaration testingMethod : testingMethods) {
					if(validMutationNum_method >= VALID_MU_LIMIT_METHOD || validMutationNum_proj >= VALID_MU_LIMIT_PROJECT) {
						break;
					}
					
					String methodName = testingMethod.getName().getIdentifier();
					if (monitor.isCanceled()) {
						return;
					}
					if (!checker.accept(className, methodName)) {
						System.out.println("--ERROR-- not accept...");
						continue;
					}
					AnalysisTestcaseParams tcParams = new AnalysisTestcaseParams(
							pack.getJavaProject().getElementName(), className, methodName, analysisParams, projectFolder);
					try {
						if (analysisParams.getIgnoredTestCaseFiles().contains(tcParams.getTestcaseName())) {
							continue;
						}
						if (testSourceFolder == null) {
							testSourceFolder = IResourceUtils.getSourceFolderPath(tcParams.getProjectName(), className);
						}
						tcParams.setTestSourceFolder(testSourceFolder);
//						collectTestcases(tcParams);
						
						// run correct test
						System.out.println("--INFO-- Run correct test...");
						TraceExecutionInfo correctTrace = executeTestcase(tcParams);
												
						// generate mutations and test
						System.out.println("--INFO-- Run single testcase...");
						runSingleTestcase(correctTrace, tcParams, monitor);
						System.out.println("--INFO-- Single testcase finishes...");
						
					} catch (Throwable e) {
						e.printStackTrace();
						tcParams.recoverOrgMutatedClassFile();
					}
				}
			}
			if (monitor.isCanceled()) {
				System.out.println("Process cancel!");
				return;
			}
		}
		System.out.println("Finish evaluation all for package: "+pack.getElementName());
	}
	
	// test correct version 
	public TraceExecutionInfo executeTestcase(AnalysisTestcaseParams params) {
		AppJavaClassPath testcaseConfig = MuRegressionUtils.createProjectClassPath(params);
		List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
		List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
		String outputFolder = params.getAnalysisOutputFolder();
		String traceExecPath = new StringBuilder(outputFolder).append(File.separator).append("fix.exec").toString();
		String precheckPath = traceExecPath.replace("fix.exec", "precheck.info");
		InstrumentationExecutor executor = new InstrumentationExecutor(testcaseConfig, traceExecPath,
				includedClassNames, excludedClassNames);
		executor.setTimeout(params.getAnalysisParams().getExecutionTimeout());
		PreCheckInformation precheckInfo = executor.runPrecheck(precheckPath, params.getAnalysisParams().getStepLimit());

		if (!precheckInfo.isPassTest() || precheckInfo.isOverLong()) {
			System.out.println(params.getTestcaseName() + " is failed to get build trace: " + precheckInfo.toString());
			params.getAnalysisParams().updateIgnoredTestcase(params.getTestcaseName());
			return null;
		}

		System.out.println(params.getTestcaseName() + " is a passed test case");
		RunningInfo info = executor.execute(precheckInfo);
		if (!info.isExpectedStepsMet()) {
			return null;
		}

		Trace correctTrace = info.getTraceList().get(0);
		Regression.fillMissingInfo(correctTrace, testcaseConfig);
		return new TraceExecutionInfo(precheckInfo, correctTrace, executor.getTraceExecFilePath(), precheckPath);
	}

	// for a test method, generate mutations and test
	public boolean runSingleTestcase(TraceExecutionInfo correctTrace, AnalysisTestcaseParams params,
			IMutationExperimentMonitor monitor) throws JavaModelException {
		
		// 1. Generate mutants
		if (correctTrace == null) {
			return false;
		}
		IMutationCaseChecker checker = monitor.getMutationCaseChecker();
		String testCaseName = params.getTestcaseName();
		
		System.out.println("mutating the tested methods of " + testCaseName);
		List<SingleMutation> mutations = mutate(correctTrace.getTrace(), params);
		System.out.println("mutation done for " + testCaseName+", total "+mutations.size()+" mutations.");
		if (mutations.isEmpty()) {
			System.out.println("What a pity, no proper mutants generated for " + testCaseName);
			return false;
		}
		
		// 2. Run mutants
		System.out.println("Start executing mutants for  " + testCaseName);
		System.out.println("=========== finding valid mutation =============");
		int i = 1;
		for (SingleMutation mutation : mutations) {
			if (monitor.isCanceled()) {
				return false;
			}
			System.out.println("*** run the mutation: " + i+" ***");
			if (!checker.accept(mutation.getMutationBugId(), MutationType.valueOf(mutation.getMutationType()))) {
				System.out.println("Not accepted mutation...");
				i+=1;
				continue;
			}
			try {
				MutationTrace muTrace = generateMutationTrace(correctTrace.getTrace().getAppJavaClassPath(), params, mutation);
				ICompilationUnit iunit = JavaUtil.findNonCacheICompilationUnitInProject(mutation.getMutatedClass(),params.getProjectName());
				String orgFilePath = IResourceUtils.getAbsolutePathOsStr(iunit.getPath());
				String mutationFilePath = mutation.getFile().getAbsolutePath();
				
				if (muTrace != null && muTrace.isValid()) {
					System.out.println("--INFO-- A valid mutation of type: "+mutation.getMutationType());
					
					validMutationNum_proj += 1;
					validMutationNum_method += 1;
					
					// create mutationProjectFolder/1/fix and mutationProjectFolder/1/bug
					Path fixFolder = Paths.get(mutationProjectFolder.toString(),String.valueOf(validMutationNum_proj),"fix");
					Path bugFolder = Paths.get(mutationProjectFolder.toString(),String.valueOf(validMutationNum_proj),"bug");
					Files.createDirectories(fixFolder);
					Files.createDirectories(bugFolder);
					
					// copy original project to fix and bug folder
					try {
						Files.walk(projectFolder).forEach(source->{
							try {
								Path target1 = fixFolder.resolve(projectFolder.relativize(source));
								Files.copy(source,target1,StandardCopyOption.REPLACE_EXISTING);
								
								Path target2 = bugFolder.resolve(projectFolder.relativize(source));
								Files.copy(source,target2,StandardCopyOption.REPLACE_EXISTING);
								
							} catch(Exception e) {
								System.err.println("--ERROR-- In copy file!");
								e.printStackTrace();
							}
						});
					} catch(Exception e) {
						System.err.println("--ERROR-- In copy project!");
						e.printStackTrace();
					}
					
					// substitute original .java and .class file with mutated ones
					// .java
					String orgFilePathInBug = orgFilePath.replace(projectFolder.toString(), bugFolder.toString());
					Files.copy(Paths.get(mutationFilePath), Paths.get(orgFilePathInBug), StandardCopyOption.REPLACE_EXISTING);
					
					// .class
					String mutationClassPath = mutationFilePath.replaceFirst("\\.java$", ".class");
					String orgClassPathInBug = orgFilePathInBug.replaceFirst("\\.java$", ".class");
					orgClassPathInBug = orgClassPathInBug.replaceFirst(
							projectConfig.srcSourceFolder.replace("\\","\\\\"), 
							projectConfig.bytecodeSourceFolder.replace("\\","\\\\"));
					
					Files.copy(Paths.get(mutationClassPath), Paths.get(orgClassPathInBug), StandardCopyOption.REPLACE_EXISTING);
					
					// add failing_test in bugFolder
					String sep = System.getProperty("file.separator");
			        File failing_test_file = new File(bugFolder + sep + "failing_test");
			        try (FileWriter writer = new FileWriter(failing_test_file)) {
			            writer.write("--- "+params.getJunitClassName()+"::"+params.getTestMethod());
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
			        
					// add mutation_info in bugFolder
			        File mutation_info_file = new File(bugFolder + sep + "mutation_info");
			        try (FileWriter writer = new FileWriter(mutation_info_file)) {
			            writer.write(mutation.toString());
			        } catch (IOException e) {
			            e.printStackTrace();
			        }
								        
					/* valid mutation, run tregression on it and fix version and record debugging trace */
					//checkRootCause(mutation, orgFilePath, mutationFilePath, muTrace.getTraceExecInfo(), correctTrace, params, monitor);					
					monitor.reportMutationCase(params, correctTrace, muTrace, mutation);
				}
				else {
					monitor.reportMutationCase(params, correctTrace, muTrace, mutation);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				params.recoverOrgMutatedClassFile();
				i+=1;
			}
		}
		
		System.out.println("================================================");
		return false;
	}
	
	public void runSimulator(SingleMutation mutation, AnalysisTestcaseParams params, Trace killingMutatantTrace,
			Trace correctTrace, DiffMatcher diffMatcher, PairList pairList) throws SimulationFailException {
		Simulator simulator = new Simulator(false, false, 0);
		simulator.prepare(killingMutatantTrace, correctTrace, pairList, diffMatcher);
		List<EmpiricalTrial> trials = simulator.detectMutatedBug(killingMutatantTrace, correctTrace, diffMatcher, 0);
		for (EmpiricalTrial t : trials) {
			t.setBuggyTrace(killingMutatantTrace);
			t.setFixedTrace(correctTrace);
			t.setPairList(pairList);
			t.setDiffMatcher(diffMatcher);
			
			PatternIdentifier identifier = new PatternIdentifier();
			identifier.identifyPattern(t);
		}
		
		if(!trials.isEmpty()) {
			EmpiricalTrial t = trials.get(0);
			for(DeadEndRecord record: t.getDeadEndRecordList()) {
				DED datas = record.getTransformedData(t.getBuggyTrace());
				try {
					new DeadEndCSVWriter("_mutation", null).export(datas.getAllData(), params.getProjectName(), mutation.getMutationBugId());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private Trace generateMutatedTrace(AnalysisTestcaseParams params, SingleMutation mutation, AppJavaClassPath testcaseConfig,
			PreCheckInformation buggyPrecheck, List<String> includedClassNames, List<String> excludedClassNames) {
		String traceDir = mutation.getMutationOutputFolder();
		params.getBkClassFiles().restoreMutatedClassFile();
		InstrumentationExecutor executor = new InstrumentationExecutor(testcaseConfig, traceDir, "bug",
				includedClassNames, excludedClassNames);
		executor.setTimeout(params.getAnalysisParams().getExecutionTimeout());
		RunningInfo runningInfo = executor.execute(buggyPrecheck);
		return runningInfo.getTraceList().get(0);
	}
	
	private Trace generateCorrectTrace(AnalysisTestcaseParams params, AppJavaClassPath testcaseConfig,
			PreCheckInformation correctPrecheck, List<String> includedClassNames, List<String> excludedClassNames) {
		String outputFolder = params.getAnalysisOutputFolder();
		params.getBkClassFiles().restoreOrgClassFile();
		InstrumentationExecutor executor = new InstrumentationExecutor(testcaseConfig, outputFolder, "fix",
				includedClassNames, excludedClassNames);
		executor.setTimeout(params.getAnalysisParams().getExecutionTimeout());
		RunningInfo info = executor.execute(correctPrecheck);
		return info.getTraceList().get(0);
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

	private MutationTrace executeTestcaseWithMutation(AppJavaClassPath testcaseConfig, AnalysisParams analysisParams,
			SingleMutation mutation) {
		MutationTrace muTrace = new MutationTrace();
		try{
			String traceDir = mutation.getMutationOutputFolder();
			List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
			List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
			String traceExecPath = new StringBuilder(traceDir).append(File.separator).append("bug.exec").toString();
			String precheckPath = traceExecPath.replace("bug.exec", "bugPrecheck.info");
			InstrumentationExecutor executor = new InstrumentationExecutor(testcaseConfig, traceExecPath,
					includedClassNames, excludedClassNames);
			executor.setTimeout(analysisParams.getExecutionTimeout());
			PreCheckInformation precheck = executor.runPrecheck(precheckPath, analysisParams.getStepLimit());
			
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
					RunningInfo info = executor.execute(precheck);
					if(info.isExpectedStepsMet()){
						Trace trace = info.getTraceList().get(0);
						long t2 = System.currentTimeMillis();
						int time = (int) ((t2-t1)/1000);
						trace.setConstructTime(time);
						/* filling up trace */
						MuRegressionUtils.fillMuBkpJavaFilePath(trace, mutation.getFile().getAbsolutePath(),
								mutation.getMutatedClass());
						Regression.fillMissingInfo(trace, testcaseConfig);
						muTrace.setTrace(new TraceExecutionInfo(precheck, trace, executor.getTraceExecFilePath(), precheckPath));
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
	
	private MutationTrace generateMutationTrace(AppJavaClassPath testcaseConfig, AnalysisTestcaseParams params,
			SingleMutation mutation) throws Exception {
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
			MutationTrace mutateInfo = executeTestcaseWithMutation(testcaseConfig, params.getAnalysisParams(), mutation);
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
		
		// other mutation type visitor
		MutationVisitor visitor = new TraceMutationVisitor(params.getAnalysisParams().getMutationTypes());
		Map<String, MutationResult> mutations = mutator.mutate(muLocations, visitor);
		
		if (params.getAnalysisParams().getMutationTypes().contains(MutationType.NEGATE_IF_CONDITION)) {
			visitor = new ControlDominatedMutationVisitor();
			Map<String, MutationResult> cdMutations = mutator.mutate(staticCandidates, visitor);
			MutationResult.merge(mutations, cdMutations);
		}
		
		List<ClassLocation> mutateLocations = muLocations.isEmpty()?staticCandidates:muLocations;
		if (params.getAnalysisParams().getMutationTypes().contains(MutationType.CHANGE_ARITHMETIC_OPERATOR)
				|| params.getAnalysisParams().getMutationTypes().contains(MutationType.CHANGE_CONDITIONALS_BOUNDARY)
				|| params.getAnalysisParams().getMutationTypes().contains(MutationType.SWAP_OPERANDS)) {
			visitor = new OperationMutationVisitor(params.getAnalysisParams().getMutationTypes());
			Map<String, MutationResult> cdMutations = mutator.mutate(mutateLocations, visitor);
			MutationResult.merge(mutations, cdMutations);
		}
		
		if (params.getAnalysisParams().getMutationTypes().contains(MutationType.CHANGE_RETURN)
				|| params.getAnalysisParams().getMutationTypes().contains(MutationType.CHANGE_LITERAL)) {
			visitor = new ValueMutationVisitor(params.getAnalysisParams().getMutationTypes());
			Map<String, MutationResult> cdMutations = mutator.mutate(mutateLocations, visitor);
			MutationResult.merge(mutations, cdMutations);
		}
		
		List<SingleMutation> result = SingleMutation.from(mutations, params.getJunitClassName(),params.getTestMethod());
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
	
	private void checkRootCause(SingleMutation mutation, String originFilePath, String mutationFilePath,
			TraceExecutionInfo mutationTraceInfo, TraceExecutionInfo correctTraceInfo, 
			AnalysisTestcaseParams params,IMutationExperimentMonitor monitor) throws SimulationFailException {
		
//		AppJavaClassPath testCaseConfig = correctTraceInfo.getTrace().getAppJavaClassPath();
//		AppJavaClassPathWrapper.wrapAppClassPath(mutationTraceInfo.getTrace(), correctTraceInfo.getTrace(),params.getBkClassFiles());
//		List<String> includedClassNames = AnalysisScopePreference.getIncludedLibList();
//		List<String> excludedClassNames = AnalysisScopePreference.getExcludedLibList();
//		PreCheckInformation buggyPrecheck = mutationTraceInfo.getPrecheckInfo();
//		PreCheckInformation correctPrecheck = correctTraceInfo.getPrecheckInfo();

		String projectFolder = params.getProjectFolder();
		String projName = params.getProjectName();
		Path fix_path = Paths.get(projectFolder);
		Path bug_path = Paths.get(mutationBaseFolder, params.getProjectName());
		
		// Step 1: substitute original file and class with buggy file and class
		String targetFilePath = originFilePath.replace(projectFolder,Paths.get(mutationBaseFolder,projName).toString());
        
		Path sourcePath = Paths.get(mutationFilePath);
        Path targetPath = Paths.get(targetFilePath);
        
		String sourceClassPath = mutationFilePath.replaceFirst("\\.java$", ".class");
		String targetClassPath = targetFilePath.replaceFirst("\\.java$", ".class");
		
		Path srcClassPath = Paths.get(sourceClassPath);
		Path tgtClassPath = Paths.get(
				targetClassPath.replaceFirst(projectConfig.srcSourceFolder.replace("\\","\\\\"), 
											 projectConfig.bytecodeSourceFolder.replace("\\","\\\\")));
		
		Path tempFilePath = Paths.get(tempFolder,targetPath.getFileName().toString());
		Path tempClassPath = Paths.get(tempFolder,tgtClassPath.getFileName().toString());
		
//		System.out.println("originFilePath: "+originFilePath);
//		System.out.println("targetFilePath: "+targetPath.toString());
//		System.out.println("sourceClassPath: "+sourceClassPath);
//		System.out.println("targetClassPath: "+tgtClassPath.toString());
		
		try {
			Files.move(targetPath,tempFilePath,StandardCopyOption.REPLACE_EXISTING);
			Files.move(tgtClassPath,tempClassPath,StandardCopyOption.REPLACE_EXISTING);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(srcClassPath, tgtClassPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
        	System.err.println("--ERROR-- In copy file!");
            e.printStackTrace();
        }
		
		
		// Step 2: run tregression
		System.out.println("*******************\n* Run Tregression *\n*******************");
		TrialGenerator1 generator1 = new TrialGenerator1();
		List<EmpiricalTrial> trials = generator1.generateTrials(bug_path.toString(), fix_path.toString(),
				false, false, false, 3, true, true, projectConfig, params.getTestcaseName());
		System.out.println("--INFO-- Trails generated");
		
		// Step 3: Record the analysis result
		DebuggingTraceRecorder recorder = new DebuggingTraceRecorder();
		recorder.recordDebuggingTrace(trials, projectName, 1, outputFolder);
		
		System.out.println("******************* Tregression Finishes *******************");
		
		// Step 4: move back the correct file for next mutation
		try {
			Files.delete(targetPath);
			Files.delete(tgtClassPath);
			Files.move(tempFilePath,targetPath,StandardCopyOption.REPLACE_EXISTING);
			Files.move(tempClassPath,tgtClassPath,StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
}
