package microbat.mutation.mutation;

import java.util.Arrays;
import java.util.List;

public enum MutationType {
	REMOVE_IF_BLOCK ("Remove If Block"),
	REMOVE_ASSIGNMENT ("Remove Assignment"),
	REMOVE_IF_CONDITION ("Remove If Condition"),
	REMOVE_IF_RETURN ("Remove If Return"),
	NEGATE_IF_CONDITION ("Negate If Condition"),
	CHANGE_ARITHMETIC_OPERATOR ("Change Arithmetic Operator"),
	CHANGE_CONDITIONALS_BOUNDARY ("Change Conditionals Boundary"),
	SWAP_OPERANDS ("Swap Operands"),
	CHANGE_RETURN ("Change Return Value"),
	CHANGE_LITERAL ("Change Certain Literal");
	
	private String text;
	private MutationType(String text) {
		this.text = text;
	}
	
	public String getText() {
		return text;
	}
	
	public static List<MutationType> getPreferenceMutationTypes() {
		return Arrays.asList(
				REMOVE_ASSIGNMENT, 
				REMOVE_IF_BLOCK, 
				REMOVE_IF_CONDITION, 
				NEGATE_IF_CONDITION,
				CHANGE_ARITHMETIC_OPERATOR,
				CHANGE_CONDITIONALS_BOUNDARY,
				SWAP_OPERANDS,
				CHANGE_RETURN,
				CHANGE_LITERAL
				);
	}
}
