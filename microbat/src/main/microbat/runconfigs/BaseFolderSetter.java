package microbat.runconfigs;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseFolderSetter extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        Display.getDefault().asyncExec(() -> {
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
