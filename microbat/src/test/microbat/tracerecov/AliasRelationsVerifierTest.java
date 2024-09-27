package microbat.tracerecov;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import microbat.codeanalysis.bytecode.CFG;
import microbat.tracerecov.staticverifiers.AliasRelationsVerifier;
import microbat.tracerecov.staticverifiers.AssignRelation;
import microbat.tracerecov.staticverifiers.ReturnRelation;

public class AliasRelationsVerifierTest {

	/**
	 * Input `index` is guaranteed not to be assigned to any internal field. Always
	 * bypass elements in array for both assign and return status. Thus return
	 * status is no guarantee.
	 */
	@Test
	public void elementInArray_GuaranteeNoAssign_NoGuaranteeReturn() {
		String methodSignature = "java.util.ArrayList#get(I)Ljava/lang/Object;";
		String className = "java.util.ArrayList";
		String paramName = "index";
		int paramIndex = 1;

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
			AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(paramName, paramIndex,
					className);
			ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

			assertEquals(AssignRelation.getGuaranteeNoAssignRelation(), assignRelation);
			assertEquals(ReturnRelation.getNoGuaranteeReturnRelation(), returnRelation);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Nothing will be assigned since there is no input parameters. `buf` is
	 * guaranteed to be returned.
	 */
	@Test
	public void noMethodInvocation_GuaranteeNoAssign_GuaranteeReturn() {
		String methodSignature = "java.io.StringWriter#getBuffer()Ljava/lang/StringBuffer;";
		String className = "java.io.StringWriter";
		String paramName = null;
		int paramIndex = -1;
		String returnedField = "buf";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
			AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(paramName, paramIndex,
					className);
			ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

			assertEquals(AssignRelation.getGuaranteeNoAssignRelation(), assignRelation);
			assertEquals(ReturnRelation.getGuaranteeReturnRelation(returnedField), returnRelation);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parameter `out` is guaranteed to be assigned to an internal field `out`. A
	 * PrintWriter instance is guaranteed to be returned.
	 */
	@Test
	public void constructor_GuaranteeAssign_GuaranteeReturn() {
		String methodSignature = "java.io.PrintWriter#<init>(Ljava/io/Writer;)V";
		String className = "java.io.PrintWriter";
		String paramName = "out";
		int paramIndex = 1;
		String fieldName = "out";
		String returnedField = "PrintWriter_instance";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
			AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(paramName, paramIndex,
					className);
			ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

			assertEquals(AssignRelation.getGuaranteeAssignRelation(paramName, fieldName), assignRelation);
			assertEquals(ReturnRelation.getGuaranteeReturnRelation(returnedField), returnRelation);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}
	
	// Verify alias relationships for parameterless constructors.
	@Test
	public void constructorNoParam_GuaranteeReturn() {
	    String methodSignature = "java.util.ArrayList#<init>()V";
	    String className = "java.util.ArrayList";
	    
	    try {
	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
	        ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

	        String returnedField = "ArrayList_instance";
	        assertEquals(ReturnRelation.getGuaranteeReturnRelation(returnedField), returnRelation);
	    } catch (CannotBuildCFGException e) {
	        e.printStackTrace();
	    }
	}
	
	/*
	 * Test assignment and return relationships for generic collections  
	 * HashMap put should guarantee key assignment but return status is not guaranteed
	 */	
	@Test
	public void hashMapPut_GuaranteeAssign_NoGuaranteeReturn() {
	    String methodSignature = "java.util.HashMap#put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
	    String className = "java.util.HashMap";
	    String paramName = "key";
	    int paramIndex = 1;

	    try {
	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
	        AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(paramName, paramIndex, className);
	        ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

	        assertEquals(AssignRelation.getGuaranteeAssignRelation(paramName, "table"), assignRelation);
	        assertEquals(ReturnRelation.getNoGuaranteeReturnRelation(), returnRelation);
	    } catch (CannotBuildCFGException e) {
	        e.printStackTrace();
	    }
	}

	/*
	 * Test the assignment relationship for the elements in the array 
	 * ArrayList#set should guarantee assignment to the array but return status is
	 * not guaranteed
	 */	
	@Test
	public void elementInArray_Assign_NoGuaranteeReturn() {
	    String methodSignature = "java.util.ArrayList#set(ILjava/lang/Object;)Ljava/lang/Object;";
	    String className = "java.util.ArrayList";
	    String paramName = "index";
	    int paramIndex = 1;

	    try {
	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
	        AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(paramName, paramIndex, className);
	        ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

	        assertEquals(AssignRelation.getGuaranteeAssignRelation(paramName, "elementData"), assignRelation);
	        assertEquals(ReturnRelation.getNoGuaranteeReturnRelation(), returnRelation);
	    } catch (CannotBuildCFGException e) {
	        e.printStackTrace();
	    }
	}
	
	/*
	 *  Test the alias relationship of a method with no input parameters but with
	 * a return value 
	 *  The method guarantees to return the result of the toString call
	 */	
	@Test
	public void noParamMethod_GuaranteeReturn() {
	    String methodSignature = "java.lang.StringBuilder#toString()Ljava/lang/String;";
	    String className = "java.lang.StringBuilder";

	    try {
	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
	        ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

	        String returnedField = "toString_result";
	        assertEquals(ReturnRelation.getGuaranteeReturnRelation(returnedField), returnRelation);
	    } catch (CannotBuildCFGException e) {
	        e.printStackTrace();
	    }
	}
	
	/*
	 * Testing complex methods with multiple parameters and return values HashMap
	 * computeIfAbsent should guarantee key assignment and return the computed value
	 */
	@Test
	public void complexMethod_GuaranteeAssign_GuaranteeReturn() {
	    String methodSignature = "java.util.HashMap#computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;";
	    String className = "java.util.HashMap";
	    String paramName = "key";
	    int paramIndex = 1;

	    try {
	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
	        AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(paramName, paramIndex, className);
	        ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

	        assertEquals(AssignRelation.getGuaranteeAssignRelation(paramName, "table"), assignRelation);
	        String returnedField = "computed_value";
	        assertEquals(ReturnRelation.getGuaranteeReturnRelation(returnedField), returnRelation);
	    } catch (CannotBuildCFGException e) {
	        e.printStackTrace();
	    }
	}
	
	/*
	 * Validates constructors with parameters. StringBuilder constructor should
	 * assign capacity to internal field and return instance
	 */
	@Test
	public void constructorWithParam_GuaranteeAssign_GuaranteeReturn() {
	    String methodSignature = "java.lang.StringBuilder#<init>(I)V";
	    String className = "java.lang.StringBuilder";
	    String paramName = "capacity";
	    int paramIndex = 1;
	    String fieldName = "value";

	    try {
	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
	        AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(paramName, paramIndex, className);
	        ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

	        assertEquals(AssignRelation.getGuaranteeAssignRelation(paramName, fieldName), assignRelation);
	        String returnedField = "StringBuilder_instance";
	        assertEquals(ReturnRelation.getGuaranteeReturnRelation(returnedField), returnRelation);
	    } catch (CannotBuildCFGException e) {
	        e.printStackTrace();
	    }
	}

	/*
	 * Tests for invalid values ​​for varName and paramIndex Invalid parameters
	 * should result in no guarantee for assign and return
	 */
	@Test
	public void invalidParam_NoAssign_NoReturn() {
	    String methodSignature = "java.util.ArrayList#get(I)Ljava/lang/Object;";
	    String className = "java.util.ArrayList";
	    String paramName = null; // Invalid parameter
	    int paramIndex = -1; // Invalid index

	    try {
	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
	        AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(paramName, paramIndex, className);
	        ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

	        assertEquals(AssignRelation.getGuaranteeNoAssignRelation(), assignRelation);
	        assertEquals(ReturnRelation.getNoGuaranteeReturnRelation(), returnRelation);
	    } catch (CannotBuildCFGException e) {
	        e.printStackTrace();
	    }
	}

}
