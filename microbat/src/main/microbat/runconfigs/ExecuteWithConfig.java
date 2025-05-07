package microbat.runconfigs;

import java.io.FileReader;
import java.net.URI;
import java.nio.file.Paths;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteWithConfig<T> {
    private final Class<T> clazz;
    private final String taskName;
    private final Consumer<ExecutionInfo<T>> consumer;
    private final Gson gson;

    @Getter
    @Setter
    private static String baseFolder = null;

    public ExecuteWithConfig(Class<T> clazz, String taskName, Consumer<ExecutionInfo<T>> consumer) {
        this.clazz = clazz;
        this.taskName = taskName;
        this.consumer = consumer;
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    }

    public void executeInDisplayThreadInner() throws Exception {
        IWorkbench wb = PlatformUI.getWorkbench();
        IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
        if (window == null) {
            throw new IllegalStateException("No active workbench window found.");
        }
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            throw new IllegalStateException("No active workbench page found.");
        }
        IEditorPart editorPart = page.getActiveEditor();
        if (!(editorPart instanceof ITextEditor)) {
            throw new IllegalStateException("No active text editor found.");
        }
        IEditorInput editorInput = editorPart.getEditorInput();
        if (editorInput == null) {
            throw new IllegalStateException("No editor input found.");
        }

        String descriptionFilePath = null;
        if (editorInput instanceof FileEditorInput) {
            IPath path = ((FileEditorInput) editorInput).getPath();
            descriptionFilePath = path.toOSString();
        } else if (editorInput instanceof IURIEditorInput) {
            URI uri = ((IURIEditorInput) editorInput).getURI();
            descriptionFilePath = Paths.get(uri).toString();
        } else {
            throw new IllegalArgumentException("Editor input is not a FileEditorInput or IURIEditorInput");
        }

        if (!(editorPart instanceof ITextEditor)) {
            throw new IllegalStateException("Active editor is not a text editor.");
        }
        ITextEditor textEditor = (ITextEditor) editorPart;
        ISelection selection = textEditor.getSelectionProvider().getSelection();
        if (selection == null || selection.isEmpty()) {
            throw new IllegalStateException("No selection found in the text editor.");
        }
        if (!(selection instanceof ITextSelection)) {
            throw new IllegalStateException("Selection is not a text selection.");
        }
        ITextSelection textSelection = (ITextSelection) selection;
        String selectedTextInEditor = textSelection.getText();

        ExecutionInfo<T> info = new ExecutionInfo<>();
        String configFile = info.resolveAndSetConfigPath(descriptionFilePath, selectedTextInEditor);

        T config = null;
        log.info("Loading config from file: {}", configFile);
        try (FileReader fis = new FileReader(configFile)) {
            config = gson.fromJson(fis, clazz);
        }

        String configString = gson.toJson(config);
        log.info("Config loaded for task {}: {}", taskName, configString);

        info.setTaskName(taskName);
        info.setConfig(config);

        Job job = new Job(taskName) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    consumer.accept(info);
                    return Status.OK_STATUS;
                } catch (Throwable e) {
                    log.error("Error executing task: {}", taskName, e);
                    return new Status(IStatus.ERROR, taskName, "Error executing task", e);
                }
            }
        };
        job.schedule();
    }

    private void executeInDisplayThread() {
        try {
            executeInDisplayThreadInner();
        } catch (Exception e) {
            log.error("Error executing task in display thread: {}", taskName, e);
        }
    }

    public void execute() {
        log.info("Executing task: {}", taskName);

        Display.getDefault().asyncExec(this::executeInDisplayThread);
    }
}
