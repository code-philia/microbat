package microbat.tracerecov;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import microbat.codeanalysis.bytecode.CFG;
import microbat.tracerecov.staticverifiers.AliasRelationsVerifier;
import microbat.tracerecov.staticverifiers.AssignRelation;
import microbat.tracerecov.staticverifiers.AssignStatus;
import microbat.tracerecov.staticverifiers.ReturnStatus;

public class AliasRelationsVerifierTest {

	/**
	 * Always bypass elements in array for both assign and return status
	 */
	@Test
	public void getStatus_elementInArray_NoGuarantee() {
		String methodSignature = "java.util.ArrayList#get(I)Ljava/lang/Object;";
		String fieldName = "elementData[0]";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			AliasRelationsVerifier aliasRelationsVerifier = new AliasRelationsVerifier(cfg);
			AssignRelation assignRelation = aliasRelationsVerifier.getVarAssignRelation(fieldName);
			ReturnStatus returnStatus = aliasRelationsVerifier.getVarReturnStatus(fieldName);

			assertEquals(AssignStatus.NO_GUARANTEE, assignRelation.getAssignStatus());
			assertEquals(ReturnStatus.NO_GUARANTEE, returnStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}
	
}
