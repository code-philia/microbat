package microbat.instrumentation.instr;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.instr.aggreplay.ThreadIdInstrumenter;

public class SystemClassTransformer {
	
	public static void transformClassLoader(Instrumentation inst) {
		transform(inst, ClassLoader.class, new ClassLoaderInstrumenter());
	}
	
	public static void transformThread(Instrumentation inst) {
		transform(inst, Thread.class, new ThreadInstrumenter());
	}
	
	/**
	 * Used to generate persistent thread id during runtime.
	 * Needed so that thread id is consistent across runs.
	 * @param inst
	 */
	public static void attachThreadId(Instrumentation inst) {
		transform(inst, Thread.class, new ThreadIdInstrumenter());
	}
	
	public static void attachThreadId(Instrumentation inst, Class<?> clazz) {
		transform(inst, Thread.class, new ThreadIdInstrumenter(clazz));
	}
	
	public static void transform(Instrumentation inst, final Class<?> clazz, final AbstractInstrumenter instrumenter) {
		final String clazzFName = clazz.getName().replace(".", "/");
		ClassFileTransformer transformer = new AbstractTransformer() {
			
			@Override
			protected byte[] doTransform(ClassLoader loader, String classFName, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				if (clazzFName.equals(classFName)) {
					try {
						byte[] data = instrumenter.instrument(classFName, classfileBuffer);
						return data;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		};
		inst.addTransformer(transformer, true);
		try {
			inst.retransformClasses(clazz);
		} catch (UnmodifiableClassException e) {
			AgentLogger.info(String.format("Cannot instrument class %s!", clazzFName));
		}
		inst.removeTransformer(transformer);
	}
}
