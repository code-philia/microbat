package microbat.ondemandtrace.views;

import org.eclipse.jface.action.Action;

import microbat.ondemandtrace.model.tracestates.TraceStateQuerier;

/**
 * This class represents an action that activates instrumentation for a code
 * block, then re-execute to record trace for this block.
 * 
 * @author HongshuW
 */
public class ExpandCodeBlockAction extends Action {

	private String codeBlockID;

	public ExpandCodeBlockAction(String codeBlockID) {
		super();
		this.codeBlockID = codeBlockID;
	}

	@Override
	public void run() {
		System.out.println(codeBlockID);
		
		TraceStateQuerier._updateStatusToRecord(codeBlockID);
		
		// TODO: re-execute
	}

	@Override
	public String getText() {
		return codeBlockID;
	}

}
