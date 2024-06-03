package microbat.handler;

import org.eclipse.core.runtime.jobs.Job;

import microbat.instrumentation.output.RunningInfo;

public class ReplayStats {
	
	/**
	 * The program msg sent to Agent._exitProgram
	 */
	public String programMsgString;
	/**
	 * False if not junit test.
	 */
	public boolean hasPassedTest = false;
	/**
	 * The stdout data
	 */
	public String stderr;
	/**
	 * The size of the recording.
	 */
	public long dumpFileSize = -1;
	/**
	 * The size of the logs.
	 */
	public long traceFileSize = -1;
	/**
	 * The runtime in miliseconds
	 */
	public long runTime = -1;
	
	/**
	 * The memory used during execution
	 */
	public long memoryUsed = -1;
	

	public ReplayStats() {
		
	}

	public String getProgramMsgString() {
		return programMsgString;
	}

	public void setProgramMsgString(String programMsgString) {
		this.programMsgString = programMsgString;
	}

	public boolean isHasPassedTest() {
		return hasPassedTest;
	}

	public void setHasPassedTest(boolean hasPassedTest) {
		this.hasPassedTest = hasPassedTest;
	}

	public String getStdout() {
		if (stderr == null) return "null";
		return stderr;
	}

	public void setStdError(String stderrString) {
		if (stderrString == null) this.stderr = "null";
		this.stderr = stderrString;
	}

	public long getDumpFileSize() {
		return dumpFileSize;
	}

	public void setDumpFileSize(long dumpFileSize) {
		this.dumpFileSize = dumpFileSize;
	}

	public long getTraceFileSize() {
		return traceFileSize;
	}

	public void setTraceFileSize(long traceFileSize) {
		this.traceFileSize = traceFileSize;
	}

	public double getRunTime() {
		return runTime;
	}

	public void setRunTime(long runTime) {
		this.runTime = runTime;
	}
	
	public void updateFromRunningInfo(RunningInfo info) {
		this.setHasPassedTest(info.hasPassedTest());
		this.setProgramMsgString(info.getProgramMsg());
	}

}