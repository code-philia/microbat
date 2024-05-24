package microbat.mutation.trace.handlers;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import microbat.mutation.trace.dto.AnalysisParams;
import microbat.mutation.trace.preference.MutationRegressionPreference;
import microbat.mutation.trace.preference.MutationRegressionSettings;
import microbat.mutation.trace.report.MutationExperimentMonitor;
import tregression.empiricalstudy.config.ProjectConfig;
import microbat.util.IProjectUtils;
import microbat.util.JavaUtil;
import microbat.util.WorkbenchUtils;
import microbat.mutation.trace.MutationGenerator;

public class GenerateMutantsHandler extends AbstractHandler{
	public final String mutationBaseFolder = "D:\\MutationProjects";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		Job job = new Job("Generate Mutations") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					System.out.println("--INFO-- Job Generate Mutations Starts...");				
					MutationRegressionSettings mutationSettings = MutationRegressionPreference.getMutationRegressionSettings();
					System.out.println("--INFO-- Mutation settings:");
					System.out.println(mutationSettings);
					
					if (mutationSettings.isRunAllProjectsInWorkspace()) {
						String[] allProjects = WorkbenchUtils.getProjectsInWorkspace();
						for (String targetProject : allProjects) {
							generateMutations(targetProject, mutationSettings, monitor);
						}
					} else {
						String targetProject = mutationSettings.getTargetProject();
						generateMutations(targetProject, mutationSettings, monitor);
					}
					
				} catch (JavaModelException | IOException e) {
					e.printStackTrace();
				}
				System.out.println("Complete Mutation Generation!");
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}

	private void generateMutations(String targetProject, MutationRegressionSettings mutationSettings, IProgressMonitor monitor) throws IOException, JavaModelException {
		System.out.println("********** Generate mutations for project: "+targetProject +" **********");
		
		AnalysisParams analysisParams = new AnalysisParams(mutationSettings);
		MutationExperimentMonitor experimentMonitor = new MutationExperimentMonitor(monitor, targetProject,analysisParams);
		
		// create a mutationGenerator for one project
		// TODO: fix config for other project structure
		ProjectConfig config = new ProjectConfig("test","src","build","build","build",targetProject,"0");
//		ProjectConfig config = new ProjectConfig("src\\test\\java","src\\main\\java","target\\test-classes","target\\classes","target",targetProject,"0");

		MutationGenerator mutationGenerator = new MutationGenerator(targetProject,config);
		IPackageFragmentRoot testRoot = JavaUtil.findTestPackageRootInProject(targetProject);
		if (testRoot == null) {
			System.out.println("--ERROR-- not found test packagement root...");
			return;
		}
		System.out.println("--INFO-- testRoot.children: "+testRoot.getChildren().length);
		for (IJavaElement element : testRoot.getChildren()) {
			if(!"".equals(element.getElementName())){
				System.out.println(element.getElementName());
			}
		}
		
		// copy fix project
		System.out.println("--INFO-- Copying project...");
		String projectFolder = IProjectUtils.getProjectFolder(testRoot.getJavaProject().getProject());
		Path fix_path = Paths.get(projectFolder);
		Path bug_path = Paths.get(mutationBaseFolder, targetProject);
		try {
			Files.walk(fix_path).forEach(source->{
				try {
					Path target = bug_path.resolve(fix_path.relativize(source));
					Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);
				} catch(Exception e) {
					System.err.println("--ERROR-- In copy file!");
					e.printStackTrace();
				}
			});
		} catch(Exception e) {
			System.err.println("--ERROR-- In copy project!");
			e.printStackTrace();
		}
		System.out.println("--INFO-- Successfully copy project!");

		// for each test package
		for (IJavaElement element : testRoot.getChildren()) {
			if (element.getElementName()!="" && element instanceof IPackageFragment && mutationGenerator.validMutationNum < 10) {
				System.out.println("--INFO-- Start generate mutations for package: "+element.getElementName());
				mutationGenerator.generateMutations((IPackageFragment) element, analysisParams, experimentMonitor);
			}
		}
		
		// delete copied project
        Files.walkFileTree(bug_path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
		
		System.out.println("********** Finish project: "+targetProject+" **********");
	}
}
