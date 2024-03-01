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
	
	public static void _onStaticRead(String className, String fieldName) {
		AggrePlaySharedVariableAgent._onStaticAccess(className, fieldName);
	}
	
	public static void _onStaticWrite(String className, String fieldName) {
		AggrePlaySharedVariableAgent._onStaticAccess(className, fieldName);
	}
	
	public static void _onArrayRead(Object arrayRef, int index) {
		AggrePlaySharedVariableAgent._onArrayAccess(arrayRef, index);
	}
	
	public static void _onArrayWrite(Object arrayRef, int index) {
		AggrePlaySharedVariableAgent._onArrayAccess(arrayRef, index);
	}
	
	public static void _onNewArray(Object arrayRef) {
		AggrePlaySharedVariableAgent._onNewArray(arrayRef);
	}
	
	public static void _onLockAcquire(Object object) {
		AggrePlaySharedVariableAgent._onLockAcquire(object);
	}
}
