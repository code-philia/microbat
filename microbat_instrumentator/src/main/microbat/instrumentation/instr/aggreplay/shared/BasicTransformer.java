package microbat.instrumentation.instr.aggreplay.shared;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.time.DayOfWeek;

import microbat.instrumentation.filter.GlobalFilterChecker;
import microbat.instrumentation.instr.AbstractInstrumenter;
import microbat.instrumentation.instr.AbstractTransformer;

public class BasicTransformer extends AbstractTransformer implements ClassFileTransformer {
	private AbstractInstrumenter instrumenter;
	
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

			if (classFName.equals("simplebug1/simplebug/TestObject")) {
				File otherFile = new File("K:\\OldOutput.class");
				File tocreate = new File("K:\\Output.class");
				try {
					FileOutputStream fileOutputStream = new FileOutputStream(otherFile);
					fileOutputStream.write(classfileBuffer);
					
					FileOutputStream fw = new FileOutputStream(tocreate);
					fw.write(result);
					fw.flush();
					fileOutputStream.flush();
					fileOutputStream.close();
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
