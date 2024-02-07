/**
 * 
 */
package microbat.sql;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.bcel.generic.InstructionHandle;

import microbat.instrumentation.Agent;
import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.AgentParams;
import microbat.instrumentation.instr.instruction.info.LineInstructionInfo;
import microbat.instrumentation.instr.instruction.info.SerializableLineInfo;
import microbat.instrumentation.output.RunningInfo;
import microbat.model.BreakPoint;
import microbat.model.trace.Trace;

/**
 * @author knightsong
 *
 */
public class FileRecorder implements TraceRecorder {
	AgentParams agentParams;
	public FileRecorder(AgentParams agentParams) {
		this.agentParams= agentParams;
	}

	/* 
	 * @see microbat.sql.TraceRecorder#store(microbat.model.trace.Trace)
	 */
	@Override
	public void store(List<Trace> traceList) {
		
//		Trace trace = traceList.get(0);
		
		int collectedSteps = traceList.get(0).getExecutionList().size();
		int expectedSteps = agentParams.getExpectedSteps();
		RunningInfo result = new RunningInfo(Agent.getProgramMsg(), traceList, collectedSteps, expectedSteps);
		try {
			result.saveToFile(agentParams.getDumpFile(), false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		AgentLogger.debug(result.toString());
		
	}
	
	@Override
	public void serialize(HashMap<Integer, SerializableLineInfo> instructionTable) {
		try {
			FileOutputStream fos = new FileOutputStream("C:\\Users\\Siang\\AppData\\Local\\Temp\\serialize.tmp");
			fos.getChannel().lock();
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(instructionTable);
			oos.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
