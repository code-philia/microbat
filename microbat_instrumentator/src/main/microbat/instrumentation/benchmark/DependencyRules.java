package microbat.instrumentation.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ClassGen;
import org.apache.commons.io.IOUtils;

import microbat.instrumentation.benchmark.MethodInfo.Action;
import microbat.instrumentation.benchmark.MethodInfo.Index;
import microbat.instrumentation.benchmark.MethodInfo.Type;

public class DependencyRules {
	
	static public Set<String> classes = new HashSet<>();
	static public Map<String, ClassInfo> classInfoMap = new HashMap<>();

	public DependencyRules() {}
	
	public static void setUp() {
		String[] classNames = new String[] {
				"java.util.LinkedList",
				"java.util.ArrayList",
				
				"java.util.HashMap",
				
				"java.util.HashSet",
				
//				"java.lang.StringBuffer",
//				"java.io.StringWriter",
//				"java.io.PrintWriter",
//				"java.lang.StringBuilder",
//				
//				"java.util.Iterator",
//				"java.util.ListIterator",
				
				"java.util.Queue"
				};
		
		List<List<MethodInfo>> writterMethods = Arrays.asList(
				// linkedlist
				Arrays.asList(
						new MethodInfo("add(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.ADD, Index.END),
						new MethodInfo("add(ILjava/lang/Object;)V", Type.IS_SETTER, Action.ADD, Index.INDEX),
						new MethodInfo("addAll(Ljava/util/Collection;)Z", Type.IS_SETTER, Action.ADD, Index.END),
						new MethodInfo("addAll(ILjava/util/Collection;)Z", Type.IS_SETTER, Action.ADD, Index.INDEX),
						new MethodInfo("addFirst(Ljava/lang/Object;)V", Type.IS_SETTER, Action.ADD, Index.START),
						new MethodInfo("addLast(Ljava/lang/Object;)V", Type.IS_SETTER, Action.ADD, Index.END),
						new MethodInfo("clear()V", Type.IS_SETTER, Action.REMOVE, Index.ALL),
						new MethodInfo("offer(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.ADD, Index.END),
						new MethodInfo("offerFirst(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.ADD, Index.START),
						new MethodInfo("offerLast(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.ADD, Index.END),
						new MethodInfo("poll()Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.START),
						new MethodInfo("pollFirst()Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.START),
						new MethodInfo("pollLast()Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.END),
						new MethodInfo("pop()Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.START),
						new MethodInfo("push(Ljava/lang/Object;)V", Type.IS_SETTER, Action.ADD, Index.START),
						new MethodInfo("remove()Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.START),
						new MethodInfo("remove(I)Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.INDEX),
						new MethodInfo("remove(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.REMOVE, Index.NA),
						new MethodInfo("removeFirst()Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.START),
						new MethodInfo("removeFirstOccurrence(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.REMOVE, Index.NA),
						new MethodInfo("removeLast()Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.END),
						new MethodInfo("removeLastOccurrence(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.REMOVE, Index.NA),
						new MethodInfo("set(ILjava/lang/Object;)Ljava/lang/Object;", Type.IS_SETTER, Action.REPLACE, Index.INDEX)),
				// arraylist
				Arrays.asList(
						new MethodInfo("add(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.ADD, Index.END),
						new MethodInfo("add(ILjava/lang/Object;)V", Type.IS_SETTER, Action.ADD, Index.INDEX),
						new MethodInfo("addAll(Ljava/util/Collection;)Z", Type.IS_SETTER, Action.ADD, Index.END),
						new MethodInfo("addAll(ILjava/util/Collection;)Z", Type.IS_SETTER, Action.ADD, Index.INDEX),
						new MethodInfo("clear()V", Type.IS_SETTER, Action.REMOVE, Index.ALL),
						new MethodInfo("forEach(Ljava/util/function/Consumer;)V", Type.IS_SETTER, Action.REPLACE, Index.ALL),
						new MethodInfo("remove(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.REMOVE, Index.NA),
						new MethodInfo("remove(I)Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.INDEX),
						new MethodInfo("removeAll(Ljava/util/Collection;)Z", Type.IS_SETTER, Action.REMOVE, Index.NA),
						new MethodInfo("removeIf(Ljava/util/function/Predicate;)Z", Type.IS_SETTER, Action.REMOVE, Index.NA),
						new MethodInfo("removeRange(II)V", Type.IS_SETTER, Action.REMOVE, Index.INDEX),
						new MethodInfo("replaceAll(Ljava/util/function/UnaryOperator;)V", Type.IS_SETTER, Action.REPLACE, Index.NA),
						new MethodInfo("retainAll(Ljava/util/Collection;)Z", Type.IS_SETTER, Action.REMOVE, Index.NA),
						new MethodInfo("set(ILjava/lang/Object;)Ljava/lang/Object;", Type.IS_SETTER, Action.REPLACE, Index.INDEX),
						new MethodInfo("sort(Ljava/util/Comparator;)V", Type.IS_SETTER, Action.REPLACE, Index.NA)),
				// hashmap
				Arrays.asList(
						new MethodInfo("clear()V", Type.IS_SETTER, Action.REMOVE, Index.ALL),
						new MethodInfo("forEach(Ljava/util/function/BiConsumer;)V", Type.IS_SETTER, Action.REPLACE, Index.ALL),
						new MethodInfo("merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;", Type.IS_SETTER, Action.REPLACE, Index.KEY),
						new MethodInfo("put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", Type.IS_SETTER, Action.ADD, Index.KEY),
						new MethodInfo("putAll(Ljava/util/Map;)V", Type.IS_SETTER, Action.ADD, Index.KEY),
						new MethodInfo("putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", Type.IS_SETTER, Action.ADD, Index.KEY),
						new MethodInfo("remove(Ljava/lang/Object;)Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.KEY),
						new MethodInfo("remove(Ljava/lang/Object;Ljava/lang/Object;)Z", Type.IS_SETTER, Action.REMOVE, Index.KEY),
						new MethodInfo("replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", Type.IS_SETTER, Action.REPLACE, Index.KEY),
						new MethodInfo("replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z", Type.IS_SETTER, Action.REPLACE, Index.KEY),
						new MethodInfo("replaceAll(Ljava/util/function/BiFunction;)V", Type.IS_SETTER, Action.REPLACE, Index.ALL)),
				// hashset
				Arrays.asList(
						new MethodInfo("add(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.ADD, Index.KEY),
						new MethodInfo("clear()V", Type.IS_SETTER, Action.REMOVE, Index.ALL),
						new MethodInfo("remove(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.REMOVE, Index.KEY)),
//				// stringbuffer
//				Arrays.asList("append(Z)Ljava/lang/StringBuffer;",
//						"append(C)Ljava/lang/StringBuffer;",
//						"append([C)Ljava/lang/StringBuffer;",
//						"append([CII)Ljava/lang/StringBuffer;",
//						"append(Ljava/lang/CharSequence;)Ljava/lang/StringBuffer;",
//						"append(Ljava/lang/CharSequence;II)Ljava/lang/StringBuffer;",
//						"append(D)Ljava/lang/StringBuffer;",
//						"append(F)Ljava/lang/StringBuffer;",
//						"append(I)Ljava/lang/StringBuffer;",
//						"append(J)Ljava/lang/StringBuffer;",
//						"append(Ljava/lang/Object;)Ljava/lang/StringBuffer;",
//						"append(Ljava/lang/String;)Ljava/lang/StringBuffer;",
//						"append(Ljava/lang/StringBuffer;)Ljava/lang/StringBuffer;",
//						"appendCodePoint(I)Ljava/lang/StringBuffer;",
//						"delete(II)Ljava/lang/StringBuffer;",
//						"deleteCharAt(I)Ljava/lang/StringBuffer;",
//						"ensureCapacity(I)V",
//						"insert(IZ)Ljava/lang/StringBuffer;",
//						"insert(IC)Ljava/lang/StringBuffer;",
//						"insert(I[C)Ljava/lang/StringBuffer;",
//						"insert(I[CII)Ljava/lang/StringBuffer;",
//						"insert(ILjava/lang/CharSequence;)Ljava/lang/StringBuffer;",
//						"insert(ILjava/lang/CharSequence;II)Ljava/lang/StringBuffer;",
//						"insert(ID)Ljava/lang/StringBuffer;",
//						"insert(IF)Ljava/lang/StringBuffer;",
//						"insert(II)Ljava/lang/StringBuffer;",
//						"insert(IJ)Ljava/lang/StringBuffer;",
//						"insert(ILjava/lang/Object;)Ljava/lang/StringBuffer;",
//						"insert(ILjava/lang/String;)Ljava/lang/StringBuffer;",
//						"replace(IILjava/lang/String;)Ljava/lang/StringBuffer;",
//						"reverse()Ljava/lang/StringBuffer;",
//						"setCharAt(IC)V",
//						"setLength(I)V",
//						"trimToSize()V"),
//				// stringwriter
//				Arrays.asList("append(C)Ljava/io/StringWriter;",
//						"append(Ljava/lang/CharSequence;)Ljava/io/StringWriter;",
//						"append(Ljava/lang/CharSequence;II)Ljava/io/StringWriter;",
//						"close()V",
//						"flush()V",
//						"write([CII)V",
//						"write(I)V",
//						"write(Ljava/lang/String;)V",
//						"write(Ljava/lang/String;II)V"),
//				// printwriter
//				Arrays.asList("append(C)Ljava/io/PrintWriter;",
//						"append(Ljava/lang/CharSequence;)Ljava/io/PrintWriter;",
//						"append(Ljava/lang/CharSequence;II)Ljava/io/PrintWriter;",
//						"checkError()Z",
//						"clearError()V",
//						"close()V",
//						"flush()V",
//						"format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintWriter;",
//						"format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintWriter;",
//						"setError()V",
//						"write([C)V",
//						"write([CII)V",
//						"write(I)V",
//						"write(Ljava/lang/String;)V",
//						"write(Ljava/lang/String;II)V"),
//				// stringbuilder
//				Arrays.asList("append(Z)Ljava/lang/StringBuilder;",
//						"append(C)Ljava/lang/StringBuilder;",
//						"append([C)Ljava/lang/StringBuilder;",
//						"append([CII)Ljava/lang/StringBuilder;",
//						"append(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;",
//						"append(Ljava/lang/CharSequence;II)Ljava/lang/StringBuilder;",
//						"append(D)Ljava/lang/StringBuilder;",
//						"append(F)Ljava/lang/StringBuilder;",
//						"append(I)Ljava/lang/StringBuilder;",
//						"append(J)Ljava/lang/StringBuilder;",
//						"append(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
//						"append(Ljava/lang/String;)Ljava/lang/StringBuilder;",
//						"append(Ljava/lang/StringBuffer;)Ljava/lang/StringBuilder;",
//						"delete(II)Ljava/lang/StringBuilder;",
//						"deleteCharAt(I)Ljava/lang/StringBuilder;",
//						"replace(IILjava/lang/Object;)Ljava/lang/StringBuilder;",
//						"appendCodePoint(I)Ljava/lang/StringBuilder;",
//						"ensureCapacity(I)V",
//						"reverse()Ljava/lang/StringBuilder;",
//						"setCharAt(IC)V",
//						"setLength(I)V",
//						"trimToSize()V",
//						"insert(IZ)Ljava/lang/StringBuilder;",
//						"insert(IC)Ljava/lang/StringBuilder;",
//						"insert(I[C)Ljava/lang/StringBuilder;",
//						"insert(I[CII)Ljava/lang/StringBuilder;",
//						"insert(ILjava/lang/CharSequence;)Ljava/lang/StringBuilder;",
//						"insert(ILjava/lang/CharSequence;II)Ljava/lang/StringBuilder;",
//						"insert(ID)Ljava/lang/StringBuilder;",
//						"insert(IF)Ljava/lang/StringBuilder;",
//						"insert(II)Ljava/lang/StringBuilder;",
//						"insert(IJ)Ljava/lang/StringBuilder;",
//						"insert(ILjava/lang/Object;)Ljava/lang/StringBuilder;",
//						"insert(ILjava/lang/String;)Ljava/lang/StringBuilder;"),
//				// iterator
//				Arrays.asList("forEachRemaining(Ljava/util/function/Consumer;)V",
//						"remove()V"),
//				// listiterator
//				Arrays.asList("add(Ljava/lang/Object;)V",
//						"remove()V",
//						"set(Ljava/lang/Object;)V"),
				//queue
				Arrays.asList(
						new MethodInfo("add(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.ADD, Index.END),
						new MethodInfo("offer(Ljava/lang/Object;)Z", Type.IS_SETTER, Action.ADD, Index.END),
						new MethodInfo("poll()Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.START),
						new MethodInfo("remove()Ljava/lang/Object;", Type.IS_SETTER, Action.REMOVE, Index.START))
				);
		
		List<List<String>> criticalDataStructures = Arrays.asList(
				Arrays.asList("", ""), // linkedlist
				Arrays.asList("elementData", "size"), // arraylist
				Arrays.asList("table", "size"), // hashmap
				Arrays.asList("map.table", "map.size"), // hashset
				Arrays.asList("queue", "size")// queue
				);
		
		for (int i = 0; i < classNames.length; i++) {
			String key = classNames[i];
			classes.add(key);
			classInfoMap.put(key, 
					new ClassInfo(key, writterMethods.get(i), criticalDataStructures.get(i)));
		}
	}
	
	private static ArrayList<String> getRelevantClasses(String method) throws IOException {
		if (classes.isEmpty()) {
			setUp();
		}
		
		String[] methodInfo = method.split("#");
		String className = methodInfo[0];
		
		ArrayList<String> relevantClasses = new ArrayList<>();
		
		/*
		 * load the class
		 */
		ClassGen classGen = loadClass(className);
		if (classGen == null) {
			return relevantClasses;
		}
		
		/*
		 * get all relevant classes
		 */
		relevantClasses.add(className);
		for (String interfaceName : classGen.getInterfaceNames()) {
			relevantClasses.add(interfaceName);
		}
		relevantClasses.add(classGen.getSuperclassName());
		
		return relevantClasses;
	}

//	public static Type getType(String method) throws IOException {
//		String[] methodInfo = method.split("#");
//		String methodSignature = methodInfo[1];
//		
//		ArrayList<String> relevantClasses = getRelevantClasses(method);
//		
//		for (String clazz : relevantClasses) {
//			if (classes.contains(clazz)) {
//				if (classInfoMap.get(clazz).hasMethod(methodSignature)) {
//					return Type.IS_SETTER;
//				}
//			}
//		}
//		return Type.NONE;
//	}
//	
//	public static String getRuntimeType(String method) throws IOException {
//		String[] methodInfo = method.split("#");
//		String methodSignature = methodInfo[1];
//		
//		ArrayList<String> relevantClasses = getRelevantClasses(method);
//		
//		for (String clazz : relevantClasses) {
//			if (classes.contains(clazz)) {
//				if (classInfoMap.get(clazz).hasMethod(methodSignature)) {
//					return clazz;
//				}
//			}
//		}
//		return "";
//	}
	
	public static ClassInfo getClassInfo(String method) {
		if (classes.isEmpty()) {
			setUp();
		}
		String clazz = method.split("#")[0];
		return classInfoMap.get(clazz);
	}
	
	public static MethodInfo getMethodInfo(String method) {
		if (classes.isEmpty()) {
			setUp();
		}
		
		String clazz = method.split("#")[0];
		String methodSignature = method.split("#")[1];
		
		ClassInfo classInfo = getClassInfo(clazz);
		if (classInfo == null) {
			return null;
		}
		
		MethodInfo methodInfo = classInfo.getMethodInfo(methodSignature);
		return methodInfo;
	}
	
	private static String getFileName(String classFName) {
		return classFName + ".class";
	}
	
	private static ClassGen loadClass(String className) throws IOException {
		String classFName = className.replace('.', '/');
		String fileName = getFileName(classFName);
		byte[] bytecode = loadByteCode(fileName);
		
		ClassParser cp = new ClassParser(new java.io.ByteArrayInputStream(bytecode), classFName);
		JavaClass jc = cp.parse();
		return new ClassGen(jc);
	}
	
	private static byte[] loadByteCode(String fileName) throws IOException {
		InputStream inputStream = ClassLoader
        		.getSystemClassLoader()
        		.getResourceAsStream(fileName);
		if (inputStream != null) {
			byte[] bytecode = IOUtils.toByteArray(inputStream);
            inputStream.close();
            return bytecode;
        }
		return null;
	}

}
