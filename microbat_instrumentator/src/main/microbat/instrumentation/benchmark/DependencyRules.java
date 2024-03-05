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

public class DependencyRules {
	
	static public Set<String> classes = new HashSet<>();
	static public Map<String, Set<String>> writters = new HashMap<>();
	
	static public enum Type {
		NONE,
		IS_SETTER,
		IS_GETTER
	}

	public DependencyRules() {}
	
	public static void setUp() {
		String[] classNames = new String[] {
				"java.util.List",
				"java.util.LinkedList",
				"java.util.ArrayList",
				
				"java.util.Map",
				"java.util.HashMap",
				
				"java.util.Set",
				"java.util.HashSet",
				
				"java.util.Collection",
				
				"java.lang.StringBuffer",
				"java.io.Writer",
				"java.io.StringWriter",
				"java.io.PrintWriter",
				"java.lang.StringBuilder",
				
				"java.util.Iterator",
				"java.lang.Iterable",
				"java.util.ListIterator",
				
				"java.util.Queue"
				};
		
		List<List<String>> writterMethods = Arrays.asList(
				// list
				Arrays.asList("add(Ljava/lang/Object;)Z",
						"add(ILjava/lang/Object;)V",
						"addAll(Ljava/util/Collection;)Z",
						"addAll(ILjava/util/Collection;)Z",
						"clear()V",
						"remove(I)Ljava/lang/Object;",
						"remove(Ljava/lang/Object;)Z",
						"removeAll(Ljava/util/Collection;)Z",
						"replaceAll(Ljava/util/function/UnaryOperator;)V",
						"retainAll(Ljava/util/Collection;)Z",
						"set(ILjava/lang/Object;)Ljava/lang/Object;",
						"sort(Ljava/util/Comparator;)V"),
				// linkedlist
				Arrays.asList("add(Ljava/lang/Object;)Z",
						"add(ILjava/lang/Object;)V",
						"addAll(Ljava/util/Collection;)Z",
						"addAll(ILjava/util/Collection;)Z",
						"addFirst(Ljava/lang/Object;)V",
						"addLast(Ljava/lang/Object;)V",
						"clear()V",
						"offer(Ljava/lang/Object;)Z",
						"offerFirst(Ljava/lang/Object;)Z",
						"offerLast(Ljava/lang/Object;)Z",
						"poll()Ljava/lang/Object;",
						"pollFirst()Ljava/lang/Object;",
						"pollLast()Ljava/lang/Object;",
						"pop()Ljava/lang/Object;",
						"push(Ljava/lang/Object;)V",
						"remove()Ljava/lang/Object;",
						"remove(I)Ljava/lang/Object;",
						"remove(Ljava/lang/Object;)Z",
						"removeFirst()Ljava/lang/Object;",
						"removeFirstOccurrence(Ljava/lang/Object;)Z",
						"removeLast()Ljava/lang/Object;",
						"removeLastOccurrence(Ljava/lang/Object;)Z",
						"set(ILjava/lang/Object;)Ljava/lang/Object;"),
				// arraylist
				Arrays.asList("add(Ljava/lang/Object;)Z",
						"add(ILjava/lang/Object;)V",
						"addAll(Ljava/util/Collection;)Z",
						"addAll(ILjava/util/Collection;)Z",
						"clear()V",
						"ensureCapacity(I)V",
						"forEach(Ljava/util/function/Consumer;)V",
						"remove(Ljava/lang/Object;)Z",
						"remove(I)Ljava/lang/Object;",
						"removeAll(Ljava/util/Collection;)Z",
						"removeIf(Ljava/util/function/Predicate;)Z",
						"removeRange(II)V",
						"replaceAll(Ljava/util/function/UnaryOperator;)V",
						"retainAll(Ljava/util/Collection;)Z",
						"set(ILjava/lang/Object;)Ljava/lang/Object;",
						"sort(Ljava/util/Comparator;)V",
						"trimToSize()V"),
				// map
				Arrays.asList("clear()V",
						"forEach(Ljava/util/function/BiConsumer;)V",
						"merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
						"put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
						"putAll(Ljava/util/Map;)V",
						"putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
						"remove(Ljava/lang/Object;)Ljava/lang/Object;",
						"remove(Ljava/lang/Object;Ljava/lang/Object;)Z",
						"replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
						"replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",
						"replaceAll(Ljava/util/function/BiFunction;)V"),
				// hashmap
				Arrays.asList("clear()V",
						"forEach(Ljava/util/function/BiConsumer;)V",
						"merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
						"put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
						"putAll(Ljava/util/Map;)V",
						"putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
						"remove(Ljava/lang/Object;)Ljava/lang/Object;",
						"remove(Ljava/lang/Object;Ljava/lang/Object;)Z",
						"replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
						"replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",
						"replaceAll(Ljava/util/function/BiFunction;)V"),
				// set
				Arrays.asList("add(Ljava/lang/Object;)Z",
						"addAll(Ljava/util/Collection;)Z",
						"clear()V",
						"remove(Ljava/lang/Object;)Z",
						"removeAll(Ljava/util/Collection;)Z",
						"retainAll(Ljava/util/Collection;)Z"),
				// hashset
				Arrays.asList("add(Ljava/lang/Object;)Z",
						"clear()V",
						"remove(Ljava/lang/Object;)Z"),
				// collection
				Arrays.asList("add(Ljava/lang/Object;)Z",
						"addAll(Ljava/util/Collection;)Z",
						"clear()V",
						"remove(Ljava/lang/Object;)Z",
						"removeAll(Ljava/util/Collection;)Z",
						"removeIf(Ljava/util/function/Predicate;)Z",
						"retainAll(Ljava/util/Collection;)Z"),
				// stringbuffer
				Arrays.asList("append(Z)Ljava/lang/StringBuffer;",
						"append(C)Ljava/lang/StringBuffer;",
						"append([C)Ljava/lang/StringBuffer;",
						"append([CII)Ljava/lang/StringBuffer;",
						"append(Ljava/lang/CharSequence;)Ljava/lang/StringBuffer;",
						"append(Ljava/lang/CharSequence;II)Ljava/lang/StringBuffer;",
						"append(D)Ljava/lang/StringBuffer;",
						"append(F)Ljava/lang/StringBuffer;",
						"append(I)Ljava/lang/StringBuffer;",
						"append(J)Ljava/lang/StringBuffer;",
						"append(Ljava/lang/Object;)Ljava/lang/StringBuffer;",
						"append(Ljava/lang/String;)Ljava/lang/StringBuffer;",
						"append(Ljava/lang/StringBuffer;)Ljava/lang/StringBuffer;",
						"appendCodePoint(I)Ljava/lang/StringBuffer;",
						"delete(II)Ljava/lang/StringBuffer;",
						"deleteCharAt(I)Ljava/lang/StringBuffer;",
						"ensureCapacity(I)V",
						"insert(IZ)Ljava/lang/StringBuffer;",
						"insert(IC)Ljava/lang/StringBuffer;",
						"insert(I[C)Ljava/lang/StringBuffer;",
						"insert(I[CII)Ljava/lang/StringBuffer;",
						"insert(ILjava/lang/CharSequence;)Ljava/lang/StringBuffer;",
						"insert(ILjava/lang/CharSequence;II)Ljava/lang/StringBuffer;",
						"insert(ID)Ljava/lang/StringBuffer;",
						"insert(IF)Ljava/lang/StringBuffer;",
						"insert(II)Ljava/lang/StringBuffer;",
						"insert(IJ)Ljava/lang/StringBuffer;",
						"insert(ILjava/lang/Object;)Ljava/lang/StringBuffer;",
						"insert(ILjava/lang/String;)Ljava/lang/StringBuffer;",
						"replace(IILjava/lang/String;)Ljava/lang/StringBuffer;",
						"reverse()Ljava/lang/StringBuffer;",
						"setCharAt(IC)V",
						"setLength(I)V",
						"trimToSize()V"),
				// writer
				Arrays.asList("write([C)V",
						"write([CII)V",
						"write(I)V",
						"write(Ljava/lang/String;)V",
						"write(Ljava/lang/String;II)V",
						"append(C)Ljava/io/Writer;",
						"append(Ljava/lang/CharSequence;)Ljava/io/Writer;",
						"append(Ljava/lang/CharSequence;II)Ljava/io/Writer;",
						"close()V",
						"flush()V"),
				// stringwriter
				Arrays.asList("append(C)Ljava/io/StringWriter;",
						"append(Ljava/lang/CharSequence;)Ljava/io/StringWriter;",
						"append(Ljava/lang/CharSequence;II)Ljava/io/StringWriter;",
						"close()V",
						"flush()V",
						"write([CII)V",
						"write(I)V",
						"write(Ljava/lang/String;)V",
						"write(Ljava/lang/String;II)V"),
				// printwriter
				Arrays.asList("append(C)Ljava/io/PrintWriter;",
						"append(Ljava/lang/CharSequence;)Ljava/io/PrintWriter;",
						"append(Ljava/lang/CharSequence;II)Ljava/io/PrintWriter;",
						"checkError()Z",
						"clearError()V",
						"close()V",
						"flush()V",
						"format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintWriter;",
						"format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/io/PrintWriter;",
						"setError()V",
						"write([C)V",
						"write([CII)V",
						"write(I)V",
						"write(Ljava/lang/String;)V",
						"write(Ljava/lang/String;II)V"),
				// stringbuilder
				Arrays.asList("append(Z)Ljava/lang/StringBuilder;",
						"append(C)Ljava/lang/StringBuilder;",
						"append([C)Ljava/lang/StringBuilder;",
						"append([CII)Ljava/lang/StringBuilder;",
						"append(Ljava/lang/CharSequence;)Ljava/lang/StringBuilder;",
						"append(Ljava/lang/CharSequence;II)Ljava/lang/StringBuilder;",
						"append(D)Ljava/lang/StringBuilder;",
						"append(F)Ljava/lang/StringBuilder;",
						"append(I)Ljava/lang/StringBuilder;",
						"append(J)Ljava/lang/StringBuilder;",
						"append(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
						"append(Ljava/lang/String;)Ljava/lang/StringBuilder;",
						"append(Ljava/lang/StringBuffer;)Ljava/lang/StringBuilder;",
						"delete(II)Ljava/lang/StringBuilder;",
						"deleteCharAt(I)Ljava/lang/StringBuilder;",
						"replace(IILjava/lang/Object;)Ljava/lang/StringBuilder;",
						"appendCodePoint(I)Ljava/lang/StringBuilder;",
						"ensureCapacity(I)V",
						"reverse()Ljava/lang/StringBuilder;",
						"setCharAt(IC)V",
						"setLength(I)V",
						"trimToSize()V",
						"insert(IZ)Ljava/lang/StringBuilder;",
						"insert(IC)Ljava/lang/StringBuilder;",
						"insert(I[C)Ljava/lang/StringBuilder;",
						"insert(I[CII)Ljava/lang/StringBuilder;",
						"insert(ILjava/lang/CharSequence;)Ljava/lang/StringBuilder;",
						"insert(ILjava/lang/CharSequence;II)Ljava/lang/StringBuilder;",
						"insert(ID)Ljava/lang/StringBuilder;",
						"insert(IF)Ljava/lang/StringBuilder;",
						"insert(II)Ljava/lang/StringBuilder;",
						"insert(IJ)Ljava/lang/StringBuilder;",
						"insert(ILjava/lang/Object;)Ljava/lang/StringBuilder;",
						"insert(ILjava/lang/String;)Ljava/lang/StringBuilder;"),
				// iterator
				Arrays.asList("forEachRemaining(Ljava/util/function/Consumer;)V",
						"remove()V"),
				// iterable
				Arrays.asList("forEach(Ljava/util/function/Consumer;)V"),
				// listiterator
				Arrays.asList("add(Ljava/lang/Object;)V",
						"remove()V",
						"set(Ljava/lang/Object;)V"),
				//queue
				Arrays.asList("add(Ljava/lang/Object;)Z",
						"offer(Ljava/lang/Object;)Z",
						"poll()Ljava/lang/Object;",
						"remove()Ljava/lang/Object;")
				);
		
		for (int i = 0; i < classNames.length; i++) {
			String key = classNames[i];
			classes.add(key);
			writters.put(key, new HashSet<String>(writterMethods.get(i)));
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

	public static Type getType(String method) throws IOException {
		String[] methodInfo = method.split("#");
		String methodSignature = methodInfo[1];
		
		ArrayList<String> relevantClasses = getRelevantClasses(method);
		
		for (String clazz : relevantClasses) {
			if (classes.contains(clazz)) {
				if (writters.get(clazz).contains(methodSignature)) {
					return Type.IS_SETTER;
				}
			}
		}
		return Type.NONE;
	}
	
	public static String getRuntimeType(String method) throws IOException {
		String[] methodInfo = method.split("#");
		String methodSignature = methodInfo[1];
		
		ArrayList<String> relevantClasses = getRelevantClasses(method);
		
		for (String clazz : relevantClasses) {
			if (classes.contains(clazz)) {
				if (writters.get(clazz).contains(methodSignature)) {
					return clazz;
				}
			}
		}
		return "";
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
