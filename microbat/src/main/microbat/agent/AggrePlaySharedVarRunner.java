package microbat.agent;

import java.util.Collection;

import microbat.instrumentation.AgentConstants;
import microbat.instrumentation.AgentParams;
import sav.common.core.SavException;
import sav.common.core.utils.CollectionBuilder;
import sav.strategies.vm.AgentVmRunner;
import sav.strategies.vm.VMConfiguration;

public class AggrePlaySharedVarRunner extends AgentVmRunner implements Runnable {
	private static final String STORAGE_FILE = "shared_var.txt";
	
	public AggrePlaySharedVarRunner(String agentJar, VMConfiguration vmConfig) {
		super(agentJar, AgentConstants.AGENT_OPTION_SEPARATOR, AgentConstants.AGENT_PARAMS_SEPARATOR);
	}

	private void setUpParams() {
		super.addAgentParam(AgentParams.OPT_SHARED_DETECTION, true);
		super.addAgentParam(AgentParams.OPT_DUMP_FILE, STORAGE_FILE);
	}
	
	@Override
	public void run() {
		try {
			super.startAndWaitUntilStop(getProgramArgs());
		} catch (SavException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
