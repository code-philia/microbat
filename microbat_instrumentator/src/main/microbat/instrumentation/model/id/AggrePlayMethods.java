package microbat.instrumentation.model.id;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;

/**
 * Events that are used in this instrumentation
 * @author Gabau
 *
 */
public enum AggrePlayMethods {
	ACQUIRE_LOCK("_acquireLock", "()V"),
	ON_LOCK_ACQUIRE("_onLock_Acquire", "(Ljava/lang/Object)V"),
	RELEASE_LOCK("_releaseLock", "()V"),
	ON_NEW_OBJECT("_onNewObject", "(Ljava/lang/Object;)V"),
	BEFORE_OBJECT_READ("_onObjectRead", "(Ljava/lang/Object;Ljava/lang/String;)V"),
	AFTER_OBJECT_READ("_afterObjectRead", "()V"),
	AFTER_OBJECT_WRITE("_afterObjectWrite", "()V"),
	BEFORE_OBJECT_WRITE("_onObjectWrite", "(Ljava/lang/Object;Ljava/lang/String;)V");
	
	
	public final String methodName;
	public final String methodSig;
	
	private AggrePlayMethods(String methodName, String methodSig) {
		this.methodName = methodName;
		this.methodSig = methodSig;
	}
	
	public static INVOKESTATIC createInvokeStatic(ConstantPoolGen cpg, Class<?> clazz, AggrePlayMethods method) {
		return createInvokeStatic(cpg, clazz, method.methodName, method.methodSig);
	}
	
	public static INVOKESTATIC createInvokeStatic(ConstantPoolGen cpg, Class<?> clazz, String methodName, String signature) {
		return new INVOKESTATIC(cpg.addMethodref(clazz.getName().replace(".", "/"), methodName, signature));
	}
	
	
	public INVOKESTATIC toInvokeStatic(ConstantPoolGen cpg, Class<?> clazz) {
		return createInvokeStatic(cpg, clazz, this);
	}
}
