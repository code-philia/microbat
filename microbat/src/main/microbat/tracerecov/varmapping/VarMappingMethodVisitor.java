package microbat.tracerecov.varmapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class VarMappingMethodVisitor extends MethodVisitor {

	private final static Set<Integer> GET_INSTRUCTIONS = new HashSet<>(
			Arrays.asList(Opcodes.GETFIELD, Opcodes.GETSTATIC));
	/* Last field read by the current method. */
	private static String fieldName;
	private static String fieldType;

	private int lastInstruction;

	public VarMappingMethodVisitor() {
		super(Opcodes.ASM9, null);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		if (GET_INSTRUCTIONS.contains(opcode)) {
			fieldName = name;
			fieldType = descriptor;
		}
		lastInstruction = opcode;
		super.visitFieldInsn(opcode, owner, name, descriptor);
	}

	@Override
	public void visitInsn(int opcode) {
		if (fieldName != null && opcode == Opcodes.ARETURN) {
			if (!GET_INSTRUCTIONS.contains(lastInstruction)) {
				// last instruction before return is not getfield or getstatic
				fieldName = null;
				fieldType = null;
			}
		}
		lastInstruction = opcode;
		super.visitInsn(opcode);
	}

	/**
	 * Reset last visited field.
	 */
	public static void reset() {
		fieldName = null;
		fieldType = null;
	}

	public static String getReturnedField() {
		return fieldName + "#" + convertDescriptorToType(fieldType);
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
