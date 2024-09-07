package microbat.tracerecov.executionsimulator;

public class LLMTimer {
	public static int varExpansionTime = 0;
	public static int aliasInferTime = 0;
	public static int defInferTime = 0;
	
	public static void reset() {
		varExpansionTime = 0;
		aliasInferTime = 0;
		defInferTime = 0;
	}
}
