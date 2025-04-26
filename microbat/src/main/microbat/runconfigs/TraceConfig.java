package microbat.runconfigs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TraceConfig {
    private int stepLimit = 1000000;
    private int variableLayers = 3;
}
