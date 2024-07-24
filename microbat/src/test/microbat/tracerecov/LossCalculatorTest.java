package microbat.tracerecov;

import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import microbat.tracerecov.autoprompt.LossCalculator;

public class LossCalculatorTest {
	
	@Test
	public void testOtherTypes() {
		String string1 = "{\n\"size:int\":5\n}";
		String string2 = "{\n\"size:int\":1\n}";

		JSONObject json1 = new JSONObject(string1);
		JSONObject json2 = new JSONObject(string2);

		LossCalculator lossCalculator = new LossCalculator();
		double loss = lossCalculator.computeLoss(json1, json2);

		System.out.println("testOtherTypes: " + loss);
		assertTrue(loss == 0.5);
	}
	
	@Test
	public void testSimpleExample() {
		String string1 = "{\n"
				+ "  \"list:java.util.ArrayList\":{\n"
				+ "    \"elementData:java.lang.Object[]\":[\n"
				+ "1,2,3,4,5,null,null,null,null,null],\n"
				+ "    \"size:int\":5\n"
				+ "  }\n"
				+ "}";
		String string2 = "{\n"
				+ "  \"list:java.util.ArrayList\":{\n"
				+ "    \"elementData:java.lang.Object[]\":[\n"
				+ "1,null,null,null,null,null,null,null,null,null],\n"
				+ "    \"size:int\":1\n"
				+ "  }\n"
				+ "}";

		JSONObject json1 = new JSONObject(string1);
		JSONObject json2 = new JSONObject(string2);

		LossCalculator lossCalculator = new LossCalculator();
		double loss = lossCalculator.computeLoss(json1, json2);

		System.out.println("testSimpleExample: " + loss);
		assertTrue(loss == (double) 19 / (double) 66);
	}

	@Test
	public void largeLoss() {
		String string1 = "{\n"
				+ "  \"table:java.util.HashMap\":{\n"
				+ "    \"TREEIFY_THRESHOLD:int\":8,\n"
				+ "    \"entrySet:java.util.HashMap$EntrySet\":{\n"
				+ "      \"this$0:java.util.HashMap\":{\n"
				+ "        \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=0\",\"r=0\",\"null\",\"null\",\"null\",\"null\",\"g=0\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"]\n"
				+ "      }\n"
				+ "    },\n"
				+ "    \"UNTREEIFY_THRESHOLD:int\":6,\n"
				+ "    \"MIN_TREEIFY_CAPACITY:int\":64,\n"
				+ "    \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=0\",\"r=0\",\"null\",\"null\",\"null\",\"null\",\"g=0\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"],\n"
				+ "    \"size:int\":5\n"
				+ "  }\n"
				+ "}";
		String string2 = "{\n"
				+ "  \"table: java.util.HashMap\": {\n"
				+ "    \"key[0]: java.lang.String\": \"a\",\n"
				+ "    \"value[0]: java.lang.Integer\": 0,\n"
				+ "    \"key[1]: java.lang.String\": \"r\",\n"
				+ "    \"value[1]: java.lang.Integer\": 0,\n"
				+ "    \"key[2]: java.lang.String\": \"g\",\n"
				+ "    \"value[2]: java.lang.Integer\": 0,\n"
				+ "    \"key[3]: java.lang.String\": \"m\",\n"
				+ "    \"value[3]: java.lang.Integer\": 0,\n"
				+ "    \"key[4]: java.lang.String\": \"n\",\n"
				+ "    \"value[4]: java.lang.Integer\": -1,\n"
				+ "    \"size: int\": 5,\n"
				+ "    \"table: java.util.HashMap$Node[]\": [],\n"
				+ "    \"entrySet: java.util.Set\": [],\n"
				+ "    \"modCount: int\": 0,\n"
				+ "    \"threshold: int\": 0,\n"
				+ "    \"loadFactor: float\": 0.75,\n"
				+ "    \"keySet: java.util.Set\": [],\n"
				+ "    \"values: java.util.Collection\": []\n"
				+ "  }\n"
				+ "}";

		JSONObject json1 = new JSONObject(string1);
		JSONObject json2 = new JSONObject(string2);

		LossCalculator lossCalculator = new LossCalculator();
		double loss = lossCalculator.computeLoss(json1, json2);

		System.out.println("largeLoss: " + loss);
		assertTrue(loss > 0.8);
	}

	@Test
	public void smallLoss() {
		String string1 = "{\n"
				+ "  \"table:java.util.HashMap\":{\n"
				+ "    \"TREEIFY_THRESHOLD:int\":8,\n"
				+ "    \"entrySet:java.util.HashMap$EntrySet\":{\n"
				+ "      \"this$0:java.util.HashMap\":{\n"
				+ "        \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=10\",\"null\",\"null\",\"null\",\"null\",\"null\",\"g=5\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"]\n"
				+ "      }\n"
				+ "    },\n"
				+ "    \"UNTREEIFY_THRESHOLD:int\":6,\n"
				+ "    \"MIN_TREEIFY_CAPACITY:int\":64,\n"
				+ "    \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=10\",\"null\",\"null\",\"null\",\"null\",\"null\",\"g=5\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"],\n"
				+ "    \"size:int\":4\n"
				+ "  }\n"
				+ "}";
		String string2 = "{\n"
				+ "  \"table:java.util.HashMap\":{\n"
				+ "    \"TREEIFY_THRESHOLD:int\":8,\n"
				+ "    \"entrySet:java.util.HashMap$EntrySet\":{\n"
				+ "      \"this$0:java.util.HashMap\":{\n"
				+ "        \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=0\",\"r=0\",\"null\",\"null\",\"null\",\"null\",\"g=0\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"]\n"
				+ "      }\n"
				+ "    },\n"
				+ "    \"UNTREEIFY_THRESHOLD:int\":6,\n"
				+ "    \"MIN_TREEIFY_CAPACITY:int\":64,\n"
				+ "    \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=0\",\"r=0\",\"null\",\"null\",\"null\",\"null\",\"g=0\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"],\n"
				+ "    \"size:int\":5\n"
				+ "  }\n"
				+ "}";

		JSONObject json1 = new JSONObject(string1);
		JSONObject json2 = new JSONObject(string2);

		LossCalculator lossCalculator = new LossCalculator();
		double loss = lossCalculator.computeLoss(json1, json2);

		System.out.println("smallLoss: " + loss);
		assertTrue(loss < 0.2);
	}
	
	@Test
	public void zeroLoss() {
		String string1 = "{\n"
				+ "  \"table:java.util.HashMap\":{\n"
				+ "    \"TREEIFY_THRESHOLD:int\":8,\n"
				+ "    \"entrySet:java.util.HashMap$EntrySet\":{\n"
				+ "      \"this$0:java.util.HashMap\":{\n"
				+ "        \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=0\",\"r=0\",\"null\",\"null\",\"null\",\"null\",\"g=0\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"]\n"
				+ "      }\n"
				+ "    },\n"
				+ "    \"UNTREEIFY_THRESHOLD:int\":6,\n"
				+ "    \"MIN_TREEIFY_CAPACITY:int\":64,\n"
				+ "    \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=0\",\"r=0\",\"null\",\"null\",\"null\",\"null\",\"g=0\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"],\n"
				+ "    \"size:int\":5\n"
				+ "  }\n"
				+ "}";
		String string2 = "{\n"
				+ "  \"table:java.util.HashMap\":{\n"
				+ "    \"TREEIFY_THRESHOLD:int\":8,\n"
				+ "    \"entrySet:java.util.HashMap$EntrySet\":{\n"
				+ "      \"this$0:java.util.HashMap\":{\n"
				+ "        \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=0\",\"r=0\",\"null\",\"null\",\"null\",\"null\",\"g=0\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"]\n"
				+ "      }\n"
				+ "    },\n"
				+ "    \"UNTREEIFY_THRESHOLD:int\":6,\n"
				+ "    \"MIN_TREEIFY_CAPACITY:int\":64,\n"
				+ "    \"table:java.util.HashMap$Node[]\":[\n"
				+ "\"null\",\"a=0\",\"r=0\",\"null\",\"null\",\"null\",\"null\",\"g=0\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=0\",\"n=-1\",\"null\"],\n"
				+ "    \"size:int\":5\n"
				+ "  }\n"
				+ "}";

		JSONObject json1 = new JSONObject(string1);
		JSONObject json2 = new JSONObject(string2);

		LossCalculator lossCalculator = new LossCalculator();
		double loss = lossCalculator.computeLoss(json1, json2);

		System.out.println("zeroLoss: " + loss);
		assertTrue(loss == 0);
	}
}
