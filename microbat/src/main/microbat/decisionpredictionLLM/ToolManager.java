package microbat.decisionpredictionLLM;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.utils.SourceRoot;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import microbat.model.trace.TraceNode;

public class ToolManager {
	private TraceNode currentNode;
	
	public String parseAndInvoke(String invocationJsonStr) {
		// 1. parse
		JSONObject invocationJson = JSONUtil.parseObj(invocationJsonStr);
		String toolName = invocationJson.getStr("tool");
		JSONArray argsJson = invocationJson.getJSONArray("args");
		List<String> args = argsJson.toList(String.class);
		
		// 2. invoke
		String output = invokeTool(toolName, args);
		
		return output;
	}
	
	public String invokeTool(String toolName, List<String> args) {
//		if("get_class".equals(toolName)) {
//			assert(args.size()==1);
//			return getClassTool(args.get(0));
//		}
		
		return "";
	}
	
	public void setNode(TraceNode node) {
		currentNode = node;
	}
	
	public String getClassTool(String className) {
		// 1. find project path
		String projectDir = currentNode.getTrace().getAppJavaClassPath().getWorkingDirectory();
		String currentFile = currentNode.getBreakPoint().getFullJavaFilePath();
		
		// 2. find full class name
    	FileInputStream in = null;
		try {
			in = new FileInputStream(currentFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
        JavaParser javaParser = new JavaParser();
        ParseResult<CompilationUnit> parseResult = javaParser.parse(in);
        Optional<CompilationUnit> optionalCu = parseResult.getResult();
        CompilationUnit cu = optionalCu.get();
        
        String fullClassName = null;
        for (ImportDeclaration importDeclaration : cu.getImports()) {
            if (importDeclaration.getNameAsString().endsWith("." + className)) {
            	fullClassName = importDeclaration.getNameAsString();
            }
        }
		
		// 3. find class definition in project
        ParserConfiguration config = new ParserConfiguration();
        SourceRoot sourceRoot = new SourceRoot(Paths.get(projectDir), config);

        try {
			for (ParseResult<CompilationUnit> result : sourceRoot.tryToParse("")) {
			    if (result.isSuccessful()) {
			        CompilationUnit cu1 = result.getResult().get();
			        for(ClassOrInterfaceDeclaration clazz : cu1.findAll(ClassOrInterfaceDeclaration.class)){
			        	if(clazz.getFullyQualifiedName().get().equals(fullClassName)) {
			        		String output = clazz.getNameAsString()+":\n";
		                    for (FieldDeclaration field : clazz.getFields()) {
		                    	output += (field.getVariables()+"\n");
		                    }
			        		return output;
			        	}
			        }
			    }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
}
