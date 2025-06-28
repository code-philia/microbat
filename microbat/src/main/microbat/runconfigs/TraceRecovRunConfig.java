package microbat.runconfigs;

import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

import lombok.Getter;
import lombok.Setter;
import microbat.Activator;
import microbat.preference.MicrobatPreference;
import microbat.preference.RecovSlicingPreference;
import microbat.tracerecov.executionsimulator.ExecutionSimulator;
import microbat.tracerecov.executionsimulator.SimulatorConstants;

@Getter
@Setter
public class TraceRecovRunConfig {
    private GptConfig gptConfig = new GptConfig();
    private JdkConfig jdkConfig = new JdkConfig();
    private TraceConfig traceConfig = new TraceConfig();

    private boolean enableTraceRecov = true;
    private boolean enableLogging = true;
    private boolean guava = false;
    private boolean multiFiles = true;

    private String datasetFolder = null;
    private boolean utilizeJunitInsteadOfMain = false;
    private boolean enableInContextLearning = true;
    private boolean enableAliasInference = true;
    private boolean enableReExecution = false;
    private boolean generatedDataset = true;

    private String resultFolderName = null;
    private String inContextLearningPath = ".";
    private String dumpGptPath = null;

    private List<String> onlyRun = null;
    private List<String> skip = null;

    private String methodProcessUrl = "http://127.0.0.1:3322/process";

    public void setToGlobal() {
        IPreferenceStore preferences = Activator.getDefault().getPreferenceStore();
        preferences.putValue(RecovSlicingPreference.ENABLE_TRACERECOV,
                String.valueOf(enableTraceRecov));
        preferences.putValue(RecovSlicingPreference.ENABLE_LLM,
                String.valueOf(enableTraceRecov));
        SimulatorConstants.API_KEY = gptConfig.getGptApiKey();
        SimulatorConstants.TB_API_KEY = gptConfig.getGptApiKey();
        SimulatorConstants.MODEL_OVERRIDE = gptConfig.getGptModel();
        SimulatorConstants.GPT_API_ENDPOINT_OVERRIDE = gptConfig.getGptBaseUrl();
        SimulatorConstants.MAX_TOKENS = gptConfig.getMaxTokens();

        preferences.putValue(MicrobatPreference.JAVA7HOME_PATH, jdkConfig.getJavaHome());
        preferences.putValue(MicrobatPreference.STEP_LIMIT, String.valueOf(traceConfig.getStepLimit()));
        preferences.putValue(MicrobatPreference.VARIABLE_LAYER, String.valueOf(traceConfig.getVariableLayers()));

        preferences.putValue(RecovSlicingPreference.ENABLE_LOGGING, String.valueOf(enableLogging));
        preferences.putValue(RecovSlicingPreference.LOG_DEBUG_INFO, String.valueOf(enableLogging));
        preferences.putValue(RecovSlicingPreference.ENABLE_IN_CONTEXT_LEARNING,
                String.valueOf(enableInContextLearning));
        preferences.putValue(RecovSlicingPreference.ENABLE_ALIAS_INFERENCE,
                String.valueOf(enableAliasInference));
        preferences.putValue(RecovSlicingPreference.ENABLE_RE_EXECUTION,
                String.valueOf(enableReExecution));

        preferences.putValue(RecovSlicingPreference.INCONTEXT_FILE_PATH,
                String.valueOf(inContextLearningPath));

        ExecutionSimulator.dumpFilePath = dumpGptPath;
        ExecutionSimulator.initDumpOutputStream();
    }
}
