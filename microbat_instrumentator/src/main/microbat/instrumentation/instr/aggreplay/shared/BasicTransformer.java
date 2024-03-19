package microbat.instrumentation.instr.aggreplay.shared;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.instr.AbstractInstrumenter;
import microbat.instrumentation.instr.AbstractTransformer;

public class BasicTransformer extends AbstractTransformer implements ClassFileTransformer {
	private AbstractInstrumenter instrumenter = new SharedObjectAccessInstrumentator();
	
	public BasicTransformer() {
		
	}
	
	public BasicTransformer(AbstractInstrumenter inst) {
		this.instrumenter = inst;
	}

	@Override
	public byte[] doTransform(ClassLoader loader, String classFName, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException  {
		// TODO Auto-generated method stub
		/* bootstrap classes */

		if ((loader == null) || (protectionDomain == null)) {
			if (!GlobalFilterChecker.isTransformable(classFName, null, true)) {
				return null;
			}
		} 

		if (protectionDomain != null) {
			String path = protectionDomain.getCodeSource().getLocation().getFile();
			if (!GlobalFilterChecker.isTransformable(classFName, path, false)) {
				return null;
			}
		}

		/* do instrumentation */
		try {
			byte[] result = instrumenter.instrument(classFName, classfileBuffer);

//			if (classFName.equals("benchmark/account/Main")) {
//				File tocreate = new File("K:\\OutputMain.class");
//				try {
//					FileOutputStream fw = new FileOutputStream(tocreate);
//					fw.write(result);
//					fw.close();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
			
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	
	}
	
	
}
