package microbat.tracerecov;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import microbat.codeanalysis.bytecode.CFG;
import microbat.tracerecov.staticverifiers.AliasRelationsVerifier;
import microbat.tracerecov.staticverifiers.AssignRelation;
import microbat.tracerecov.staticverifiers.AssignStatus;
import microbat.tracerecov.staticverifiers.ReturnRelation;
import microbat.tracerecov.staticverifiers.ReturnStatus;

public class AliasRelationsVerifierTest {

	/**
	 * Always bypass elements in array for both assign and return status.
	 */
	@Test
	public void elementInArray_NoGuarantee() {
		String methodSignature = "java.util.ArrayList#get(I)Ljava/lang/Object;";
		String fieldName = "elementData[0]";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg);
			AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(fieldName);
			ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

			assertEquals(AssignStatus.NO_GUARANTEE, assignRelation.getAssignStatus());
			assertEquals(ReturnStatus.NO_GUARANTEE, returnRelation.getReturnStatus());
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Nothing will be assigned since there is no input parameters. `buf` will be
	 * returned.
	 */
	@Test
	public void noMethodInvocation_GuaranteeReturn() {
		String methodSignature = "java.io.StringWriter#getBuffer()Ljava/lang/StringBuffer;";
		String fieldName = "buf";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg);
			AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(fieldName);
			ReturnRelation returnRelation = aliasRelationsVerifier.getVarReturnRelation();

			assertEquals(AssignStatus.GUARANTEE_NO_ASSIGN, assignRelation.getAssignStatus());
			assertEquals(ReturnStatus.GUARANTEE_RETURN, returnRelation.getReturnStatus());
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

}
