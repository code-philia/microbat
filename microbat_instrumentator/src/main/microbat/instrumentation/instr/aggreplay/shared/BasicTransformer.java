package microbat.instrumentation.instr.aggreplay.shared;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.instr.AbstractInstrumenter;

public class BasicTransformer implements ClassFileTransformer {
	private AbstractInstrumenter instrumenter = new SharedObjectAccessInstrumentator();
	
	public BasicTransformer() {
		
	}
	
	public BasicTransformer(AbstractInstrumenter inst) {
		this.instrumenter = inst;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		// TODO Auto-generated method stub
		/* bootstrap classes */

		if ((loader == null) || (protectionDomain == null)) {
			if (!GlobalFilterChecker.isTransformable(className, null, true)) {
				return null;
			}
		} 

		if (protectionDomain != null) {
			String path = protectionDomain.getCodeSource().getLocation().getFile();
			if (!GlobalFilterChecker.isTransformable(className, path, false)) {
				return null;
			}
		}

		/* do instrumentation */
		try {
			byte[] result = instrumenter.instrument(className, classfileBuffer);

			if (className.equals("Test$DumbThread")) {
				File tocreate = new File("Output.class");
				try {
					FileOutputStream fw = new FileOutputStream(tocreate);
					fw.write(result);
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	
	}
	
	
}
