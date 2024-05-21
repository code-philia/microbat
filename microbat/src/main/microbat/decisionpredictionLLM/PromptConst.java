package microbat.decisionpredictionLLM;

public final class PromptConst {
	// ============= Using =============
	//Also, don't point out the bug.
	public static final String INFER_INSTR_SIMPLE = """
			You are an assistant skilled in understanding and interpreting code.
			Given a java function enclosed within three backticks(```), your task is to infer its specifications like functionality and intention. Note that the function may contain a bug, and you need to infer its previous intention instead of explaining the buggy version step by step.
			Make your response as short as possible.
			""";
	
	public static final String DECISION_INSTR = """
			You are an assistant skilled in debugging. We are conducting bug localization in Java projects. We first build a trace model for one execution. The trace contains many steps, each step represents the execution of certain statement. We start from where a fault happens (wrong variable detected or exception thrown) and trace the control or data flow step by step backward to the root cause of the fault. 
			Now we are at "current step", I will give you the following information:
			- statement executed in current step, commented with "// current step" in the context;
			- variables that are read in current step (in format of "{type:..., name:..., value:...}");
			- specification about the context;
			- debugging trace which contains previous steps we checked. Every step is the data or control dependency of its last step in debugging trace and the first step of it is where the fault happens.
			Each section is enclosed within three backticks (```).
			You need to consider the following questions:
			Is the statement of current step root cause of the fault? If yes, return "root_cause". 
			If not, which step to see next? Here are two options:
			[1] Check the control dependency of current step which determines whether to execute current step, return "check_control_dependency". This means current step should not be executed and we need to check its control dependency to see why the program falls into a wrong path;
			[2] Check the data dependency of one or more variables that are read in current step, return "check_data_dependency". This means the value of variables(s) read in current step is not correct according to the observed fault and debugging trace, we need to check its data dependency which defined or wrote the wrong variable. If you choose this option, you have to show which variables are wrong. Note that all of them must come from the "Read Variables" section.
			If not the above cases, return "stop". This means we have reached a step where everything is correct and there is no data dependency or control dependency to track.
			
			Note that:
			1.Only the statement commented with "// current step" is what we are asking about. Although you have seen the root cause in context but not statement executed in current step, you can't just return "root_cause" because we are asking about current step. 
			2.It's possible that certain step contains error (wrong variable or in a wrong control flow), but not the root cause. What we aim to do is to trace the data dependency or control dependency to find the actual root cause of the observed fault.
			3.If wrong variable or wrong control flow is detected, we should continue to track the data dependency or control dependency to find the root cause although the code executed in current step seems correct.
			Organize the response into a JSON format and briefly explain what information helps you make decision in the "reason" section. For example, you may return "{"decision":"check_data_dependency","wrong_variables":["return_from_add"],"reason":"..."}". Don't return any other explanation outside of the JSON object.
			
			""";
	
	public static final String DECISION_INSTR_TOOL = """
			You are an assistant skilled in debugging. We are conducting bug localization in Java projects. We first build a trace model for one execution. The trace contains many steps, each step represents the execution of certain statement. We start from where a fault happens (wrong variable detected or exception thrown) and trace the control or data flow step by step backward to the root cause of the fault. 
			Now we are at "current step", I will give you the following information:
			- statement executed in current step, commented with "// current step" in the context;
			- variables that are read in current step (in format of "{type:..., name:..., value:...}");
			- specification about the context;
			- debugging trace which contains previous steps we checked. Every step is the data or control dependency of its last step in debugging trace and the first step of it is where the fault happens.
			Each section is enclosed within three backticks (```).
			You need to consider the following questions:
			Is the statement of current step commented with "// current step" root cause of the fault? If yes, return "root_cause";
			If not, which step to see next? Here are two options:
			[1] Check the control dependency of current step which determines whether to execute current step, return "check_control_dependency". This means current step should not be executed and we need to check its control dependency to see why the program falls into a wrong path;
			[2] Check the data dependency of one or more variables that are read in current step, return "check_data_dependency". This means the value of variables(s) read in current step is not correct according to the observed fault and debugging trace, we need to check its data dependency which defined or wrote the wrong variable. If you choose this option, you have to show which variables are wrong. Note that all of them must come from the "Read Variables" section.
			If not the above cases, return "stop". This means we have reached a step where everything is correct and there is no data dependency or control dependency to track.
			
			Note that:
			1.Only the statement commented with "// current step" is what we are asking about. Although you have seen the root cause in context but not statement executed in current step, you can't just return "root_cause" because we are asking about current step. 
			2.It's possible that certain step contains error (wrong variable or in a wrong control flow), but not the root cause. What we aim to do is to trace the data dependency or control dependency to find the actual root cause of the observed fault.
			3.If wrong variable or wrong control flow is detected, we should continue to track the data dependency or control dependency to find the root cause although the code executed in current step seems correct.
			Organize the response into a JSON format and briefly explain what information helps you make decision in the "reason" section. For example, you may return "{"decision":"check_data_dependency","wrong_variables":["return_from_add"],"reason":"..."}". Don't return any other explanation outside of the JSON object.
			Also, if you lack information to make a decision, just return what information you need. In this case, enclose your response within a pair of <Request> tag, for example: "<Request> I need the fields of class A </Request>".

			""";

	
	// ============= Trail =============
	public static final String INFER_INSTR_COMPLEX = """
			You are an assistant skilled in understanding and interpreting code.
			I'm conducting bug localization and will give you a function (may contain a bug), the target step (with the comment "// target code") and the read variables in the target step (formatted in {type:...,name:...,value:...}).
			The debugging trace shows how we look into target step from where a fault happens (wrong variable detected or exception thrown) through control dependency or data dependency.
			Each section is enclosed within three backticks (```).
			
			Also, you have access to the following tools :
			[1] get_method: get the implementation of certain method. 
			Arguments: class.method_name (str) (e.g. Complex.getReal)
			[2] get_class: get the fields of certain class.
			Arguments: class_name (str)
			[3] get_excution_info: get one excution trace of this function, including code of each step, variables in each step.
			Arguments: None
			---
			You have two options:
			1. Use one of the tools by outputting a JSON object with the following fields:
			- "tool": the name of the tool
			- "args": a list of arguments to the tool
			Your tool invocation should be enclosed using "<Tool>" tag, for example: <Tool> {"tool":"tool_name","args":["argument_1"]} </Tool>.
			You can only invoke one tool at a time. All the outputs of tools used before are shown in "Gathered information" section enclosed within two dollars($$).
			2. Make inferences about:
			- The specifications of the function including the original intention and actual behavior according to the code, variables, debugging trace and gathered information.
			- The intention of the target step and whether it is in the correct control flow and what the value of the variable read by this step should be.
			Make inferences only when you have gathered enough information and have enough confidance.
			Your inferences should be enclosed using "<Inference>" tag, for example: <Inference> Function:... Target Step:... </Inference>.
			Note that your output should always contain either "<Tool>" or '<Inference>' , but not both.
			---
			""";
	
	public static final String INFER_INSTR_WO_TOOL = """
			You are an assistant skilled in reading and interpreting code.
			I'm conducting bug localization and will give you a function (may contain a bug), the target step (with the comment "// target code") and the read variables in the target step (formatted in {type:...,name:...,value:...}).
			The debugging trace shows how we look into target step from where a fault happens (wrong variable detected or exception thrown) through control dependency or data dependency.
			Each section is enclosed within three backticks (```).

			You need to make inferences about:
			- The specifications of the function including the original intention and actual behavior according to the code, variables, debugging trace and gathered information.
			- The intention of the target step and whether it is in the correct control flow and what the value of the variable read by this step should be.
			Your inferences should be enclosed using "<Inference>" tag, for example: <Inference> Function:... Target Step:... </Inference> and reply as short as possible.
			---
			""";
	
	
	public static final String DECISION_TASK = """
			Current step is the data or control dependency of the last step in the debugging trace. From the above information, make a decision about the type of current step, you can choose:
			1.wrong variable: The value of some variables read in this step is wrong and we want to look into the data dependency which wrote or defined the wrong data. Note that only consider the variables in the "Read Variables" section. If its content is "None", you are not allowed to choose this option;
			2.wrong path: This step should not happen and we want to look into its control dependency to inspect why current step is executed;
			3.root cause: We find the statement executed in current step contains a bug and is the root cause of the fault;
			4.correct: We have reached a step where the statement and variables are all correct. This means the fault is caused by missing a piece of code..
			At the same time, label each variable in "Read variables" section with "correct" or "wrong". 
			Finally, briefly explain what information helps you make decision in the "reason" section. 
			Organize the response into a JSON format. For example, if current step is of wrong variable type and the "Read variables" section contains 2 variables, you may return "{"type":"wrong variable", "variable":{"var1":"correct","var2":"wrong"}, "reason":"..."}". If "Read variables" section is "None", just return "{"type":"...","variable":{},"reason":"..."}".		
			""";
}
