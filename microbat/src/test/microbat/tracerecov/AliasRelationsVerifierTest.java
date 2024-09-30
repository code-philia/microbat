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

	/**
	 * Constructor is guaranteed to return a new instance.
	 */
	@Test
	public void constructorNoParam_GuaranteeReturn() {
		String methodSignature = "java.util.ArrayList#<init>()V";
		String returnedField = "ArrayList_instance";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
			ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

			assertEquals(ReturnRelation.getGuaranteeReturnRelation(returnedField), returnRelation);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The assignment of `key` occurs in another class, thus it's guaranteed not to
	 * be assigned to any field within HashMap class. The return status is not
	 * guaranteed because there are multiple return branches with different
	 * behaviors.
	 */
	@Test
	public void hashMapPut_GuaranteeNoAssign_NoGuaranteeReturn() {
		String methodSignature = "java.util.HashMap#put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
		String className = "java.util.HashMap";
		String paramName = "key";
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
	 * `index` is not assigned to any field. The returned variable is an element in
	 * the internal array, since its index cannot be determined, the return status
	 * cannot be guaranteed.
	 */
	@Test
	public void elementInArray_GuaranteeNoAssign_NoGuaranteeReturn2() {
		String methodSignature = "java.util.ArrayList#set(ILjava/lang/Object;)Ljava/lang/Object;";
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
	 * This test verifies that both `key` and `value` parameters of HashMap#put 
	 * are not assigned to any internal field. This test ensures the static analysis tool 
	 * correctly identifies that no assign relation is formed between these parameters and internal fields.
	 */
	@Test
	public void multipleParams_AssignRelationTest() {
	    String methodSignature = "java.util.HashMap#put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
	    String className = "java.util.HashMap";
	    String paramNameKey = "key";
	    int paramIndexKey = 1;
	    String paramNameValue = "value";
	    int paramIndexValue = 2;

	    try {
	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);

	        // Check if 'key' is assigned to an internal field
	        AssignRelation assignRelationKey = aliasRelationsVerifier.getVarAssignRelation(paramNameKey, paramIndexKey, className);
	        // Check if 'value' is assigned to an internal field
	        AssignRelation assignRelationValue = aliasRelationsVerifier.getVarAssignRelation(paramNameValue, paramIndexValue, className);

	        // Neither 'key' nor 'value' should be assigned to internal fields
	        assertEquals(AssignRelation.getGuaranteeNoAssignRelation(), assignRelationKey);
	        assertEquals(AssignRelation.getGuaranteeNoAssignRelation(), assignRelationValue);

	    } catch (CannotBuildCFGException e) {
	        e.printStackTrace();
	    }
	}

	
//	/**
//	 * This test verifies that StringBuilder#toString can return its internal field 'toStringCache'.
//	 * It checks whether the return relation is correctly formed between the method and the internal field.
//	 */
//	@Test
//	public void complexControlFlow_ReturnRelationTest() {
//	    String methodSignature = "java.lang.StringBuilder#toString()Ljava/lang/String;";
//	    String className = "java.lang.StringBuilder";
//
//	    try {
//	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
//	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);
//
//	        // Check the return relation
//	        ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();
//
//	        // Ensure that the returned field is 'toStringCache'
//	        String expectedReturnedField = "toStringCache";
//	        assertEquals(ReturnRelation.getGuaranteeReturnRelation(expectedReturnedField), returnRelation);
//
//	    } catch (CannotBuildCFGException e) {
//	        e.printStackTrace();
//	    }
//	}


	/**
	 * This test checks that the key parameter in HashMap#computeIfAbsent is not assigned to an internal field.
	 * It ensures that the static analysis tool correctly identifies that there is no assign relation.
	 */
	@Test
	public void methodCallChain_AssignAndReturnRelationTest() {
	    String methodSignature = "java.util.HashMap#computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;";
	    String className = "java.util.HashMap";
	    String paramName = "key";
	    int paramIndex = 1;

	    try {
	        CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
	        AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg, methodSignature);

	        // Check if 'key' is assigned to any internal field across method calls
	        AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(paramName, paramIndex, className);
	        ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

	        // 'key' should not be assigned to any internal field
	        assertEquals(AssignRelation.getGuaranteeNoAssignRelation(), assignRelation);
	        
	        // Return value might not be guaranteed due to the nature of compute logic
	        assertEquals(ReturnRelation.getNoGuaranteeReturnRelation(), returnRelation);

	    } catch (CannotBuildCFGException e) {
	        e.printStackTrace();
	    }
	}


}
