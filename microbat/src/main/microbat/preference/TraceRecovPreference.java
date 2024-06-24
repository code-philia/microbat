package microbat.preference;

import java.util.stream.Stream;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

public class TraceRecovPreference extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String API_KEY = "api_key";
	public static final String MODEL_TYPE = "model_type";

	/* constants before update */
	private String apiKey;
	private LLMModel llmModelType = LLMModel.GPT4O; // default model

	/* constants after update */
	private Combo modelTypeCombo;
	private Text apiKeyText;

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
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;

		composite.setLayout(layout);

		createModelSelectionGroup(composite);

		performOk();

		return composite;
	}

	private void createModelSelectionGroup(final Composite parent) {
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

	private Group initGroup(final Composite parent, String title) {
		final Group modelSelectionGroup = new Group(parent, SWT.NONE);
		modelSelectionGroup.setText(title);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		modelSelectionGroup.setLayout(layout);

		modelSelectionGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		return modelSelectionGroup;
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

	public boolean performOk() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode("microbat.preference");

		preferences.put(API_KEY, this.apiKeyText.getText());
		preferences.put(MODEL_TYPE, this.modelTypeCombo.getText());

		Activator.getDefault().getPreferenceStore().putValue(API_KEY, this.apiKeyText.getText());
		Activator.getDefault().getPreferenceStore().putValue(MODEL_TYPE, this.modelTypeCombo.getText());

		SimulatorConstants.API_KEY = this.apiKeyText.getText();
		SimulatorConstants.modelType = LLMModel.valueOf(this.modelTypeCombo.getText());
		
		return true;
	}

}
