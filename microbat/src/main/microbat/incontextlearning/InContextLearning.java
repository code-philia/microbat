package microbat.incontextlearning;

import microbat.tracerecov.executionsimulator.ExecutionSimulator;

public interface InContextLearning {
    /***
     * Execute in context learning and return prompt for alias inference and
     * definition inference.
     * 
     * @param imports          imports of the target method
     * @param targetMethod     target method
     * @param targetLineNumber target line number
     * @param type             type of in context learning
     * @return prompt for alias inference and definition inference
     */
    public String executeInContextLearning(
            String imports,
            String targetMethod,
            int targetLineNumber,
            InContextLearningType type);

    /**
     * Set execution simulator (i.e. LLM invoker).
     * 
     * @param simulator execution simulator
     */
    public void setExecutionSimulator(ExecutionSimulator simulator);

    public static enum InContextLearningType {
        ALIAS_INFERENCE, DEFINITION_INFERENCE
    }
}
