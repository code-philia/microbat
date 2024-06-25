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
import microbat.tracerecov.executionsimulator.LLMModel;
import microbat.tracerecov.executionsimulator.SimulatorConstants;
import microbat.util.Settings;

public class TraceRecovPreference extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String API_KEY = "api_key";
	public static final String MODEL_TYPE = "model_type";
	public static final String ENABLE_LLM = "enable_llm";
	public static final String USE_MUTATION_CONFIG = "use_mutation_config";

	/* constants before update */
	private String apiKey;
	private LLMModel llmModelType = LLMModel.GPT4O; // default model
	private boolean isEnableLLMInference;
	private boolean isMutationExperiment;

	/* constants after update */
	private Combo modelTypeCombo;
	private Text apiKeyText;
	private Button isEnableLLMButton;
	private Button useMutationConfigButton;

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
		this.apiKey = Activator.getDefault().getPreferenceStore().getString(API_KEY);

		String modelType = Activator.getDefault().getPreferenceStore().getString(MODEL_TYPE);
		if (modelType != null && !modelType.equals("")) {
			this.llmModelType = LLMModel.valueOf(modelType);
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
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);

		composite.setLayout(layout);

		// LLM Model Settings
		createModelConfigGroup(composite);

		// Experiment Settings
		createExperimentSettingGroup(composite);

		performOk();

		return composite;
	}

	private void createModelConfigGroup(final Composite parent) {
		String title = "LLM Model Settings";
		Group modelSelectionGroup = initGroup(parent, title);

		String modelSelectionLabel = "LLM Model:";
		Combo modelTypeCombo = createDropDown(modelSelectionGroup, modelSelectionLabel, LLMModel.values(),
				this.llmModelType.ordinal());
		this.modelTypeCombo = modelTypeCombo;

		String apiKeyLabel = "API Key:";
		Text apiKeyText = createText(modelSelectionGroup, apiKeyLabel, this.apiKey);
		this.apiKeyText = apiKeyText;
	}

	private void createExperimentSettingGroup(final Composite parent) {
		String title = "Experiment Settings";
		Group experimentSettingGroup = initGroup(parent, title);

		String enableLLMLabel = "Enable LLM Inference";
		Button isEnableLLMButton = createCheckButton(experimentSettingGroup, enableLLMLabel, this.isEnableLLMInference);
		this.isEnableLLMButton = isEnableLLMButton;
		
		String mutationLabel = "Use Mutation Configuration";
		Button useMutationConfigButton = createCheckButton(experimentSettingGroup, mutationLabel, this.isMutationExperiment);
		this.useMutationConfigButton = useMutationConfigButton;

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
		button.setLayoutData(buttonData);

		button.setSelection(defaultValue);

		return button;
	}

	public boolean performOk() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("microbat.preference");

		preferences.put(API_KEY, this.apiKeyText.getText());
		preferences.put(MODEL_TYPE, this.modelTypeCombo.getText());
		preferences.put(ENABLE_LLM, String.valueOf(this.isEnableLLMButton.getSelection()));
		preferences.put(USE_MUTATION_CONFIG, String.valueOf(this.useMutationConfigButton.getSelection()));

		Activator.getDefault().getPreferenceStore().putValue(API_KEY, this.apiKeyText.getText());
		Activator.getDefault().getPreferenceStore().putValue(MODEL_TYPE, this.modelTypeCombo.getText());
		Activator.getDefault().getPreferenceStore().putValue(ENABLE_LLM,
				String.valueOf(this.isEnableLLMButton.getSelection()));
		Activator.getDefault().getPreferenceStore().putValue(USE_MUTATION_CONFIG,
				String.valueOf(this.useMutationConfigButton.getSelection()));

		SimulatorConstants.API_KEY = this.apiKeyText.getText();
		SimulatorConstants.modelType = LLMModel.valueOf(this.modelTypeCombo.getText());
		Settings.isEnableGPTInference = this.isEnableLLMButton.getSelection();

		return true;
	}

}
