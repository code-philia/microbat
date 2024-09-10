package microbat.handler;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.json.JSONArray;
import org.json.JSONObject;

import microbat.tracerecov.executionsimulator.ExecutionSimulator;
import microbat.tracerecov.executionsimulator.ExecutionSimulatorFactory;
import sav.common.core.Pair;

public class Atest {
	private static int matchedNum = 0;
	private static int wrongPredictionNum = 0;
	private static int notPredictedNum = 0;
	
	public static void main(String[] args) {
		ExecutionSimulator simulator = ExecutionSimulatorFactory.getExecutionSimulator();
		try {
			String res = simulator.sendRequest("who are you", ", tell me", null);
			System.out.println(res);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		String a = "{\"hashMap|java.util.HashMap\":{\"TREEIFY_THRESHOLD|int\":\"8\",\"entrySet|java.util.HashMap$EntrySet\":{\"this$0|java.util.HashMap\":{\"TREEIFY_THRESHOLD|int\":\"8\",\"entrySet|java.util.HashMap$EntrySet\":\"[a=3, r=1, g=1, m=1, n=1]\",\"UNTREEIFY_THRESHOLD|int\":\"6\",\"MIN_TREEIFY_CAPACITY|int\":\"64\",\"table|java.util.HashMap$Node[]\":[\"null\",\"a=3\",\"r=1\",\"null\",\"null\",\"null\",\"null\",\"g=1\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=1\",\"n=1\",\"null\"],\"size|int\":\"5\"}},\"values|interface java.util.Collection\":\"null\",\"UNTREEIFY_THRESHOLD|int\":\"6\",\"MIN_TREEIFY_CAPACITY|int\":\"64\",\"modCount|int\":\"5\",\"keySet|interface java.util.Set\":\"null\",\"table|java.util.HashMap$Node[]\":\"[null, a=3, r=1, null, null, null, null, g=1, null, null, null, null, null, m=1, n=1, null]\",\"size|int\":\"5\",\"threshold|int\":\"12\"}}";
//		String b = "{\"hashMap|java.util.HashMap\":{\"entrySet|java.util.HashMap$EntrySet\":{\"this$0|java.util.HashMap\":{\"table|java.util.HashMap$Node[]\":[\"null\",\"a=3\",\"r=1\",\"null\",\"null\",\"null\",\"null\",\"g=1\",\"null\",\"null\",\"null\",\"null\",\"null\",\"m=1\",\"n=1\"],\"size|int\":\"5\"},\"size|int\":\"5\"},\"values|interface java.util.Collection\":\"[3, 1, 1, 1, 1]\",\"modCount|int\":\"5\",\"keySet|interface java.util.Set\":\"[a, r, g, m, n]\",\"table|java.util.HashMap$Node[]\":\"[null, a=3, r=1, null, null, null, null, g=1, null, null, null,null,null,m=1,n=1,null] \",\"size|int\":\"5\", \"threshold|int\": \"12\"}}";
//		
//		calculatePR(a,b);
		
	}
	
	protected static Pair<Double, Double> calculatePR(String groundtruth, String prediction) {
		// parse to json object
		System.out.println("Original string: ");
		System.out.println("groundtruth: " + groundtruth);
		System.out.println("prediction: " + prediction);

		JSONObject groundtruthJson = processResponse(groundtruth);
		JSONObject predictionJson = processResponse(prediction);

		matchJsonTree(groundtruthJson, predictionJson);

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
			Set<String> matched1 = new HashSet<String>();
			Set<String> matched2 = new HashSet<String>();

			for (String key1 : keySet1) {
				for (String key2 : keySet2) {
					if(isAllUpperCase(key1.split("\\|")[0])) {
						continue;
					}
					if (matched2.contains(key2)) {
						continue;
					}
					// successfully matched
					if (nameTypeMatch(key1, key2)) {
						Object child1 = ((JSONObject) json1).get(key1);
						Object child2 = ((JSONObject) json2).get(key2);
						if (child1 instanceof JSONObject && child2 instanceof JSONObject) {
							matchedNum += 1;
							matched1.add(key1);
							matched2.add(key2);
						} else if (valueMatch(child1, child2)) {
							matchedNum += 1;
							matched1.add(key1);
							matched2.add(key2);
						}
						matchJsonTree(child1, child2);
						break;
					}
				}
			}

			for (String key1 : keySet1) {
				if (!matched1.contains(key1) && !isAllUpperCase(key1.split("\\|")[0])) {
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
		String[] name_type1 = key1.split("\\|");
		String[] name_type2 = key2.split("\\|");

		String name1 = name_type1[0];
		String type1 = name_type1.length == 2 ? name_type1[1] : "null";
		String name2 = name_type2[0];
		String type2 = name_type2.length == 2 ? name_type2[1] : "null";

		boolean nameMatch = (name1.equals(name2) || name1.contains(name2)) || name2.contains(name1);
		boolean typeMatch = (type1.equals(type2) || type1.contains(type2)) || type2.contains(type1);

		return nameMatch && typeMatch;
	}

	protected static boolean valueMatch(Object value1, Object value2) {
		if(value1.toString().equals("null") ||  calStringSim(value1.toString(),value2.toString())>0.75) {
			return true;
		}
		if (value1 instanceof JSONObject || value2 instanceof JSONObject) {
			return false;
		} else if (value1 instanceof JSONArray && value2 instanceof JSONArray) {
			return calJSONArraySim((JSONArray) value1, (JSONArray) value2) > 0.75;
		} else if ((!(value1 instanceof JSONArray)) && (!(value2 instanceof JSONArray))) {
			return calStringSim(value1.toString(), value2.toString()) > 0.75;
		}
		return false;
	}

	protected static void processNotMatchedNode(Object child, boolean isOnGt) {
		if (!(child instanceof JSONObject)) {
			return;
		}
		if (isOnGt) {
			for (String key : ((JSONObject) child).keySet()) {
				if (!isAllUpperCase(key.split("\\|")[0])) {
					notPredictedNum += 1;
				}
			}
		} else {
			wrongPredictionNum += ((JSONObject) child).keySet().size();
		}
		for (String key : ((JSONObject) child).keySet()) {
			processNotMatchedNode(((JSONObject) child).get(key), isOnGt);
		}
	}

	public static boolean isAllUpperCase(String str) {
		return str.chars().filter(Character::isLetter).allMatch(Character::isUpperCase);
	}

	public static double calJSONArraySim(JSONArray array1, JSONArray array2) {
		if(array1.toString().contains(array2.toString()) || array2.toString().contains(array1.toString())) {
			return 1.0;
		}
		int matchCount = 0;
		for (int i = 0; i < array1.length(); i++) {
			Object element = array1.get(i);
			if (array2.toList().contains(element)) {
				matchCount++;
			}
		}
		int maxSize = Math.max(array1.length(), array2.length());
		return (double) matchCount / maxSize;
	}

	public static double calStringSim(String str1, String str2) {
		if(str1.contains(str2) || str2.contains(str1)) {
			return 1.0;
		}
		LevenshteinDistance levenshtein = new LevenshteinDistance();
		int distance = levenshtein.apply(str1, str2);

		int maxLength = Math.max(str1.length(), str2.length());
		return 1.0 - (double) distance / maxLength;
	}
	
	public static boolean successfullyRecovField(String prediction, String field) {
		int begin = prediction.indexOf("{");
		int end = prediction.lastIndexOf("}");
		prediction = prediction.substring(begin, end + 1);
		JSONObject jsonObj = new JSONObject(prediction);
		
		return successfullyRecovFieldRecur(jsonObj,field);
	}
	
	public static boolean successfullyRecovFieldRecur(JSONObject jsonObj, String field) {
		for(String key : jsonObj.keySet()) {
			String name = null;
			if(key.contains("|")) {
				name = key.split("\\|")[0];
			}
			else {
				name = key;
			}
			if(name.contains(field)) {
				return true;
			}
		}
		
		for(String key : jsonObj.keySet()) {
			Object value = jsonObj.get(key);
			if(value instanceof JSONObject) {
				if(successfullyRecovFieldRecur((JSONObject)value,field)) {
					return true;
				}
			}
		}
		
		return false;
	}
}
