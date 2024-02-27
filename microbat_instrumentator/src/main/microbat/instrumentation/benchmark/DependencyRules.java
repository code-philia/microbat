package microbat.instrumentation.benchmark;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyRules {
	
	static public Set<String> classes = new HashSet<>();
	static public Map<String, Set<String>> writters = new HashMap<>();
	static public Map<String, Set<String>> getters = new HashMap<>();
	
	static public enum Type {
		NONE,
		IS_WRITTER,
		IS_GETTER
	}

	public DependencyRules() {}
	
	public static void setUp() {
		String[] classNames = new String[] {
				"java.util.List",
				"java.util.Map",
				"java.util.Set",
				"java.util.Collection",
				"java.lang.Appendable",
				"java.lang.CharSequence"
				};
		
		List<List<String>> writterMethods = Arrays.asList(
				// list
				Arrays.asList("add(Ljava/lang/Object;)Z", "add(ILjava/lang/Object;)V"),
				// map
				Arrays.asList("put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
				// set
				Arrays.asList("add(Ljava/lang/Object;)Z"),
				// collection
				Arrays.asList("add(Ljava/lang/Object;)Z"),
				// appendable
				Arrays.asList("append(Ljava/lang/CharSequence;)Ljava/lang/Appendable;", 
						"append(Ljava/lang/CharSequence;II)Ljava/lang/Appendable;", 
						"append(C)Ljava/lang/Appendable;"),
				// charsequence
				Arrays.asList("")
				);
		
		List<List<String>> getterMethods = Arrays.asList(
				// list
				Arrays.asList("get(I)Ljava/lang/Object;"),
				// map
				Arrays.asList("get(Ljava/lang/Object;)Ljava/lang/Object;"),
				// set
				Arrays.asList(""),
				// collection
				Arrays.asList(""),
				// appendable
				Arrays.asList(""),
				// charsequence
				Arrays.asList("charAt(I)C", 
						"subSequence(II)Ljava/lang/CharSequence;", 
						"toString()Ljava/lang/String;")
				);
		
		for (int i = 0; i < classNames.length; i++) {
			String key = classNames[i];
			classes.add(key);
			writters.put(key, new HashSet<String>(writterMethods.get(i)));
			getters.put(key, new HashSet<String>(getterMethods.get(i)));
		}
	}
	
	public static Type getType(String method) {
		if (classes.isEmpty()) {
			setUp();
		}
		
		String[] methodInfo = method.split("#");
		String className = methodInfo[0];
		String methodSignature = methodInfo[1];
		if (classes.contains(className)) {
			if (writters.get(className).contains(methodSignature)) {
				return Type.IS_WRITTER;
			}
			if (getters.get(className).contains(methodSignature)) {
				return Type.IS_GETTER;
			}
		}
		return Type.NONE;
	}

}
