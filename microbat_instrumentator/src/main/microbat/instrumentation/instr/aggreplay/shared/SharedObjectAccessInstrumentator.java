package microbat.instrumentation.instr.aggreplay.shared;

import microbat.instrumentation.AgentParams;
import microbat.instrumentation.instr.aggreplay.ObjectAccessInstrumentator;
import microbat.instrumentation.instr.aggreplay.agents.SharedVariableAgent;

public class SharedObjectAccessInstrumentator extends ObjectAccessInstrumentator {

	public SharedObjectAccessInstrumentator(AgentParams agentParams) {
		super(SharedObjectAccessInstrumentator.class, agentParams);
	}
	
	public static void _onNewObject(Object object) {
		SharedVariableAgent._onObjectCreation(object);
	}
	
	public static void _onObjectWrite(Object object, String field) {
		SharedVariableAgent._onObjectAccess(object, field);
	}
	
	public static void _onObjectRead(Object object, String field) {
		SharedVariableAgent._onObjectAccess(object, field);
	}
	
	public static void _onStaticRead(String className, String fieldName) {
		SharedVariableAgent._onStaticAccess(className, fieldName);
	}
	
	public static void _onStaticWrite(String className, String fieldName) {
		SharedVariableAgent._onStaticAccess(className, fieldName);
	}
	
	public static void _onArrayRead(Object arrayRef, int index) {
		SharedVariableAgent._onArrayAccess(arrayRef, index);
	}
	
	public static void _assertObjectExists(Object object) {
		SharedVariableAgent._assertObjectExists(object);
	}
	
	public static void _assertArrayExists(Object object) {
		SharedVariableAgent._assertArrayExists(object);
	}
	
	public static void _onArrayWrite(Object arrayRef, int index) {
		SharedVariableAgent._onArrayAccess(arrayRef, index);
	}
	
	public static void _onNewArray(Object arrayRef) {
		SharedVariableAgent._onNewArray(arrayRef);
	}
	
	public static void _onLockAcquire(Object object) {
		SharedVariableAgent._onLockAcquire(object);
	}
}
