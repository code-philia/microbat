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

	/**
	 * `size` is written after method invocation.
	 */
	@Test
	public void getVarWriteStatus_afterMethodInvocation_GuaranteeWrite2() {
		String methodSignature = "java.util.ArrayList#addAll(Ljava/util/Collection;)Z";
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
	 * method might terminate early before `size` is incremented.
	 */
	@Test
	public void getVarWriteStatus_afterReturn_NoGuarantee() {
		String methodSignature = "java.util.HashMap#put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
		String className = "java.util.HashMap";
		String fieldName = "size";

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
	 * `size` is decreased on one of the control branches (not all).
	 */
	@Test
	public void getVarWriteStatus_oneBranchModification_NoGuarantee() {
		String methodSignature = "java.util.HashMap#remove(Ljava/lang/Object;)Ljava/lang/Object;";
		String className = "java.util.HashMap";
		String fieldName = "size";

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
	 * method might terminate early before `head` is written.
	 */
	@Test
	public void getVarWriteStatus_afterThrow_NoGuarantee() {
		String methodSignature = "java.util.ArrayDeque#push(Ljava/lang/Object;)V";
		String className = "java.util.ArrayDeque";
		String fieldName = "head";

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
	 * `size` is guaranteed to be written in nested method invocations.
	 */
	@Test
	public void getVarWriteStatus_withinMethodInvocation_GuaranteeWrite() {
		String methodSignature = "java.util.LinkedList#push(Ljava/lang/Object;)V";
		String className = "java.util.LinkedList";
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
