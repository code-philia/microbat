package microbat.runconfigs;

import java.io.FileReader;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteWithConfig<T> {
    private final Class<T> clazz;
    private final String taskName;
    private final Consumer<T> consumer;
    private final Gson gson;

    public ExecuteWithConfig(Class<T> clazz, String taskName, Consumer<T> consumer) {
        this.clazz = clazz;
        this.taskName = taskName;
        this.consumer = consumer;
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    }

    public void execute() {
        Display.getDefault().asyncExec(() -> {
            IWorkbench wb = PlatformUI.getWorkbench();
            IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
            if (window == null) {
                log.error("No active workbench window found.");
            }
            IWorkbenchPage page = window.getActivePage();
            if (page == null) {
                log.error("No active workbench page found.");
            }
            IEditorPart editorPart = page.getActiveEditor();
            if (!(editorPart instanceof ITextEditor)) {
                log.error("No active text editor found.");
            }
            ITextEditor textEditor = (ITextEditor) editorPart;
            ISelection selection = textEditor.getSelectionProvider().getSelection();
            if (selection == null) {
                log.error("No selection found in the text editor.");
            }
            if (!(selection instanceof ITextSelection)) {
                log.error("Selection is not a text selection.");
            }
            ITextSelection textSelection = (ITextSelection) selection;
            String selectedTextInEditor = textSelection.getText();
            log.info("Selected text: {}", selectedTextInEditor);

            T config = null;
            try (FileReader fis = new FileReader(selectedTextInEditor)) {
                config = gson.fromJson(fis, clazz);
            } catch (Exception e) {
                log.error("Error reading config file: {}", selectedTextInEditor, e);
                return;
            }

            String configString = gson.toJson(config);
            log.info("Config loaded for task {}: {}", taskName, configString);

            T configInner = config;

            Job job = new Job(taskName) {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        consumer.accept(configInner);
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        log.error("Error executing task: {}", taskName, e);
                        return new Status(IStatus.ERROR, taskName, "Error executing task", e);
                    }
                }
            };
            job.schedule();

        });
    }
}
