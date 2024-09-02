package microbat.codeanalysis.runtime;

import java.util.HashSet;
import java.util.Set;

public class PkgFilter {
	private static final Set<String> pkgExclusives;
	
	static {
		pkgExclusives = new HashSet<>();
		pkgExclusives.add("java.io");
		pkgExclusives.add("java.net");
		pkgExclusives.add("org.junit");
		pkgExclusives.add("java.util.Collections");
	}

	public static boolean filter(String className) {
		for(String s : pkgExclusives) {
			if(className.startsWith(s)) {
				return false;
			}
		}
		return true;
	}
	
}