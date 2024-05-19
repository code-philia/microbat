package microbat.tracerecov;

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
	
	private int lastInstruction;

	public VarMappingMethodVisitor() {
		super(Opcodes.ASM6, null);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		if (GET_INSTRUCTIONS.contains(opcode)) {
			fieldName = name;
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
	}

	public static String getReturnedField() {
		return fieldName;
	}
}
