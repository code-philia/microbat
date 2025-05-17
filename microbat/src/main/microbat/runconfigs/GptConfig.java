package microbat.runconfigs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GptConfig {
    private String gptApiKey = null;
    private String gptBaseUrl = "https://api.key77qiqi.cn/v1/chat/completions";
    private String gptModel = "gpt-4o";
    private int maxTokens = 16384;
}
