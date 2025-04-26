package microbat.runconfigs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TraceRecovRunConfig {
    private GptConfig gptConfig = new GptConfig();
    private JdkConfig jdkConfig = new JdkConfig();
    private TraceConfig traceConfig = new TraceConfig();

    private boolean enableTraceRecov = true;
    private boolean enableLogging = true;

    private String datasetFolder = null;
    private boolean utilizeJunitInsteadOfMain = false;
    private boolean enableInContextLearning = true;
    private boolean enableAliasInference = true;
    private boolean enableReExecution = false;
}
