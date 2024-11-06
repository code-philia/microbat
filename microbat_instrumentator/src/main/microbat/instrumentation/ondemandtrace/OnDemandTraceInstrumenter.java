package microbat.instrumentation.ondemandtrace;

import microbat.instrumentation.AgentParams;
import microbat.instrumentation.instr.TraceInstrumenter;

/**
 * This class is responsible for instrumenting loaded java classes for on-demand
 * trace loading.
 * 
 * @author HongshuW
 */
public class OnDemandTraceInstrumenter extends TraceInstrumenter {

	public OnDemandTraceInstrumenter(AgentParams params) {
		super(params);
	}

}
