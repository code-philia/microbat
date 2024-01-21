package microbat.instrumentation.aggreplay;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.util.logging.Handler;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.util.ByteSequence;
import org.junit.Test;
import org.junit.internal.runners.TestClass;
import org.junit.validator.PublicClassValidator;

import javassist.bytecode.Opcode;
import microbat.instrumentation.instr.aggreplay.ObjectAccessInstrumentator;


/**
 * Utility Class for testing a generic instrumentator
 * @author Gabau
 *
 */
public class ObjectAccessInstrumentatorTest {
	private static class InjectedObjectAccessInstrumentor extends ObjectAccessInstrumentator {
		private static int counter = 0;
		public InjectedObjectAccessInstrumentor() {
			super(InjectedObjectAccessInstrumentor.class);
			// TODO Auto-generated constructor stub
		}
		public static void _onNewObject(Object object) {
			counter++;
		}
		
		public static void _onObjectWrite(Object object, String field) {
			counter++;
		}
		
		public static void _onObjectRead(Object object, String field) {
			counter++;
		}
	}
	
	private static class TestClass {
		int value = 0;
		public static void main(String[] args) {
			TestClass tClass = new TestClass();
			TestClass tClass2 = new TestClass();
			tClass.value = 1000;
			tClass.value = 100;
		}
	}
	private ClassGen getClassGen(byte[] classData, String classFName) throws ClassFormatException, IOException {
		// verify that
		ClassParser cParser = new ClassParser(new ByteArrayInputStream(classData), classFName);
		JavaClass javaClass = cParser.parse();
		ClassGen cGen = new ClassGen(javaClass);
		return cGen;
	}
	
	public void testInstrumentation(Class<?> testClass) throws Exception {
		InjectedObjectAccessInstrumentor toTestInstrumentor 
			= new InjectedObjectAccessInstrumentor();
		JavaClass jClass = null;
		try {
			jClass = Repository.lookupClass(testClass);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] result =  null;
		result = toTestInstrumentor.instrument(jClass.getClassName(), jClass.getBytes());
		assert(result != null);
		
		// verify that
		ClassGen cGen = getClassGen(result, jClass.getClassName());
		for (Method method : cGen.getMethods()) {
			Code code = method.getCode();
			InstructionList iList = new InstructionList(code.getCode());
			for (InstructionHandle iHandle : iList) {
				// assert that dup is called after + invoke
				if (iHandle.getInstruction().getOpcode() == Opcode.NEW) {
					InstructionHandle nextInstructionHandle = iHandle.getNext();
					assertNotNull(nextInstructionHandle);
					assertTrue("Should dup new object", 
							nextInstructionHandle.getInstruction().getOpcode() == Opcode.DUP);
					InstructionHandle nextNextInstructionHandle = nextInstructionHandle.getNext();
					assertTrue("Should invoke after dup", 
							nextNextInstructionHandle.getInstruction().getOpcode() == Opcode.INVOKESTATIC);
					// should invoke method "_onNewObject
					INVOKESTATIC invokestatic = (INVOKESTATIC) nextNextInstructionHandle.getInstruction();
					assertEquals(invokestatic.getClassName(cGen.getConstantPool()), 
							InjectedObjectAccessInstrumentor.class.getName());
					continue;
				}

			}
		}
		
		
		
		
		
	}
	
	@Test
	public void testTestClass() throws Exception {
		testInstrumentation(TestClass.class);
		TestClass.main(null);
	}
	
}
