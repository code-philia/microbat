package microbat.runconfigs;

import org.eclipse.core.commands.AbstractHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestLoadConfig extends AbstractHandler {

    private ExecuteWithConfig<TraceRecovRunConfig> executeWithConfig = new ExecuteWithConfig<>(
            TraceRecovRunConfig.class, "Load Config", this::loadConfig);

    @Override
    public Object execute(org.eclipse.core.commands.ExecutionEvent event) {
        executeWithConfig.execute();
        return null;
    }

    private void loadConfig(TraceRecovRunConfig config) {
        log.info("Loading configuration: {}", config);
    }
}
