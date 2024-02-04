package microbat.instrumentation.dataflowrecovery;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import microbat.instrumentation.instr.AbstractTransformer;

/**
 * @author hongshuwang
 */
public class DependencyRecoveryTransformer extends AbstractTransformer implements ClassFileTransformer {

	public DependencyRecoveryTransformer() {
	}

	@Override
	protected byte[] doTransform(ClassLoader loader, String classFName, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		return null;
	}

}
