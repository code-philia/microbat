package microbat.instrumentation.filter;

import java.util.HashSet;
import java.util.Set;

public class JdkFilter {
	private static final String[] jdkExclusivesArray = new String[] {
			"java.lang.Integer",
			"java.lang.Boolean",
			"java.lang.Float",
			"java.lang.Character",
			"java.lang.Double",
			"java.lang.Long",
			"java.lang.Short",
			"java.lang.Byte",
			"java.lang.Object",
			"java.lang.String",
			"java.lang.Thread",
			"java.lang.ThreadLocal",
			"java.lang.Error",
			"java.lang.AssertionError",
			"java.lang.Class",
			"java.lang.StringBuffer",
			"java.lang.StringBuilder",
			"java.lang.System",
			"java.lang.ThreadGroup",
			"java.lang.Throwable"
	};

	private static final Set<String> jdkExclusives;

	private static String[] excludePrefixes = new String[] {
			"sun.",
			"com.sun.",
			"microbat.",
			"java.lang.",
			"jdk.",
			"org.junit.",
			"sav.common.",
			"sav.commons.",
			"sav.strategies.",
			"java.nio.",
			"java.util.concurrent.",
			"java.io.",
			"java.net.",
			"java.security.",
			// "java.",
			"junit.",
			"org.testng.",
			"java.util.regex.",
			"org.apache.bcel.",
			"java.time.",
	};

	static {
		jdkExclusives = new HashSet<>();
		for (String className : jdkExclusivesArray) {
			jdkExclusives.add(className);
		}
	}

	public static boolean filter(String className) {
		// if (jdkExclusives.contains(className)) {
		// 	return false;
		// }
		// return true;
		return filterClass(className);
	}

	public static boolean filterClass(String className) {
		if (jdkExclusives.contains(className)) {
			return false;
		}
		for (String prefix : excludePrefixes) {
			if (className.startsWith(prefix)) {
				return false;
			}
		}
		return true;
	}

}
