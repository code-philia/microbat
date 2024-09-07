package microbat.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import sav.common.core.Pair;

public class VarExpMatchTest {
	private static int matchedNum = 0;
	private static int wrongPredictionNum = 0;
	private static int notPredictedNum = 0;

	public static void main(String[] args) {
		String gt = "{\"map|java.util.HashMap\":{\"TREEIFY_THRESHOLD|int\":\"8\",\"entrySet|java.util.HashMap$EntrySet\":{\"this$0|java.util.HashMap\":{\"TREEIFY_THRESHOLD|int\":\"8\",\"entrySet|java.util.HashMap$EntrySet\":\"[]\",\"UNTREEIFY_THRESHOLD|int\":\"6\",\"MIN_TREEIFY_CAPACITY|int\":\"64\",\"size|int\":\"0\"}},\"values|interface java.util.Collection\":\"null\",\"UNTREEIFY_THRESHOLD|int\":\"6\",\"MIN_TREEIFY_CAPACITY|int\":\"64\",\"modCount|int\":\"0\",\"keySet|interface java.util.Set\":\"null\",\"size|int\":\"0\",\"table|class [Ljava.util.HashMap$Node;\":\"null\",\"threshold|int\":\"0\"}}";
		String pre = "{\"map|java.util.HashMap\":{\"TREEIFY_THRESHOLD|int\":\"8\",\"entrySet|java.util.HashMap$EntrySet\":{\"this$0|java.util.HashMap\":{\"TREEIFY_THRESHOLD|int\":\"8\",\"entrySet|java.util.HashMap$EntrySet\":\"[]\",\"UNTREEIFY_THRESHOLD|int\":\"6\",\"MIN_TREEIFY_CAPACITY|int\":\"64\",\"size|int\":\"0\"}},\"values|interface java.util.Collection\":\"null\",\"UNTREEIFY_THRESHOLD|int\":\"6\",\"MIN_TREEIFY_CAPACITY|int\":\"64\",\"modCount|int\":\"0\",\"keySet|interface java.util.Set\":\"null\",\"size|int\":\"0\",\"table|class [Ljava.util.HashMap$Node;\":\"null\"}}";
		calculatePR(gt, pre);
	}

	protected static Pair<Double, Double> calculatePR(String groundtruth, String prediction) {
		// parse to json object
		System.out.println("Original string: ");
		System.out.println("groundtruth: " + groundtruth);
		System.out.println("prediction: " + prediction);

		JSONObject groundtruthJson = processResponse(groundtruth);
		JSONObject predictionJson = processResponse(prediction);

		Object expansion1 = null;
		for (String key : groundtruthJson.keySet()) {
			expansion1 = groundtruthJson.get(key);
			break; // only contains one (root var)
		}

		Object expansion2 = null;
		for (String key : predictionJson.keySet()) {
			expansion2 = predictionJson.get(key);
			break;
		}
		matchJsonTree(expansion1, expansion2);

		double precision = 0;
		if (matchedNum + wrongPredictionNum != 0) {
			precision = (double) matchedNum / (matchedNum + wrongPredictionNum);
		}
		double recall = 0.0;
		if (matchedNum + notPredictedNum != 0) {
			recall = (double) matchedNum / (matchedNum + notPredictedNum);
		}

		System.out.println("PR: " + precision + ", " + recall);

		return Pair.of(precision, recall);
	}

	protected static JSONObject processResponse(String response) {
		int begin = response.indexOf("{");
		int end = response.lastIndexOf("}");
		response = response.substring(begin, end + 1);
		JSONObject variable = new JSONObject(response);
		return variable;
	}

	protected static void matchJsonTree(Object json1, Object json2) {
		if (json1 instanceof JSONObject && json2 instanceof JSONObject) {
			Set<String> keySet1 = ((JSONObject) json1).keySet();
			Set<String> keySet2 = ((JSONObject) json2).keySet();
			System.out.println("In matchJsonTree:");
			System.out.println("keySet1: " + keySet1);
			System.out.println("keySet2: " + keySet2);

			Set<String> matched1 = new HashSet<String>();
			Set<String> matched2 = new HashSet<String>();

			for (String key1 : keySet1) {
				for (String key2 : keySet2) {
					if (matched2.contains(key2)) {
						continue;
					}
					// successfully matched
					if (nameTypeMatch(key1, key2)) {
						matchedNum += 1;
						matched1.add(key1);
						matched2.add(key2);
						matchJsonTree(((JSONObject) json1).get(key1), ((JSONObject) json2).get(key2));
						break;
					}
				}
			}

			for (String key1 : keySet1) {
				if (!matched1.contains(key1)) {
					notPredictedNum += 1;
					processNotMatchedNode(((JSONObject) json1).get(key1), true);
				}
			}
			for (String key2 : keySet2) {
				if (!matched2.contains(key2)) {
					wrongPredictionNum += 1;
					processNotMatchedNode(((JSONObject) json2).get(key2), false);
				}
			}

		} else if (json1 instanceof JSONObject) {
			notPredictedNum += ((JSONObject) json1).keySet().size();
			for (String key : ((JSONObject) json1).keySet()) {
				processNotMatchedNode(((JSONObject) json1).get(key), true);
			}
		} else if (json2 instanceof JSONObject) {
			wrongPredictionNum += ((JSONObject) json2).keySet().size();
			for (String key : ((JSONObject) json2).keySet()) {
				processNotMatchedNode(((JSONObject) json2).get(key), false);
			}
		}

	}

	protected double getAvg(List<Double> precisionList) {
		double sum = 0.0;
		if (!precisionList.isEmpty()) {
			for (Double number : precisionList) {
				sum += number;
			}
			double average = sum / precisionList.size();
			return average;
		} else {
			return 0.0;
		}
	}

	protected static boolean nameTypeMatch(String key1, String key2) {
		String[] name_type1 = key1.split("|");
		String[] name_type2 = key2.split("|");

		String name1 = name_type1[0];
		String type1 = name_type1.length == 2 ? name_type1[1] : "null";
		String name2 = name_type2[0];
		String type2 = name_type2.length == 2 ? name_type2[1] : "null";

		boolean nameMatch = (name1.equals(name2) || name1.contains(name2) || name2.contains(name1));
		boolean typeMatch = (type1.equals(type2) || type1.contains(type2) || type2.contains(type1));

		return nameMatch && typeMatch;
	}

	protected static void processNotMatchedNode(Object child, boolean isOnGt) {
		if (!(child instanceof JSONObject)) {
			return;
		}
		if (isOnGt) {
			notPredictedNum += ((JSONObject) child).keySet().size();
		} else {
			wrongPredictionNum += ((JSONObject) child).keySet().size();
		}
		for (String key : ((JSONObject) child).keySet()) {
			processNotMatchedNode(((JSONObject) child).get(key), isOnGt);
		}
	}
}
