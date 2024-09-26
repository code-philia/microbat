package microbat.tracerecov;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import microbat.codeanalysis.bytecode.CFG;
import microbat.tracerecov.staticverifiers.CandidateVarVerifier;
import microbat.tracerecov.staticverifiers.WriteStatus;

public class CandidateVarVerifierTest {

	/**
	 * Special case: Always bypass elements in array.
	 */
	@Test
	public void getVarWriteStatus_elementInArray_NoGuarantee() {
		String methodSignature = "java.util.ArrayList#clear()V";
		String className = "java.util.ArrayList";
		String fieldName = "elementData[0]";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName, className);

			assertEquals(WriteStatus.NO_GUARANTEE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 2:putfield java.lang.StringBuffer.toStringCache:[C (11)
	 */
	@Test
	public void getVarWriteStatus_beforeMethodInvocation_GuaranteeWrite() {
		String methodSignature = "java.lang.StringBuffer#append(Ljava/lang/CharSequence;)Ljava/lang/StringBuffer;";
		String className = "java.lang.StringBuffer";
		String fieldName = "toStringCache";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName, className);

			assertEquals(WriteStatus.GUARANTEE_WRITE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * `getBuffer` reads `buf` but doesn't write to it.
	 */
	@Test
	public void getVarWriteStatus_noMethodInvocation_GuaranteeNoWrite() {
		String methodSignature = "java.io.StringWriter#getBuffer()Ljava/lang/StringBuffer;";
		String className = "java.io.StringWriter";
		String fieldName = "buf";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName, className);

			assertEquals(WriteStatus.GUARANTEE_NO_WRITE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * possible to return early
	 */
	@Test
	public void getVarWriteStatus_withinForLoop_NoGuarantee() {
		String methodSignature = "java.util.ArrayList#remove(Ljava/lang/Object;)Z";
		String className = "java.util.ArrayList";
		String fieldName = "modCount";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName, className);

			assertEquals(WriteStatus.NO_GUARANTEE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * `value` is written within StringBuffer class instead of StringWriter.
	 */
	@Test
	public void getVarWriteStatus_withinMethodInvocation_NoGuarantee() {
		String methodSignature = "java.io.StringWriter#append(Ljava/lang/CharSequence;)Ljava/io/StringWriter;";
		String className = "java.io.StringWriter";
		String fieldName = "value";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName, className);

			assertEquals(WriteStatus.GUARANTEE_NO_WRITE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

	/**
	 * `size` is written after method invocation.
	 */
	@Test
	public void getVarWriteStatus_afterMethodInvocation_GuaranteeWrite() {
		String methodSignature = "java.util.ArrayList#add(Ljava/lang/Object;)Z";
		String className = "java.util.ArrayList";
		String fieldName = "size";

		try {
			CFG cfg = TraceRecovUtils.getCFGFromMethodSignature(methodSignature);
			CandidateVarVerifier candidateVarVerifier = new CandidateVarVerifier(cfg);
			WriteStatus writeStatus = candidateVarVerifier.getVarWriteStatus(fieldName, className);

			assertEquals(WriteStatus.GUARANTEE_WRITE, writeStatus);
		} catch (CannotBuildCFGException e) {
			e.printStackTrace();
		}
	}

}
