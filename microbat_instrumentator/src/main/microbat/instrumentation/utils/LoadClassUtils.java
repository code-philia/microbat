package microbat.instrumentation.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.commons.io.IOUtils;

public class LoadClassUtils {

	public LoadClassUtils() {}
	
	public static Map<String, Set<String>> getRelevantMethods(String className, String methodSignature) throws IOException {
		/*
		 * load the class
		 */
		ClassGen classGen = loadClass(className);
		if (classGen == null) {
			return new HashMap<>();
		}
		
		/* 
		 * get all relevant methods
		 */
		PriorityQueue<String> queue = new PriorityQueue<>();
		
		for (Method method : classGen.getJavaClass().getMethods()) {
			String methodSig = MicrobatUtils.getMicrobatMethodFullName(className, method);
			if (methodSignature.equals(methodSig)) {
				queue.add(methodSig);
				break;
			}
		}
		
		return getReleventMethodsBFS(queue);
	}
	
	private static String getFileName(String classFName) {
		return classFName + ".class";
	}
	
	private static ClassGen loadClass(String className) throws IOException {
		String classFName = className.replace('.', '/');
		String fileName = getFileName(classFName);
		byte[] bytecode = loadByteCode(fileName);
		
		if (bytecode == null) {
			return null;
		}
		
		ClassParser cp = new ClassParser(new java.io.ByteArrayInputStream(bytecode), classFName);
		JavaClass jc = cp.parse();
		return new ClassGen(jc);
	}
	
	private static byte[] loadByteCode(String fileName) throws IOException {
		InputStream inputStream = ClassLoader
        		.getSystemClassLoader()
        		.getResourceAsStream(fileName);
		if (inputStream != null) {
			byte[] bytecode = IOUtils.toByteArray(inputStream);
            inputStream.close();
            return bytecode;
        }
		return null;
	}

	private static Map<String, Set<String>> getReleventMethodsBFS(PriorityQueue<String> queue) throws IOException {
		Map<String, Set<String>> relevantMethods = new HashMap<>();
		
		while (!queue.isEmpty()) {
			String methodFullName = queue.remove();
			String clsName = methodFullName.split("#")[0];
			if (!relevantMethods.containsKey(clsName)) {
				relevantMethods.put(clsName, new HashSet<String>());
			}
			
			if (relevantMethods.get(clsName).contains(methodFullName)) {
				continue; // expanded
			}
			
			ClassGen clsGen = loadClass(clsName);
			if (clsGen == null) {
				continue;
			}
			ConstantPoolGen constPoolGen = clsGen.getConstantPool();
			for (Method method : clsGen.getJavaClass().getMethods()) {
				if (!MicrobatUtils.getMicrobatMethodFullName(clsName, method).equals(methodFullName)) {
					continue;
				}
				
				if (relevantMethods.get(clsName).contains(methodFullName)) {
					continue;
				} else {
					// mark as expanded
					relevantMethods.get(clsName).add(methodFullName);
				}
				
				if (method.isNative() || method.isAbstract() || method.getCode() == null) {
					continue; // Only instrument methods with code in them!
				}
				
				String classFName = clsName.replace('.', '/');
				MethodGen methodGen = new MethodGen(method, classFName, constPoolGen);
				InstructionList insnList = methodGen.getInstructionList();
				for (Instruction i : insnList.getInstructions()) {
					if (i instanceof InvokeInstruction) {
						InvokeInstruction instruction = (InvokeInstruction) i;
						String invokedClassName = instruction.getClassName(constPoolGen);
						String invokedMethod = MicrobatUtils.getMicrobatMethodFullName(
								invokedClassName, 
								instruction.getMethodName(constPoolGen), 
								instruction.getSignature(constPoolGen));
						
						if (!relevantMethods.containsKey(invokedClassName)) {
							relevantMethods.put(invokedClassName, new HashSet<String>());
						}
						if (!relevantMethods.get(invokedClassName).contains(invokedMethod)) {
							queue.add(invokedMethod);
						}
					}
				}
				
			}
		}
		
		return relevantMethods;
	}
}
