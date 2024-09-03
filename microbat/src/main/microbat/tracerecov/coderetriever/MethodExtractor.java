package microbat.tracerecov.coderetriever;

import java.util.Optional;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import sav.strategies.dto.AppJavaClassPath;

/**
 * This class is used to extract the source code of a method given the source
 * code of the class.
 */
public class MethodExtractor {

	public MethodExtractor() {
	}

	public String getConstructorCode(AppJavaClassPath appJavaClassPath, String sourceCode, String className,
			String... paramTypes) throws CodeRetrieverException {
		// initialize TypeSolver to resolve generic types
		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ReflectionTypeSolver());
		typeSolver.add(new JavaParserTypeSolver(appJavaClassPath.getSoureCodePath()));

		// configure Parser with Symbol Resolver
		JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
		ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver(symbolSolver);
		StaticJavaParser.setConfiguration(parserConfiguration);

		ClassOrInterfaceDeclaration classOrInterfaceDeclaration = getClass(sourceCode, className);

		Optional<ConstructorDeclaration> constructor = classOrInterfaceDeclaration.findAll(ConstructorDeclaration.class)
				.stream().filter(c -> parametersMatch(c, paramTypes)).findAny();

		if (!constructor.isPresent()) {
			StringBuilder message = new StringBuilder("Constructor ");
			if (paramTypes.length > 0) {
				message.append("\nwith input parameters: ");
				for (String type : paramTypes) {
					message.append(type);
					message.append(";");
				}
				message.append("\n");
			}
			message.append("is not found in the given source code.");
			throw new CodeRetrieverException(message.toString());
		}

		return constructor.map(ConstructorDeclaration::toString).orElse(null);
	}

	public String getMethodCode(AppJavaClassPath appJavaClassPath, String sourceCode, String className,
			String returnType, String methodName, String... paramTypes) throws CodeRetrieverException {
		// initialize TypeSolver to resolve generic types
		CombinedTypeSolver typeSolver = new CombinedTypeSolver();
		typeSolver.add(new ReflectionTypeSolver());
		typeSolver.add(new JavaParserTypeSolver(appJavaClassPath.getSoureCodePath()));

		// configure Parser with Symbol Resolver
		JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
		ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver(symbolSolver);
		StaticJavaParser.setConfiguration(parserConfiguration);

		ClassOrInterfaceDeclaration classOrInterfaceDeclaration = getClass(sourceCode, className);

		Optional<MethodDeclaration> method = classOrInterfaceDeclaration.getMethodsByName(methodName).stream()
				.filter(m -> returnTypeMatches(m, returnType) && parametersMatch(m, paramTypes)).findFirst();

		if (!method.isPresent()) {
			StringBuilder message = new StringBuilder("Method ");
			message.append(methodName);
			if (paramTypes.length > 0) {
				message.append("\nwith input parameters: ");
				for (String type : paramTypes) {
					message.append(type);
					message.append(";");
				}
			}
			message.append("\nwith return type: ");
			message.append(returnType);
			message.append("\nis not found in the given source code.");
			throw new CodeRetrieverException(message.toString());
		}

		return method.map(MethodDeclaration::toString).orElse(null);
	}

	private ClassOrInterfaceDeclaration getClass(String sourceCode, String className) throws CodeRetrieverException {
		CompilationUnit compilationUnit = null;
		try {
			StaticJavaParser.getParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_14);
			compilationUnit = StaticJavaParser.parse(sourceCode);
		} catch (ParseProblemException e) {
			e.printStackTrace();
			throw new CodeRetrieverException("Class " + className + " cannot be parsed.");
		}

		Optional<ClassOrInterfaceDeclaration> classDeclaration = compilationUnit
				.findFirst(ClassOrInterfaceDeclaration.class, c -> c.getNameAsString().equals(className));

		if (!classDeclaration.isPresent()) {
			throw new CodeRetrieverException("Class " + className + " is not found in the given source code.");
		}

		return classDeclaration.get();
	}

	private boolean returnTypeMatches(MethodDeclaration method, String returnType) {
		String actualReturnType = method.getType().resolve().describe();
		return actualReturnType.equals(returnType);
	}

	private boolean parametersMatch(CallableDeclaration method, String[] paramTypes) {
		if (method.getParameters().size() != paramTypes.length) {
			return false;
		}

		for (int i = 0; i < paramTypes.length; i++) {
			String actualParamType = method.getParameter(i).getType().resolve().describe();
			if (actualParamType.matches("[A-Z]") && paramTypes[i].equals("java.lang.Object")) {
				continue;
			}
			if (!actualParamType.equals(paramTypes[i])) {
				return false;
			}
		}
		return true;
	}

}