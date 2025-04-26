package microbat.tracerecov.autoprompt.incontextlearning;

public class FailToExtractMethodException extends Exception {

	public FailToExtractMethodException(String filePath, int lineNo) {
		super("fail to extract method code containing line " + String.valueOf(lineNo) + " from file " + filePath);
	}

}
