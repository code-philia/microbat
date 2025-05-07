package microbat.runconfigs;

import java.net.URI;
import java.nio.file.Paths;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseFolderSetter extends AbstractHandler {

    public void getBaseFolder() {
        try {
            IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .getActiveEditor();
            log.info("Editor part: {}", editorPart);
            IEditorInput editorInput = editorPart.getEditorInput();
            log.info("Editor input: {}", editorInput);
            if (editorInput instanceof FileEditorInput) {
                IPath path = ((FileEditorInput) editorInput).getPath();
                String location = path.toOSString();
                log.info("loc: {}", location);
            } else if (editorInput instanceof IURIEditorInput) {
                URI uri = ((IURIEditorInput) editorInput).getURI();
                String location = Paths.get(uri).toString();
                log.info("loc: {}", location);
            } else {
                log.error("Editor input is not a FileEditorInput or IURIEditorInput");
            }
        } catch (Exception e) {
            log.error("Error getting base folder: {}", e.getMessage());
        }
    }

    @Override
    public Object execute(ExecutionEvent event) {
        Display.getDefault().asyncExec(() -> {
            getBaseFolder();

            IWorkbench wb = PlatformUI.getWorkbench();
            IWorkbenchWindow window = wb.getActiveWorkbenchWindow();
            if (window == null) {
                log.error("No active workbench window found.");
                return;
            }
            IWorkbenchPage page = window.getActivePage();
            if (page == null) {
                log.error("No active workbench page found.");
                return;
            }
            IEditorPart editorPart = page.getActiveEditor();
            if (!(editorPart instanceof ITextEditor)) {
                log.error("No active text editor found.");
                return;
            }
            IFile file = (IFile) editorPart.getEditorInput().getAdapter(IFile.class);
            if (file == null) {
                log.error("No active file found.");
                return;
            }
            IPath path = file.getLocation();
            if (path == null) {
                log.error("No path found for the container.");
                return;
            }
            String location = path.toPath().getParent().toString();

            log.info("Base folder set to: {}", location);
            ExecuteWithConfig.setBaseFolder(location);
        });

        return null;
    }
}
