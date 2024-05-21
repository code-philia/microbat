package microbat.decisionpredictionLLM;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.LineComment;

import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.debugpilot.userfeedback.DPUserFeedbackType;
import microbat.model.BreakPoint;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
import microbat.model.variable.VirtualVar;

public class PromptConstructor {
	private int condition_result_num;
	
	public String inferPromptNew(DPUserFeedback curStep) {
		String instruction = PromptConst.INFER_INSTR_SIMPLE;
		
    	//=============== Code ==================
		TraceNode node = curStep.getNode();
		BreakPoint breakPoint = node.getBreakPoint();
		int lineNumber = breakPoint.getLineNumber();
		String filePath = breakPoint.getFullJavaFilePath();		
		String contextCode = getContext(filePath,lineNumber,0);
		
		String content = "Function:\n```java\n"+contextCode+"\n```\n";
		return instruction+content;
	}
	
	public String inferencePrompt(DPUserFeedback curStep, Map<String, String> tool_output, boolean canUseTool) {
		String instruction = canUseTool ? PromptConst.INFER_INSTR_COMPLEX : PromptConst.INFER_INSTR_WO_TOOL;
		
    	//=============== Code ==================
		condition_result_num = 0;
		TraceNode node = curStep.getNode();
		BreakPoint breakPoint = node.getBreakPoint();
		int lineNumber = breakPoint.getLineNumber();
		String filePath = breakPoint.getFullJavaFilePath();
		List<VarValue> readVariables = node.getReadVariables();
		
		String contextCode = getContext(filePath,lineNumber,1);
		
		//=============== Variables ==================
		String variableStr = "";
		if(readVariables.isEmpty()) {
			variableStr = "None";
		}
		for(VarValue var : readVariables) {
			variableStr+=(Var2Str(var,readVariables)+"\n");
		}
    	
		//=============== Debugging trace ==================
    	String debuggingTraceStr = "";
    	List<DPUserFeedback> debuggingTrace = new ArrayList<>();
    	for(DPUserFeedback fb = curStep.getParent();fb!=null;fb = fb.getParent()) {
    		debuggingTrace.add(fb);
    	}
    	Collections.reverse(debuggingTrace);
    	for(DPUserFeedback fb : debuggingTrace) {
    		debuggingTraceStr += (getDebuggingTraceDesc(fb)+"\n");
    	}
    	
    	//=============== Gathered information ==================
    	String gatheredInfoStr = "";
    	for(Map.Entry<String, String> entry : tool_output.entrySet()) {
    		gatheredInfoStr += (entry.getKey()+":\n```\n" + entry.getValue()+"\n```\n");
    	}
    	
    	
    	String content = "Function:\n```\n" + contextCode + "\n```\n\n"
    			+"Read variables:\n```\n" + variableStr + "```\n\n"
    			+"Debugging trace:\n```\n" + debuggingTraceStr + "```\n\n"
    			+"Gathered information:\n$$\n"+ gatheredInfoStr +"$$\n";
		
		return instruction + content;
	}
	
	
	public String decisionPrompt(DPUserFeedback curStep, String specification, boolean canUseTool) {
		String instruction = canUseTool? PromptConst.DECISION_INSTR_TOOL : PromptConst.DECISION_INSTR;
		
    	//=============== Code ==================
		condition_result_num = 0;
		TraceNode node = curStep.getNode();
		BreakPoint breakPoint = node.getBreakPoint();
		int lineNumber = breakPoint.getLineNumber();
		String filePath = breakPoint.getFullJavaFilePath();
		List<VarValue> readVariables = node.getReadVariables();
		
		String contextCode = getContext(filePath,lineNumber,1);
		
		//=============== Variables ==================
		String variableStr = "";
		if(readVariables.isEmpty()) {
			variableStr = "None";
		}
		for(VarValue var : readVariables) {
			variableStr+=(Var2Str(var,readVariables)+"\n");
		}
    	
		//=============== Debugging trace ==================
    	String debuggingTraceStr = "";
    	List<DPUserFeedback> debuggingTrace = new ArrayList<>();
    	for(DPUserFeedback fb = curStep.getParent();fb!=null;fb = fb.getParent()) {
    		debuggingTrace.add(fb);
    	}
    	Collections.reverse(debuggingTrace);
    	for(DPUserFeedback fb : debuggingTrace) {
    		debuggingTraceStr += (getDebuggingTraceDesc(fb)+"\n");
    	}
		
    	String content = "Context:\n```\n" + contextCode + "\n```\n\n"
    			+"Read Variables:\n```\n" + variableStr + "```\n\n"
    			+"Specification:\n```\n" + specification.strip() + "\n```\n\n"
    			+"Debugging Trace:\n```\n" + debuggingTraceStr + "```\n\n";
    	
    	return instruction+content;
	}
	
	/*
     * Given line number, get target code
     */
    public String getLineFromFile(String filePath, int lineNumber) {
    	if(lineNumber<=0) {
    		return "";
    	}
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int currentLine = 1;

            while ((line = reader.readLine()) != null && currentLine < lineNumber) {
                currentLine++;
            }
            if (lineNumber == currentLine) {
            	if(line == null) {
            		return "";
            	}
                return line.strip()+" ";
            } else {
                return "";
            }
        } catch(Exception e) {
        	return "";
        }
    }
    
	/*
     * Get context info
     * mode:0. method implement
     *   	1. method implement with "// target code"
     * 	 	2. method name
     */
    public String getContext(String filePath, int lineNumber, int mode) {
    	FileInputStream in = null;
		try {
			in = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        JavaParser javaParser = new JavaParser();
        ParseResult<CompilationUnit> parseResult = javaParser.parse(in);
        Optional<CompilationUnit> optionalCu = parseResult.getResult();
        if (optionalCu.isPresent()) {
            CompilationUnit cu = optionalCu.get();
            for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            	for (MethodDeclaration method : cls.getMethods()) {
                    int startLine = method.getBegin().get().line;
                    int endLine = method.getEnd().get().line;

                    if (lineNumber >= startLine && lineNumber <= endLine) {
                        if (mode == 0) {
                            return method.toString();
                        } else if (mode == 1) {
                        	return getCodeBetween(filePath, startLine, endLine, lineNumber);
                        } else if (mode == 2) {
                            return cls.getNameAsString()+"#"+method.getNameAsString()+"()";
                        }
                    }
                }
            	for (ConstructorDeclaration method : cls.getConstructors()) {
                    int startLine = method.getBegin().get().line;
                    int endLine = method.getEnd().get().line;

                    if (lineNumber >= startLine && lineNumber <= endLine) {
                        if (mode == 0) {
                            return method.toString();
                        } else if (mode == 1) {
                        	return getCodeBetween(filePath, startLine, endLine, lineNumber);
                        } else if (mode == 2) {
                            return cls.getNameAsString()+"#"+cls.getNameAsString()+"()";
                        }
                    }
                }
                for (FieldDeclaration field : cls.getFields()) {
                    int startLine = field.getBegin().get().line;
                    int endLine = field.getEnd().get().line;

                    if (lineNumber >= startLine && lineNumber <= endLine) {
                        if (mode == 0) {
                            return field.toString();
                        } else if (mode == 1) {
                            return getCodeBetween(filePath, startLine, endLine, lineNumber);
                        } else if (mode == 2) {
                            return cls.getNameAsString();
                        }
                    }
                }
            }
        }
		return "";
    }
    
    public static String getCodeBetween(String file, int startLine, int endLine, int lineNumber){
    	BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			return "";
		}
        String line;
        List<String> lines = new ArrayList<>();
        int currentLine = 1;
        try {
			while ((line = reader.readLine()) != null && currentLine<=endLine) {
			    if (currentLine >= startLine) {
			    	if(currentLine == lineNumber) {
			    		lines.add(line+" // current step");
			    	}
			    	else {
			    		lines.add(line);
			    	}
			    }
			    currentLine++;
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        return String.join("\n",lines);
    }
    
    /*
     * Get variable name
     */
    public String parseVarName(Variable variable, List<VarValue> readVarValues) {
    	String varName = variable.getName();
    	if(variable instanceof VirtualVar) {
    		String result = "return_from_";
    		String[] parts = varName.split("#");

    		String className = parts[0];
    		result+=(getClassName(className)+".");

    		String methodName = parts[1].split( "\\(" )[0];
    		result+=methodName;
    		return result;
    	}
    	//ConditionResult_1
    	else if(varName.contains("ConditionResult_")) {
    		String result =  "condition_result_"+String.valueOf(condition_result_num);
    		condition_result_num+=1;
    		return result;
    	}
    	else if(varName.contains("[")) {
    		int index = varName.indexOf("[");
    		String address = varName.substring(0,index);
    		varName = varName.substring(index);
    		
    		for(VarValue v : readVarValues) {
    			String oriValue = v.getStringValue();
    			if(oriValue.contains("@")) {
    				String afterAt = oriValue.substring(oriValue.indexOf("@")+1);
    				if(Long.parseLong(afterAt,16) == Long.parseLong(address,10)) {
    					varName = (v.getVarName()+varName);
    					break;
    				}
    			}
    		}
    		
    	}
    	return varName;
    }
    
    /*
     * Get simplified class name
     */
    public String getClassName(String classFullName) {
    	if(!classFullName.contains(".")) {
    		return classFullName;
    	}
    	String[] parts = classFullName.split("\\.");
    	return parts[parts.length-1];
    }
    
    /*
     * Get value string
     */
    public String getObjValue(VarValue var_value) {
    	Variable variable = var_value.getVariable();
    	String type = variable.getType();
    	// basic type
    	if("byte".equals(type)||"short".equals(type)||"int".equals(type)||"long".equals(type)
    			||"float".equals(type)||"double".equals(type)||"char".equals(type)||"boolean".equals(type)) {
    		return var_value.getStringValue();
    	}
    	// java Array
    	else if(var_value.isArray()) {
    		List<VarValue> children = var_value.getChildren();
    		int len = Math.min(children.size(), 10);
        	String valueString = "[";
        	for(int i = 0;i<len;i++) {
        		VarValue child = children.get(i);
        		valueString += getObjValue(child);
        		if(i<len-1) {
        			valueString+=",";
        		}
        	}
        	if(len < children.size()) {
        		valueString += "...]";
        	}
        	else {
        		valueString += "]";
        	}
        	return valueString;    	
        }
    	// has toString method
    	else if(var_value.isDefinedToStringMethod()) {
    		return var_value.getStringValue();
    	}
    	// object with no toString method, record its fields
    	else { 
        	List<VarValue> children = var_value.getChildren();
        	int len = Math.min(children.size(),10);
        	String valueString = "{";
        	for(int i = 0;i<len;i++) {
        		VarValue child = children.get(i);
        		valueString+=(child.getVariable().getName()+":"+getObjValue(child));
        		if(i<len-1) {
        			valueString+=",";
        		}
        	}
        	if(len < children.size()) {
        		valueString += "...}";
        	}
        	else {
        		valueString += "}";
        	}
        	return valueString;
    	}
    }
    
    /*
     * Given a var_value, get {type: ,name: ,value: }
     */
    public String Var2Str(VarValue var, List<VarValue> readVariables) {
		Variable variable = var.getVariable();
		String variableStr = "{type:"+getClassName(variable.getType())+",name:"+parseVarName(variable,readVariables)+",value:"+getObjValue(var)+"}";
		return variableStr;
    }
    
    /*
     * Given a certain step, get the debugging trace element string
     */
    public String getDebuggingTraceDesc(DPUserFeedback step) {
		condition_result_num = 0;
		TraceNode node = step.getNode();
		BreakPoint breakPoint = node.getBreakPoint();
		int lineNumber = breakPoint.getLineNumber();
		String filePath = breakPoint.getFullJavaFilePath();
		List<VarValue> readVariables = node.getReadVariables();
		
		String targetCode = "";
		targetCode = getLineFromFile(filePath,lineNumber);
		
    	String str = " - step \""+targetCode+"\" in "+getContext(filePath,lineNumber,2)+" is of \""+ step.getTypeStr4LLM()+"\" type";
//    	String str = " - step \""+targetCode+"\" in "+getContext(filePath,lineNumber,2)+ step.getTypeStr4LLM();
		String varStr = "";
    	for(VarValue v : step.getWrongVars()) {
    		varStr+=(",variable "+Var2Str(v,readVariables)+" is wrong");
    	}
    	for(VarValue v : step.getCorrectVars()) {
    		varStr+=(",variable "+Var2Str(v,readVariables)+" is correct");
    	}
    	
    	if(step.getType()==DPUserFeedbackType.WRONG_VARIABLE) {
    		return str+varStr+". Look into its data dependency.";
    	}
    	else if(step.getType()==DPUserFeedbackType.WRONG_PATH) {
    		return str+varStr+". Look into its control dependency.";
    	}
		return str+varStr;
    }
}
