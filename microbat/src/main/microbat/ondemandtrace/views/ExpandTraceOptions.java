package microbat.ondemandtrace.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import microbat.model.trace.TraceNode;
import microbat.ondemandtrace.model.traceskeleton.CodeBlockKeyIssuer;

/**
 * @author HongshuW
 */
public class ExpandTraceOptions {

	private ISelection step;
	private String launchClass;
	private String testCase;

	public ExpandTraceOptions(ISelection step, String launchClass, String testCase) {
		this.step = step;
		this.launchClass = launchClass;
		this.testCase = testCase;
	}

	public MenuManager getOptions() {
		if (step.isEmpty()) {
			return null;
		}

		if (step instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) step;
			TraceNode node = (TraceNode) selection.getFirstElement();

			List<ExpandCodeBlockAction> expandCodeBlockActions = getChildrenCodeBlockOptions(node);
			return getTraceExpansionMenu(expandCodeBlockActions);
		}

		return null;
	}

	private List<ExpandCodeBlockAction> getChildrenCodeBlockOptions(TraceNode node) {
		List<ExpandCodeBlockAction> expandCodeBlockActions = new ArrayList<>();

		String[] invokingMethods = node.getInvokingMethod().split("%");
		for (String method : invokingMethods) {
			String[] entries = method.split("#");
			if (entries.length != 2) {
				continue;
			}

			String codeBlockKey = CodeBlockKeyIssuer.getKeyForMethod(entries[0], entries[1]);
			expandCodeBlockActions.add(new ExpandCodeBlockAction(codeBlockKey, launchClass, testCase));
		}

		return expandCodeBlockActions;
	}

	private MenuManager getTraceExpansionMenu(List<ExpandCodeBlockAction> expandCodeBlockActions) {
		MenuManager menuMgr = new MenuManager("expand trace", "#TraceExpansionMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				expandCodeBlockActions.stream().forEach(action -> menuMgr.add(action));
			}
		});

		return menuMgr;
	}

}
