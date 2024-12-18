package microbat.instrumentation.ondemandtrace;

/**
 * Same structure as {@code TracerMethods}.
 * 
 * @author HongshuW
 */
public enum OnDemandTraceMethods {
	IS_RECORDED(false, "microbat/instrumentation/ondemandtrace/tracestatus/TraceStatusStore", "_isRecorded",
			"(Ljava/lang/String;)Z", 2),
	IS_TO_RECORD(false, "microbat/instrumentation/ondemandtrace/tracestatus/TraceStatusStore", "_isToRecord",
			"(Ljava/lang/String;)Z", 2),
	IS_UNRECORDED(false, "microbat/instrumentation/ondemandtrace/tracestatus/TraceStatusStore", "_isUnrecorded",
			"(Ljava/lang/String;)Z", 2),
	UPDATE_STATUS_TO_RECORD(false, "microbat/instrumentation/ondemandtrace/tracestatus/TraceStatusStore",
			"_updateStatusToRecord", "(Ljava/lang/String;)V", 2),
	UPDATE_STATUS_RECORDED(false, "microbat/instrumentation/ondemandtrace/tracestatus/TraceStatusStore",
			"_updateStatusRecorded", "(Ljava/lang/String;)V", 2),

	;

	private boolean interfaceMethod;
	private String declareClass;
	private String methodName;
	private String methodSign;
	private int argNo;

	private OnDemandTraceMethods(boolean ifaceMethod, String declareClass, String methodName, String methodSign,
			int argNo) {
		this.interfaceMethod = ifaceMethod;
		this.declareClass = declareClass;
		this.methodName = methodName;
		this.methodSign = methodSign;
		this.argNo = argNo;
	}

	public String getDeclareClass() {
		return declareClass;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getMethodSign() {
		return methodSign;
	}

	public int getArgNo() {
		return argNo;
	}

	public boolean isInterfaceMethod() {
		return interfaceMethod;
	}
}
