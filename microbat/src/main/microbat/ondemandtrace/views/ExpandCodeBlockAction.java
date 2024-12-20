package microbat.ondemandtrace.views;

import org.eclipse.jface.action.Action;

/**
 * @author HongshuW
 */
public class ExpandCodeBlockAction extends Action {

	private String codeBlockID;

	public ExpandCodeBlockAction(String codeBlockID) {
		super();
		this.codeBlockID = codeBlockID;
	}
	
	@Override
	public String getText() {
		return codeBlockID;
	}

}
