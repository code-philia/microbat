package microbat.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import microbat.codeanalysis.runtime.PreCheckInformation;
import microbat.instrumentation.output.RunningInfo;
import microbat.instrumentation.precheck.PrecheckInfo;
import microbat.model.trace.Trace;

public class ExecTraceFileReader {
	private String msg;
	private Trace trace;
	private List<Trace> allTraces;
	
	public Trace read(String execTraceFile) {
		RunningInfo info = RunningInfo.readFromFile(execTraceFile);
		this.trace = info.getMainTrace();
		this.msg = info.getProgramMsg();
		this.allTraces = info.getTraceList();
		return trace;
	}
	
	public List<Trace> getAllTraces() {
		return this.allTraces;
	}
	
	public PreCheckInformation readPrecheck(String precheckFile) {
		PrecheckInfo info = PrecheckInfo.readFromFile(precheckFile);
		PreCheckInformation result = new PreCheckInformation(info.getThreadNum(), info.getStepTotal(), info.isOverLong(),
				new ArrayList<>(info.getVisitedLocs()), info.getExceedingLimitMethods(), info.getLoadedClasses());
		return result;
	}
	
	public String getMsg() {
		return msg;
	}
}
