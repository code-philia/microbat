package microbat.tracerecov;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import microbat.codeanalysis.bytecode.CFG;
import microbat.tracerecov.staticverifiers.AliasRelationsVerifier;
import microbat.tracerecov.staticverifiers.AliasRelationsVerifier.AssignStatus;
import microbat.tracerecov.staticverifiers.AliasRelationsVerifier.ReturnStatus;

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
			AssignStatus assignStatus = aliasRelationsVerifier.getVarAssignStatus(fieldName);
			ReturnStatus returnStatus = aliasRelationsVerifier.getVarReturnStatus(fieldName);

			assertEquals(AssignStatus.NO_GUARANTEE, assignStatus);
			assertEquals(ReturnStatus.NO_GUARANTEE, returnStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}
	
}
