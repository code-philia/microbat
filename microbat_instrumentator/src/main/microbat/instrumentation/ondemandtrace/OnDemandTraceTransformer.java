package microbat.instrumentation.ondemandtrace;

import microbat.instrumentation.AgentParams;
import microbat.instrumentation.instr.TraceTransformer;

/**
 * This class is the transformer for on-demand trace loading.
 * 
 * @author HongshuW
 */
public class OnDemandTraceTransformer extends TraceTransformer {

	public OnDemandTraceTransformer(AgentParams params) {
		instrumenter = new OnDemandTraceInstrumenter(params);
	}

}
