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
	private String launchClass;
	private String testCase;

	public ExpandCodeBlockAction(String codeBlockID, String launchClass, String testCase) {
		super();
		this.codeBlockID = codeBlockID;
		this.launchClass = launchClass;
		this.testCase = testCase;
	}

	@Override
	public void run() {
		System.out.println(codeBlockID);

		String traceStatesPath = System.getProperty("java.io.tmpdir") + launchClass + "#" + testCase + ".txt";
		TraceStateQuerier.init(traceStatesPath);

		TraceStateQuerier._updateStatusToRecord(codeBlockID);

		// TODO: re-execute
	}

	@Override
	public String getText() {
		return codeBlockID;
	}

}
