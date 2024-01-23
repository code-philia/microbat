package microbat.instrumentation.instr.aggreplay.shared;

import microbat.instrumentation.instr.aggreplay.ObjectAccessInstrumentator;
import microbat.instrumentation.instr.aggreplay.agents.AggrePlaySharedVariableAgent;

public class SharedObjectAccessInstrumentator extends ObjectAccessInstrumentator {

	public SharedObjectAccessInstrumentator() {
		super(SharedObjectAccessInstrumentator.class);
	}
	
	public static void _onNewObject(Object object) {
		AggrePlaySharedVariableAgent._onObjectCreation(object);
	}
	
	public static void _onObjectWrite(Object object, String field) {
		AggrePlaySharedVariableAgent._onObjectAccess(object, field);
	}
	
	public static void _onObjectRead(Object object, String field) {
		AggrePlaySharedVariableAgent._onObjectAccess(object, field);
	}
}
