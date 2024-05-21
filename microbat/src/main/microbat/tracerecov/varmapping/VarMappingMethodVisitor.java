package microbat.tracerecov.varmapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
	private List<String> candidateVariables;

	public VarMappingMethodVisitor(List<String> candidateVariables) {
		super(Opcodes.ASM9, null);
		this.candidateVariables = candidateVariables;
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		if (GET_INSTRUCTIONS.contains(opcode) && candidateVariables.contains(name)) {
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

	public static String getReturnedFieldType() {
		return fieldType;
	}
}
