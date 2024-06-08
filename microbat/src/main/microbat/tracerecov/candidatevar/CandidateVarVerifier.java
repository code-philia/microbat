package microbat.tracerecov.candidatevar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;

import microbat.tracerecov.TraceRecovUtils;

/**
 * This class performs static analysis and verifies whether a candidate variable
 * is guaranteed to be written or not written by a method call.
 * 
 * @author hongshuwang
 */
public class CandidateVarVerifier {

	public enum WriteStatus {
		GUARANTEE_WRITE, GUARANTEE_NO_WRITE, NO_GUARANTEE
	}

	private String className;
	private ClassReader classReader;

	public CandidateVarVerifier(String className) throws CandidateVarVerificationException {
		if (!TraceRecovUtils.shouldBeChecked(className)) {
			throw new CandidateVarVerificationException("class:" + className + " shouldn't be checked");
		}
		this.className = className;
		this.classReader = this.loadClass(className);
	}

	private ClassReader loadClass(String className) throws CandidateVarVerificationException {
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(className.replace('.', '/') + ".class");
		try {
			return new ClassReader(inputStream);
		} catch (IOException e) {
			throw new CandidateVarVerificationException(className.replace('.', '/') + ".class is not found");
		}
	}

	public WriteStatus verifyCandidateVariable(String invokingMethod, String fieldName)
			throws CandidateVarVerificationException {
		// invalid invoking method
		if (invokingMethod == null || invokingMethod.startsWith("%")) {
			throw new CandidateVarVerificationException("invalid invoking method: " + invokingMethod);
		}

		String declaredType = invokingMethod.split("#")[0];

		// checking declaredType and runtimeType
		if (isInvokingTypeValid(declaredType)) {
			return WriteStatus.NO_GUARANTEE;
		}

		String methodSignature = invokingMethod.split("#")[1].split("%")[0];
		int index = methodSignature.indexOf("(");
		String methodName = methodSignature.substring(0, index);
		String methodDescriptor = methodSignature.substring(index);

		return verifyCandidateVariable(declaredType, methodName, methodDescriptor, methodSignature, fieldName);
	}

	private boolean isInvokingTypeValid(String declaredType) {
		try {
			Class<?> runtimeClass = Class.forName(className);
			Class<?> declaredClass = Class.forName(declaredType);

			Set<Class<?>> visitedClasses = new HashSet<>();
			visitedClasses.add(runtimeClass);
			
			List<Class<?>> classesToCheck = new ArrayList<>();
			classesToCheck.add(runtimeClass);
			
			while (!classesToCheck.isEmpty()) {
				Class<?> currentClass = classesToCheck.remove(0);
				if (declaredClass == currentClass) {
					return true;
				}
				
				if (currentClass == null) {
					continue;
				}
				
				if (!visitedClasses.contains(currentClass.getSuperclass())) {
					visitedClasses.add(currentClass.getSuperclass());
					classesToCheck.add(currentClass.getSuperclass());
				}
				Class<?>[] interfaces = currentClass.getInterfaces();
				for (Class<?> i : interfaces) {
					if (!visitedClasses.contains(i)) {
						visitedClasses.add(i);
						classesToCheck.add(i);
					}
				}
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	private WriteStatus verifyCandidateVariable(String declaredType, String methodName, String methodDescriptor,
			String methodSignature, String fieldName) {
		// create and accept a classVisitor
		CandidateVarClassVisitor classVisitor = new CandidateVarClassVisitor(className, methodName, methodDescriptor,
				fieldName);
		classReader.accept(classVisitor, 0);

		return CandidateVarMethodVisitor.getWriteStatus();
	}

}
