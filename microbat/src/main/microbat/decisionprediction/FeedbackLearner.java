package microbat.decisionprediction;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.debugpilot.userfeedback.DPUserFeedbackType;
import microbat.model.BreakPoint;
import microbat.model.trace.TraceNode;
import microbat.model.value.VarValue;
import microbat.model.variable.Variable;
import microbat.model.variable.VirtualVar;

import org.json.*;

public class FeedbackLearner {
	private int condition_result_num;
	
	public FeedbackLearner() {
		condition_result_num = 0;
	}
	
	public HttpURLConnection setupConnection() {
		HttpURLConnection connection = null;
        try {
			URL url = new URL("http://127.0.0.1:5001/learn");
	        connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
	        connection.setDoOutput(true);
	        connection.setRequestProperty("Content-Type", "application/json; utf-8");
		} catch (MalformedURLException e) {
			System.out.println("--ERROR-- MalformedURLException in connection setup...");
			e.printStackTrace();
		} catch (ProtocolException e) {
			System.out.println("--ERROR-- ProtocolException in connection setup...");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("--ERROR-- IOException in connection setup...");
			e.printStackTrace();
		}
        return connection;
	}
	
	public void learnFeedback(DPUserFeedback curStep) throws IOException {
		// 1.prepare model input
		String target_seq = formTargetSeq(curStep);
		System.out.println("--INFO-- targetSeq: "+target_seq);
		System.out.println("--INFO-- learning...");
		
		// 2.setup connection
		HttpURLConnection connection = setupConnection();
		
		// 3.send request
		String input = "{\"target_seq\" : \""+target_seq+"\"}";
        OutputStream outputStream;
		outputStream = connection.getOutputStream();
        outputStream.write(input.getBytes());
        outputStream.flush();
        
        BufferedReader bufferReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String output = bufferReader.readLine();
        JSONObject responseJson = new JSONObject(output);
        
		connection.disconnect();

        System.out.println("--INFO-- Train done.");
	}
	
	// prepare model input
	public String formTargetSeq(DPUserFeedback curStep) {
		condition_result_num = 0;
		
		TraceNode node = curStep.getNode();
		BreakPoint breakPoint = node.getBreakPoint();
		
		int lineNumber = breakPoint.getLineNumber();
		String filePath = breakPoint.getFullJavaFilePath();
		String doc = getClassFunName(filePath,lineNumber);
		String classFuncName = null;
		String comment = null;
		if(doc.contains(":")) {
			int idx = doc.indexOf(":");
			classFuncName = doc.substring(0, idx);
			comment = doc.substring(idx+1);
		}
		else {
			classFuncName = doc;
			comment = "";
		}
		
		List<VarValue> readVariables = node.getReadVariables();
		
		String target_seq = null;
		try {
			// 1.1 context
			target_seq = "<func>"+classFuncName+"</func><code_window><before>"
					+getLineFromFile(filePath,lineNumber-2)+getLineFromFile(filePath,lineNumber-1)
					+curStep.getTypeStr()+getLineFromFile(filePath,lineNumber)
					+"<after>"+getLineFromFile(filePath,lineNumber+1)+getLineFromFile(filePath,lineNumber+2)+"</code_window>"
					+"<comment>"+comment+"</comment>";
			
			
			// 1.2 variables
			List<String> var_str_list = new ArrayList<String>();
			for(VarValue var_value : readVariables) {
				String label = null;
				if(curStep.getWrongVars().contains(var_value)) {
					label = "<FALSE>";
				}
				else {
					label = "<TRUE>";
				}
				
				Variable variable = var_value.getVariable();
				String var_str = label+getClassName(variable.getType())
					+" "+parseVarName(variable,readVariables)
					+" = "+getObjValue(var_value);
				var_str_list.add(var_str);
			}
			target_seq+="<variables>"+String.join("</s>", var_str_list)+"</variables>";
			
			// 1.3 observed
			DPUserFeedback lastStep = curStep.getParent();
			if(lastStep != null) {
				TraceNode lastNode = lastStep.getNode();
				BreakPoint lastBreakPoint = lastNode.getBreakPoint();
				
				int lastLineNumber = lastBreakPoint.getLineNumber();
				String lastFilePath = lastBreakPoint.getFullJavaFilePath();
//				String lastDoc = getClassFunName(filePath,lineNumber);
				
				List<String> wrong_var_list = new ArrayList<String>();
				if( lastStep.getType() == DPUserFeedbackType.WRONG_VARIABLE) {
					for(VarValue var_value : lastStep.getWrongVars()) {
						Variable variable = var_value.getVariable();
						String wrong_var_str = getClassName(variable.getType())
								+" "+parseVarName(variable,lastNode.getReadVariables())
								+" = "+getObjValue(var_value)+" (wrong)";
						wrong_var_list.add(wrong_var_str);
					}
				}
				//1
				target_seq += ("<observed><last_step>"+getLineFromFile(lastFilePath,lastLineNumber)
					+"<type>"+lastStep.getTypeStr()+":"+lastStep.getTypeDesc()
					+"<wrong_variable>"+String.join("</s>",wrong_var_list)+"</observed>");
			}
			else {
				target_seq += "<observed></observed>";
			}
		} catch (IOException e) {
			System.out.println("--ERROR-- IOException in formSourceSeq...");
			e.printStackTrace();
		}
		return target_seq;
	}
	
	// given line number, get target code
    public String getLineFromFile(String filePath, int lineNumber) throws IOException {
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
        }
    }
    
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
    	//ConditionResult_36916
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

    // given full name, get class name
    public String getClassName(String classFullName) {
    	if(!classFullName.contains(".")) {
    		return classFullName;
    	}
    	String[] parts = classFullName.split("\\.");
    	return parts[parts.length-1];
    }
    
    // given varValue, get string value
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
    // get class, function name and comment
    public String getClassFunName(String filename, int lineNumber) {
    	FileInputStream in = null;
		try {
			in = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        JavaParser javaParser = new JavaParser();
        ParseResult<CompilationUnit> parseResult = javaParser.parse(in);
        Optional<CompilationUnit> optionalCu = parseResult.getResult();

        String className = "";
        String result = "";
        if (optionalCu.isPresent()) {
            CompilationUnit cu = optionalCu.get();
            for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                className = cls.getNameAsString();
                for (MethodDeclaration method : cls.findAll(MethodDeclaration.class)) {
                    int startLine = method.getBegin().get().line;
                    int endLine = method.getEnd().get().line;

                    if (lineNumber >= startLine && lineNumber <= endLine) {
                        result = className+"#"+method.getNameAsString();
                        
                        Optional<String> comment = method.getComment().map(c->c.getContent());
                        if(comment.isPresent()) {
                        	String commentStr = comment.get();
                        	commentStr = commentStr.replace("\n","\\n");
                        	commentStr = commentStr.replace("\r\n","\\n");
                        	result+=(":"+commentStr);                        
                        }
                    }
                }
            }
        }
        return result;
    }
    
    String parseFlag(VarValue var_value) {
    	String result = "";
    	if(var_value.isField()) {
    		result+="isField ";
    	}
    	if(var_value.isLocalVariable()) {
    		result+="isLocal";
    	}
    	if(var_value.isStatic()) {
    		result+="isStatic";
    	}
    	return result.strip();
    }
}
