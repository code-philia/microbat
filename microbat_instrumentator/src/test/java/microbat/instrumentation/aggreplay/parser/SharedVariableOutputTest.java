package microbat.instrumentation.aggreplay.parser;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import microbat.instrumentation.instr.aggreplay.output.SharedVariableOutput;
import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.SharedDataParser;
import microbat.instrumentation.model.generator.SharedVariableObjectGenerator;

public class SharedVariableOutputTest {
	
	/**
	 * Thread used to test, adds access to a given object in a different thread
	 * for a particular string
	 * @author Gabau
	 *
	 */
	private static class BasicIncrement extends Thread {
		Object testObject;
		SharedVariableObjectGenerator objGenerator;
		String fieldName;
		
		protected BasicIncrement(Object testObject, 
				SharedVariableObjectGenerator objGenerator, String fieldName) {
			this.testObject = testObject;
			this.objGenerator = objGenerator;
			this.fieldName = fieldName;
		}
		
		@Override
		public void run() {
			this.objGenerator
				.getId(testObject)
				.addAccess(Thread.currentThread()
						.getId(), fieldName);
		}
		
		public void startUntilStop() throws InterruptedException {
			this.start();
			this.join();
		}
		
	}

	@Test
	public void testSimpleObjectGeneration() throws IOException, InterruptedException {
		SharedVariableObjectGenerator generator = new SharedVariableObjectGenerator();
		generator.createId(generator);
		SharedVariableOutput output = new SharedVariableOutput(generator);
		String result = output.getFromStore();
		BasicIncrement increment1 = new BasicIncrement(generator, generator, "field");
		BasicIncrement increment2 = new BasicIncrement(generator, generator, "field");
		increment1.startUntilStop();
		increment2.startUntilStop();
		List<ParseData> data = new SharedDataParser().parse(new StringReader(result));
		SharedVariableOutput newOutput = new SharedVariableOutput(data.get(0));
		assertEquals(newOutput.getObjects(), output.getObjects());
	}
	
}
