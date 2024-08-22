package microbat.instrumentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import microbat.instrumentation.utils.DebugUtils;
import sav.common.core.utils.CollectionUtils;

/**
 * 
 * @author lyly
 *
 */
public class CommandLine {
	private Map<String, String> argMap = new HashMap<>();
	
	public static CommandLine parse(String agentArgs) {
		CommandLine cmd = new CommandLine();
		
		int headOfCondition = agentArgs.indexOf("condition=");
		int tailOfCondition = agentArgs.lastIndexOf("###");
		
		String conditionArgs = agentArgs.substring(headOfCondition,tailOfCondition);
		String otherArgs1 = agentArgs.substring(0,headOfCondition);
		String otherArgs2 = agentArgs.substring(tailOfCondition+4);
		String otherArgs = otherArgs1+otherArgs2;

		//condition args
		String[] conditionKeyValue = conditionArgs.split(AgentConstants.AGENT_OPTION_SEPARATOR,2);
		cmd.argMap.put(conditionKeyValue[0], conditionKeyValue[1]);
		
		//other args
		String[] args = otherArgs.split(AgentConstants.AGENT_PARAMS_SEPARATOR);
		for (String arg : args) {
			String[] keyValue = arg.split(AgentConstants.AGENT_OPTION_SEPARATOR);
			cmd.argMap.put(keyValue[0], keyValue[1]);
		}
		
//		int i = 0;
//		while(i < args.length) {
//			if(args[i].indexOf(AgentConstants.AGENT_OPTION_SEPARATOR)==-1) {
//				// condition.value contains ","
//				String condition_arg = args[i-1];
//				while(i<args.length && args[i].indexOf(AgentConstants.AGENT_OPTION_SEPARATOR)==-1) {
//					condition_arg += (","+args[i]);
//					i++;
//				}
//				String[] keyValue = condition_arg.split(AgentConstants.AGENT_OPTION_SEPARATOR,2);
//				cmd.argMap.put(keyValue[0], keyValue[1]);
//			}
//			else {
//				String[] keyValue = args[i].split(AgentConstants.AGENT_OPTION_SEPARATOR,2);
//				cmd.argMap.put(keyValue[0], keyValue[1]);
//				i++;
//			}
//		}
		return cmd;
	}

	public boolean getBoolean(String option, boolean defaultValue) {
		String strVal = getString(option);
		if (strVal != null) {
			return Boolean.valueOf(strVal);
		}
		return defaultValue;
	}

	public int getInt(String option, int defaultValue) {
		String strVal = getString(option);
		if (strVal != null) {
			return Integer.valueOf(strVal);
		}
		return defaultValue;
	}

	public List<String> getStringList(String option) {
		String value = getString(option);
		if (value == null || value.isEmpty()) {
			return new ArrayList<>(0);
		}

		return CollectionUtils.toArrayList(value.split(AgentConstants.AGENT_PARAMS_MULTI_VALUE_SEPARATOR));
	}
	
	public Set<String> getStringSet(String option) {
		String value = getString(option);
		if (value == null || value.isEmpty()) {
			return new HashSet<>(0);
		}

		return CollectionUtils.toHashSet(value.split(AgentConstants.AGENT_PARAMS_MULTI_VALUE_SEPARATOR));
	}
	
	public RuntimeCondition getRuntimeCondition(String option) {
		String value = getString(option);
		RuntimeCondition condition = new RuntimeCondition(null, null, null, null);
		
		if (value == null || value.isEmpty()) {
			return condition;
		}

		String[] conditions = value.split(AgentConstants.AGENT_CONDITION_SEPARATOR);
		
		for (String conditionString : conditions) {
			String[] keyValPair = conditionString.split(AgentConstants.AGENT_CONDITION_KEY_VAL_SEPARATOR, 2);
			String conditionKey = keyValPair[0];
			String conditionVal = keyValPair[1];
						
			switch (conditionKey) {
			case AgentParams.OPT_CONDITION_VAR_NAME:
				condition.setVariableName(conditionVal);
				break;
			case AgentParams.OPT_CONDITION_VAR_TYPE:
				condition.setVariableType(conditionVal);
				break;
			case AgentParams.OPT_CONDITION_VAR_VALUE:
				condition.setVariableValue(conditionVal);
				break;
			case AgentParams.OPT_CONDITION_CLASS_STRUCTURE:
				condition.setClassStructure(conditionVal);
				break;
			case AgentParams.OPT_CONDITION_EXT_LIBCALL:
				if(!"null".equals(conditionVal)) {
					String[] libCalls = conditionVal.split(",");
					ArrayList<String> extLibCall = new ArrayList<>(Arrays.asList(libCalls));
					condition.setExtLibCall(extLibCall);
				}
				break;
			default: // do nothing
			}
		}
		return condition;
	}

	public String getString(String option) {
		return argMap.get(option);
	}
}
