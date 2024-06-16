package microbat.tracerecov;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import microbat.codeanalysis.bytecode.CFG;
import microbat.tracerecov.candidatevar.CandidateVarVerifier;
import microbat.tracerecov.candidatevar.CandidateVarVerifier.WriteStatus;

public class CandidateVarVerifierTest {

	/**
	 * Always bypass elements in array
	 */
	@Test
	public void getVarWriteStatus_elementInArray_NoGuarantee() {
		String methodSignature = "java.util.ArrayList#clear()V";
		String fieldName = "elementData[0]";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName);

			assertEquals(WriteStatus.NO_GUARANTEE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 2:putfield java.lang.StringBuffer.toStringCache:[C (11)
	 */
	@Test
	public void getVarWriteStatus_GuaranteeWrite() {
		String methodSignature = "java.lang.StringBuffer#append(Ljava/lang/CharSequence;)Ljava/lang/StringBuffer;";
		String fieldName = "toStringCache";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName);

			assertEquals(WriteStatus.GUARANTEE_WRITE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * `getBuffer` reads `buf` but doesn't write to it.
	 */
	@Test
	public void getVarWriteStatus_GuaranteeNoWrite() {
		String methodSignature = "java.io.StringWriter#getBuffer()Ljava/lang/StringBuffer;";
		String fieldName = "buf";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName);

			assertEquals(WriteStatus.GUARANTEE_NO_WRITE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	// TODO: not passed (method invocation not expanded)
	@Test
	public void getVarWriteStatus_forLoop_GuaranteeNoWrite() {
		String methodSignature = "java.util.ArrayList#remove(Ljava/lang/Object;)Z";
		String fieldName = "elementData";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName);

			assertEquals(WriteStatus.GUARANTEE_NO_WRITE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	// TODO: not passed (method invocation not expanded)
	@Test
	public void getVarWriteStatus_withMethodInvocation_GuaranteeWrite() {
		String methodSignature = "java.io.StringWriter#append(Ljava/lang/CharSequence;)Ljava/io/StringWriter;";
		String fieldName = "toStringCache";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName);

			assertEquals(WriteStatus.GUARANTEE_WRITE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	// TODO: not passed (method invocation not expanded)
	@Test
	public void getVarWriteStatus_withMethodInvocation_GuaranteeNoWrite() {
		String methodSignature = "java.io.StringWriter#append(Ljava/lang/CharSequence;)Ljava/io/StringWriter;";
		String fieldName = "buf";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName);

			assertEquals(WriteStatus.GUARANTEE_NO_WRITE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	// TODO: method invocation not expanded
	@Test
	public void getVarWriteStatus_withMethodInvocation_NoGuarantee() {
		String methodSignature = "java.io.StringWriter#append(Ljava/lang/CharSequence;)Ljava/io/StringWriter;";
		String fieldName = "value";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName);

			assertEquals(WriteStatus.NO_GUARANTEE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}
}
