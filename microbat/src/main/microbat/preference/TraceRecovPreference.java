package microbat.preference;

import java.util.stream.Stream;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import microbat.Activator;
import microbat.tracerecov.autoprompt.PromptType;
import microbat.tracerecov.executionsimulator.LLMModel;
import microbat.tracerecov.executionsimulator.SimulatorConstants;
import microbat.util.Settings;

public class TraceRecovPreference extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ENABLE_TRACERECOV = "enable_tracerecov";
	public static final String ENABLE_LLM = "enable_llm";
	public static final String USE_MUTATION_CONFIG = "use_mutation_config";
	public static final String API_KEY = "api_key";
	public static final String MODEL_TYPE = "model_type";
	public static final String METHOD_LAYER = "method_layer";
	public static final String COLLECT_PROMPT = "collect_prompt";
	public static final String ENABLE_LOGGING = "enable_logging";
	public static final String LOG_DEBUG_INFO = "log_debug_info";
	public static final String VAR_EXPAND_FILE_PATH = "var_expand_file_path";
	public static final String ALIAS_FILE_PATH = "alias_file_path";
	public static final String COLLECT_GROUND_TRUTH = "collect_ground_truth";
	public static final String PROMPT_TYPE = "prompt_type";

	/* constants before update */
	private boolean isEnableTraceRecov;
	private boolean isEnableLLMInference;
	private boolean isMutationExperiment;
	private String apiKey;
	private LLMModel llmModelType = LLMModel.GPT4O; // default model
	private String methodLayer;
	private boolean isCollectingPrompt;
	private boolean isEnableLogging;
	private boolean logDebugInfo;
	private String promptGTPath;
	private boolean collectGroundTruth;
	private PromptType promptType = PromptType.VAR_EXPANSION; // default prompt type for incontext learning

	/* constants after update */
	private Button isEnableTraceRecovButton;
	private Button isEnableLLMButton;
	private Button useMutationConfigButton;
	private Combo modelTypeCombo;
	private Text apiKeyText;
	private Text methodLayerText;
	private Button isCollectingPromptButton;
	private Button isEnableLoggingButton;
	private Button logDebugInfoButton;
	private Button collectGroundTruthButton;
	private Text promptGTPathText;
	private Text aliasFilePathText;
	private Combo promptTypeCombo;

	public TraceRecovPreference() {
	}

	public TraceRecovPreference(String title) {
		super(title);
	}

	public TraceRecovPreference(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		String isEnableTraceRecovString = Activator.getDefault().getPreferenceStore().getString(ENABLE_TRACERECOV);
		if (isEnableTraceRecovString != null && isEnableTraceRecovString.equals("true")) {
			this.isEnableTraceRecov = true;
		} else {
			this.isEnableTraceRecov = false;
		}

		String isEnableLLMInferenceString = Activator.getDefault().getPreferenceStore().getString(ENABLE_LLM);
		if (isEnableLLMInferenceString != null && isEnableLLMInferenceString.equals("true")) {
			this.isEnableLLMInference = true;
		} else {
			this.isEnableLLMInference = false;
		}

		String isMutationExperimentString = Activator.getDefault().getPreferenceStore().getString(USE_MUTATION_CONFIG);
		if (isMutationExperimentString != null && isMutationExperimentString.equals("true")) {
			this.isMutationExperiment = true;
		} else {
			this.isMutationExperiment = false;
		}

		this.apiKey = Activator.getDefault().getPreferenceStore().getString(API_KEY);

		String modelType = Activator.getDefault().getPreferenceStore().getString(MODEL_TYPE);
		if (modelType != null && !modelType.equals("")) {
			try {
				this.llmModelType = LLMModel.valueOf(modelType);
			} catch (IllegalArgumentException e) {
				this.llmModelType = LLMModel.GPT4O; // default model if unknown value
			}
		}

		this.methodLayer = Activator.getDefault().getPreferenceStore().getString(METHOD_LAYER);

		String isCollectingPromptString = Activator.getDefault().getPreferenceStore().getString(COLLECT_PROMPT);
		if (isCollectingPromptString != null && isCollectingPromptString.equals("true")) {
			this.isCollectingPrompt = true;
		} else {
			this.isCollectingPrompt = false;
		}

		String isEnableLoggingString = Activator.getDefault().getPreferenceStore().getString(ENABLE_LOGGING);
		if (isEnableLoggingString != null && isEnableLoggingString.equals("true")) {
			this.isEnableLogging = true;
		} else {
			this.isEnableLogging = false;
		}

		String logDebugInfoString = Activator.getDefault().getPreferenceStore().getString(LOG_DEBUG_INFO);
		if (logDebugInfoString != null && logDebugInfoString.equals("true")) {
			this.logDebugInfo = true;
		} else {
			this.logDebugInfo = false;
		}

		String collectGTString = Activator.getDefault().getPreferenceStore().getString(COLLECT_GROUND_TRUTH);
		if (collectGTString != null && collectGTString.equals("true")) {
			this.collectGroundTruth = true;
		} else {
			this.collectGroundTruth = false;
		}

		this.promptGTPath = Activator.getDefault().getPreferenceStore().getString(VAR_EXPAND_FILE_PATH);

		String promptType = Activator.getDefault().getPreferenceStore().getString(PROMPT_TYPE);
		if (promptType != null && !promptType.equals("")) {
			try {
				this.promptType = PromptType.valueOf(promptType);
			} catch (IllegalArgumentException e) {
				this.promptType = PromptType.VAR_EXPANSION; // default prompt type if unknown value
			}
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);

		composite.setLayout(layout);

		// Experiment Settings
		createExperimentSettingGroup(composite);

		// LLM Model Settings
		createModelConfigGroup(composite);

		// RQ1: Shorten Trace Settings
		createShortenTraceSettingGroup(composite);

		// RQ3: Logging Settings
		createLogSettingGroup(composite);

		performOk();
		return composite;
	}

	private void createExperimentSettingGroup(final Composite parent) {
		String title = "Experiment Settings";
		Group experimentSettingGroup = initGroup(parent, title);

		String enableTraceRecovLabel = "Enable TraceRecov";
		this.isEnableTraceRecovButton = createCheckButton(experimentSettingGroup, enableTraceRecovLabel,
				this.isEnableTraceRecov);

		String enableLLMLabel = "Enable TraceRecov in Auto Root Cause Localization";
		this.isEnableLLMButton = createCheckButton(experimentSettingGroup, enableLLMLabel, this.isEnableLLMInference);

		String mutationLabel = "Use Mutation Configuration";
		this.useMutationConfigButton = createCheckButton(experimentSettingGroup, mutationLabel,
				this.isMutationExperiment);

		String colleatGTLabel = "Collect Ground Truth";
		this.collectGroundTruthButton = createCheckButton(experimentSettingGroup, colleatGTLabel,
				this.collectGroundTruth);
	}

	private void createModelConfigGroup(final Composite parent) {
		String title = "LLM Model Settings";
		Group modelSelectionGroup = initGroup(parent, title);

		String modelSelectionLabel = "LLM Model:";
		this.modelTypeCombo = createDropDown(modelSelectionGroup, modelSelectionLabel, LLMModel.values(),
				this.llmModelType.ordinal());

		String apiKeyLabel = "API Key:";
		this.apiKeyText = createText(modelSelectionGroup, apiKeyLabel, this.apiKey);
	}

	private void createShortenTraceSettingGroup(Composite parent) {
		String title = "Shorten Trace Settings";
		Group shortenTraceSettingGroup = initGroup(parent, title);

		String methodLayerLabel = "Method Layer Expanded:";
		this.methodLayerText = createText(shortenTraceSettingGroup, methodLayerLabel, this.methodLayer);
	}

	private void createLogSettingGroup(final Composite parent) {
		String title = "Log Settings";
		Group logSettingGroup = initGroup(parent, title);

		String collectPromptLabel = "RQ3: Collect and Label Prompts";
		this.isCollectingPromptButton = createCheckButton(logSettingGroup, collectPromptLabel, this.isCollectingPrompt);

		String enableLogLabel = "Enable Logging";
		this.isEnableLoggingButton = createCheckButton(logSettingGroup, enableLogLabel, this.isEnableLogging);

		String logDebugInfoLabel = "Log Debug Info";
		this.logDebugInfoButton = createCheckButton(logSettingGroup, logDebugInfoLabel, this.logDebugInfo);

		String promptSelectionLabel = "Type of Prompt (for auto incontext learning experiment, independent from TraceRecov):";
		this.promptTypeCombo = createDropDown(logSettingGroup, promptSelectionLabel, PromptType.values(),
				this.promptType.ordinal());

		String promptGTPathLabel = "Path for Prompt Ground Truth:";
		this.promptGTPathText = createText(logSettingGroup, promptGTPathLabel, this.promptGTPath);

		String aliasFilePathLabel = "Path for Alias File:";
		this.aliasFilePathText = createText(logSettingGroup, aliasFilePathLabel,
				Activator.getDefault().getPreferenceStore().getString(ALIAS_FILE_PATH));
	}

	private Group initGroup(final Composite parent, String title) {
		final Group group = new Group(parent, SWT.NONE);
		group.setText(title);

		GridLayout layout = new GridLayout(2, false);
		group.setLayout(layout);

		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		return group;
	}

	private <T extends Enum<T>> Combo createDropDown(Group settingGroup, String textLabel, T[] enumOptions,
			int defaultSelection) {
		final Label label = new Label(settingGroup, SWT.NONE);
		label.setText(textLabel);

		Combo contentCombo = new Combo(settingGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		final String[] selectionNames = Stream.of(enumOptions).map(Enum::name).toArray(String[]::new);
		contentCombo.setItems(selectionNames);
		contentCombo.select(defaultSelection);

		GridData comboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		comboData.horizontalAlignment = GridData.END;
		contentCombo.setLayoutData(comboData);

		return contentCombo;
	}

	private Text createText(Group settingGroup, String textLabel, String textContent) {
		final Label label = new Label(settingGroup, SWT.NONE);
		label.setText(textLabel);

		Text text = new Text(settingGroup, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
		text.setText(textContent);

		GridData textData = new GridData(SWT.FILL, SWT.LEFT, true, false);
		text.setLayoutData(textData);

		return text;
	}

	private Button createCheckButton(Group settingGroup, String textLabel, boolean defaultValue) {
		Button button = new Button(settingGroup, SWT.CHECK);
		button.setText(textLabel);

		GridData buttonData = new GridData(SWT.FILL, SWT.FILL, true, false);
		buttonData.horizontalSpan = 2;
		button.setLayoutData(buttonData);

		button.setSelection(defaultValue);

		return button;
	}

	public boolean performOk() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("microbat.preference");

		preferences.put(ENABLE_TRACERECOV, String.valueOf(this.isEnableTraceRecovButton.getSelection()));
		preferences.put(ENABLE_LLM, String.valueOf(this.isEnableLLMButton.getSelection()));
		preferences.put(USE_MUTATION_CONFIG, String.valueOf(this.useMutationConfigButton.getSelection()));
		preferences.put(COLLECT_GROUND_TRUTH, String.valueOf(this.collectGroundTruthButton.getSelection()));
		preferences.put(API_KEY, this.apiKeyText.getText());
		preferences.put(MODEL_TYPE, this.modelTypeCombo.getText());
		preferences.put(METHOD_LAYER, this.methodLayerText.getText());
		preferences.put(COLLECT_PROMPT, String.valueOf(this.isCollectingPromptButton.getSelection()));
		preferences.put(ENABLE_LOGGING, String.valueOf(this.isEnableLoggingButton.getSelection()));
		preferences.put(LOG_DEBUG_INFO, String.valueOf(this.logDebugInfoButton.getSelection()));
		preferences.put(VAR_EXPAND_FILE_PATH, this.promptGTPathText.getText());
		preferences.put(ALIAS_FILE_PATH, this.aliasFilePathText.getText());
		preferences.put(PROMPT_TYPE, this.promptTypeCombo.getText());

		Activator.getDefault().getPreferenceStore().putValue(ENABLE_TRACERECOV,
				String.valueOf(this.isEnableTraceRecovButton.getSelection()));
		Activator.getDefault().getPreferenceStore().putValue(ENABLE_LLM,
				String.valueOf(this.isEnableLLMButton.getSelection()));
		Activator.getDefault().getPreferenceStore().putValue(USE_MUTATION_CONFIG,
				String.valueOf(this.useMutationConfigButton.getSelection()));
		Activator.getDefault().getPreferenceStore().putValue(COLLECT_GROUND_TRUTH,
				String.valueOf(this.collectGroundTruthButton.getSelection()));
		Activator.getDefault().getPreferenceStore().putValue(API_KEY, this.apiKeyText.getText());
		Activator.getDefault().getPreferenceStore().putValue(MODEL_TYPE, this.modelTypeCombo.getText());
		Activator.getDefault().getPreferenceStore().putValue(METHOD_LAYER, this.methodLayerText.getText());
		Activator.getDefault().getPreferenceStore().putValue(COLLECT_PROMPT,
				String.valueOf(this.isCollectingPromptButton.getSelection()));
		Activator.getDefault().getPreferenceStore().putValue(ENABLE_LOGGING,
				String.valueOf(this.isEnableLoggingButton.getSelection()));
		Activator.getDefault().getPreferenceStore().putValue(LOG_DEBUG_INFO,
				String.valueOf(this.logDebugInfoButton.getSelection()));
		Activator.getDefault().getPreferenceStore().putValue(VAR_EXPAND_FILE_PATH, this.promptGTPathText.getText());
		Activator.getDefault().getPreferenceStore().putValue(ALIAS_FILE_PATH, this.aliasFilePathText.getText());
		Activator.getDefault().getPreferenceStore().putValue(PROMPT_TYPE, this.promptTypeCombo.getText());

		Settings.isEnableGPTInference = this.isEnableLLMButton.getSelection();
		SimulatorConstants.API_KEY = this.apiKeyText.getText();
		SimulatorConstants.modelType = LLMModel.valueOf(this.modelTypeCombo.getText());

		return true;
	}

	public static String getMethodLayer() {
		return Activator.getDefault().getPreferenceStore().getString(METHOD_LAYER);
	}
}
