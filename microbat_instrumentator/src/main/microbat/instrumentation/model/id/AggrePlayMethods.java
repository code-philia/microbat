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
	ON_LOCK_ACQUIRE("_onLockAcquire", "(Ljava/lang/Object;)V"),
	ON_LOCK_ACQUIRE2("_onLockAcquire2", "()V"),
	RELEASE_LOCK("_releaseLock", "()V"),
	START("_start", "()V"),
	/**
	 * After NEW instruction, before object initialisation
	 */
	ON_NEW_OBJECT("_onNewObject", "(Ljava/lang/Object;)V"),
	BEFORE_OBJECT_READ("_onObjectRead", "(Ljava/lang/Object;Ljava/lang/String;)V"),
	AFTER_OBJECT_READ("_afterObjectRead", "()V"),
	AFTER_OBJECT_WRITE("_afterObjectWrite", "()V"),
	BEFORE_OBJECT_WRITE("_onObjectWrite", "(Ljava/lang/Object;Ljava/lang/String;)V"),
	BEFORE_ARRAY_WRITE("_onArrayWrite", "(Ljava/lang/Object;I)V"),
	BEFORE_ARRAY_READ("_onArrayRead", "(Ljava/lang/Object;I)V"),
	ON_NEW_ARRAY("_onNewArray", "(Ljava/lang/Object;)V"),
	AFTER_LOCK_ACQUIRE("_afterLockAcquire", "()V"),
	BEFORE_STATIC_READ("_onStaticRead", "(Ljava/lang/String;Ljava/lang/String;)V"),
	BEFORE_STATIC_WRITE("_onStaticWrite", "(Ljava/lang/String;Ljava/lang/String;)V"),
	ASSERT_OBJECT_EXISTS("_assertObjectExists", "(Ljava/lang/Object;)V"),
	ASSERT_ARRAY_EXISTS("_assertArrayExists", "(Ljava/lang/Object;)V");
	
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
