package microbat.instrumentation.dataflowrecovery;

import microbat.instrumentation.AgentParams;
import microbat.instrumentation.instr.TraceInstrumenter;

/**
 * @author hongshuwang
 */
public class DependencyRecoveryInstrumenter extends TraceInstrumenter {

	public DependencyRecoveryInstrumenter(AgentParams params) {
		super(params);
	}

}
