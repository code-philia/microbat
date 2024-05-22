package microbat.tracerecov.varmapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.ClassReader;

import microbat.model.value.VarValue;

/**
 * This class performs static analysis and maps candidate variables to recorded
 * variables.
 * 
 * @author hongshuwang
 */
public class VariableMapper {

	/**
	 * Given invokingMethod, get the return type of the candidate variable.
	 */
	public static String getReturnTypeOfCandidateVariable(String invokingMethod) {
		if (invokingMethod == null || invokingMethod.startsWith("%")) {
			return null;
		}

		String className = invokingMethod.split("#")[0];
		String methodSignature = invokingMethod.split("#")[1].split("%")[0];
		int index = methodSignature.indexOf("(");
		String methodName = methodSignature.substring(0, index);
		String methodDescriptor = methodSignature.substring(index);

		return getReturnTypeOfCandidateVariable(className, methodName, methodDescriptor, methodSignature);
	}

	/**
	 * Create a copy of variableOnTrace in parentVariable with value == null.
	 */
	public static VarValue mapVariable(VarValue variableOnTrace, VarValue parentVariable) {
		if (variableOnTrace == null || parentVariable == null) {
			return null;
		}
		
		List<VarValue> children = parentVariable.getChildren();
		for (VarValue c : children) {
			if (c.getAliasVarID().equals(variableOnTrace.getAliasVarID())) {
				return null;
			}
		}

		VarValue child = variableOnTrace.clone();
		child.setStringValue(null);
		child.setParents(Arrays.asList(parentVariable));
		child.setRoot(false);
		child.setVarID(parentVariable.getVarID() + "-" + child.getVarName());

		parentVariable.addChild(child);
		
		return child;
	}

	private static String getReturnTypeOfCandidateVariable(String className, String methodName, String methodDescriptor,
			String methodSignature) {
		// load the class
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");

		try {
			ClassReader classReader = new ClassReader(inputStream);

			// create and accept a classVisitor
			VarMappingClassVisitor classVisitor = new VarMappingClassVisitor(className, methodName, methodDescriptor);
			classReader.accept(classVisitor, 0);

			String typeDescriptor = VarMappingMethodVisitor.getReturnedFieldType();
			return convertDescriptorToType(typeDescriptor);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static String convertDescriptorToType(String descriptor) {
		if (descriptor == null) {
			return null;
		}

		if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
			String typeName = descriptor.substring(1, descriptor.length() - 1);
			return typeName.replace('/', '.');
		}

		throw new IllegalArgumentException("Invalid type descriptor: " + descriptor);
	}

}
