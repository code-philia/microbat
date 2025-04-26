package microbat.runconfigs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GptConfig {
    private String gptApiKey = null;
    private String gptBaseUrl = "https://api.openai.com/v1";
    private String gptModel = "gpt-4o";
}
