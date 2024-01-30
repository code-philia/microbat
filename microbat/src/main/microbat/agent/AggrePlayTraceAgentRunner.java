package microbat.agent;

import java.util.List;

import microbat.instrumentation.AgentConstants;
import microbat.instrumentation.AgentParams;
import microbat.util.Settings;
import sav.common.core.SavException;
import sav.common.core.utils.CollectionBuilder;
import sav.common.core.utils.SingleTimer;
import sav.common.core.utils.StringUtils;
import sav.strategies.vm.AgentVmRunner;
import sav.strategies.vm.VMConfiguration;
import sav.strategies.vm.VMRunner;

/**
 * Runner used to abstract the running of the aggre play trace agent
 * @author Gabau
 *
 */
public class AggrePlayTraceAgentRunner extends AgentVmRunner implements Runnable {

	private boolean enableSettingHeapSize = true;
	private VMConfiguration configuration;
	private static final String VARIABLE_DUMP_FILEPATH= "shared_var.info";
	private static final String RECORDING_DUMP_PATH = "recording.info";
	public AggrePlayTraceAgentRunner(String agentJar, VMConfiguration vmConfig) {
		super(agentJar, AgentConstants.AGENT_OPTION_SEPARATOR, AgentConstants.AGENT_PARAMS_SEPARATOR);
		this.configuration = vmConfig;
	}
	
	private void setUp() {
		this.configuration.setDebug(Settings.isRunWtihDebugMode);
		this.configuration.setPort(9000);
	}
	
	@Override
	protected void buildVmOption(CollectionBuilder<String, ?> builder,
			VMConfiguration config) {
		builder.appendIf("-Xmx30g", enableSettingHeapSize);
		// builder.appendIf("-Xmn10g", enableSettingHeapSize);
		builder.appendIf("-XX:+UseG1GC", enableSettingHeapSize);
		super.buildVmOption(builder, config);
	}

	private void runSharedVarDetector() throws SavException {
		super.addAgentParam(AgentParams.OPT_SHARED_DETECTION, true);
		super.addAgentParam(AgentParams.OPT_DUMP_FILE, VARIABLE_DUMP_FILEPATH);
		super.startAndWaitUntilStop(getProgramArgs());
	}
	
	/**
	 * Method used for running the individual concurrent modes.
	 * @param mode
	 * @param concDumpFile The path to the conc dump
	 * @param dumpFile The dump file
	 * @throws SavException 
	 */
	public void concReplay(String mode, String concDumpFile, String dumpFile) throws SavException {
		setUp();
		addAgentParam(mode, true);
		addAgentParam(AgentParams.OPT_CONC_RECORD_DUMP, concDumpFile);
		addAgentParam(AgentParams.OPT_DUMP_FILE, dumpFile);
		super.startAndWaitUntilStop(this.configuration);
		removeAgentParam(mode);
	}
	
	/**
	 * Run's the three stages of the record and replay
	 */
	@Override
	public void run() {
		SingleTimer.start("Aggr");
		try {
			runSharedVarDetector();
		} catch (SavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

}
