package microbat.tracerecov.autoprompt.incontextlearning;

import java.util.List;

import microbat.model.value.VarValue;
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
            InContextLearningType type,
            ContextVariablesToString contextToString);

    /**
     * Set execution simulator (i.e. LLM invoker).
     * 
     * @param simulator execution simulator
     */
    public void setExecutionSimulator(ExecutionSimulator simulator);

    public static enum InContextLearningType {
        VAR_EXPANSION, ALIAS_INFERENCE, DEFINITION_INFERENCE
    }

    @FunctionalInterface
    public static interface ContextVariablesToString {
        public String contextToString(ContextVariables context, InContextLearningType type);
    }

    public static interface ContextVariables {
        /**
         * Get all written variables in the marker line including written variables in
         * libraries.
         */
        public List<VarValue> getAllWrittenVariables();

        /**
         * Get all read variables in the marker line including read variables in
         * libraries.
         */
        public List<VarValue> getAllReadVariables();

        /**
         * Get written variables in the marker line excluding written variables in
         * libraries.
         */
        public List<VarValue> getOuterWrittenVariables();

        /**
         * Get read variables in the marker line excluding read variables in libraries.
         */
        public List<VarValue> getOuterReadVariables();

        /**
         * Get executed test case code. This code includes extra methods to execute the
         * test case.
         */
        public String getCode();

        /**
         * Get gpt generated code.
         */
        public String getCodeToView();

        /**
         * Get target code line number. (The first line number is 1.)
         */
        public int getMarkerLine();
    }
}
