package microbat.instrumentation.dataflowrecovery;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import microbat.instrumentation.AgentParams;
import microbat.instrumentation.instr.AbstractTransformer;
import microbat.instrumentation.instr.TraceTransformer;

/**
 * @author hongshuwang
 */
public class DependencyRecoveryTransformer extends TraceTransformer {

	public DependencyRecoveryTransformer(AgentParams agentParams) {
		super(agentParams);
	}

	@Override
	protected byte[] doTransform(ClassLoader loader, String classFName, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		return null;
	}

}
