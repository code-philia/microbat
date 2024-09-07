package microbat.instrumentation.dataflowrecovery;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import microbat.instrumentation.AgentParams;
import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.instr.TraceTransformer;

/**
 * @author hongshuwang
 */
public class DependencyRecoveryTransformer extends TraceTransformer {

	public DependencyRecoveryTransformer(AgentParams agentParams) {
		super.instrumenter = new DependencyRecoveryInstrumenter(agentParams);
	}

}
