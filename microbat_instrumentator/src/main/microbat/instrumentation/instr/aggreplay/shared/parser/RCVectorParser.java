package microbat.instrumentation.instr.aggreplay.shared.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microbat.instrumentation.instr.aggreplay.shared.ParseData;
import microbat.instrumentation.instr.aggreplay.shared.Parser;
import sav.common.core.Pair;

/**
 * Parser for parsing a map from long to int
 * @author Gabau
 *
 */
public class RCVectorParser implements Parser<Map<Long, Integer>> {
	
	public Map<Long, Integer> parse(ParseData data) {
		List<Pair<ParseData, ParseData>> objectMap = data.toPairList();
		Map<Long, Integer> values = new HashMap<>();
		for (Pair<ParseData, ParseData> innerData : objectMap) {
			Long keyLong = innerData.first().getLongValue();
			Integer keyInteger = innerData.second().getIntValue();
			values.put(keyLong, keyInteger);
		}
		return values;
	}
}
