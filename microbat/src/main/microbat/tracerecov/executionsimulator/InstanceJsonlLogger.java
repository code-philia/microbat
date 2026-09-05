package microbat.tracerecov.executionsimulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.google.gson.Gson;

/** Append-only per-instance event log shared by the recovery and LLM code. */
public final class InstanceJsonlLogger {
    private static final ThreadLocal<InstanceJsonlLogger> CURRENT = new ThreadLocal<>();
    private static final AtomicLong EVENT_ID = new AtomicLong();

    private final PrintWriter writer;
    private final Gson gson = new Gson();

    private InstanceJsonlLogger(String path) throws IOException {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        writer = new PrintWriter(new FileWriter(file, true));
    }

    public static InstanceJsonlLogger begin(String path, String instance) {
        closeCurrent();
        try {
            InstanceJsonlLogger logger = new InstanceJsonlLogger(path);
            CURRENT.set(logger);
            logger.event("instance_start", details("instance", instance));
            return logger;
        } catch (IOException e) {
            return null;
        }
    }

    public static InstanceJsonlLogger current() {
        return CURRENT.get();
    }

    public static void closeCurrent() {
        InstanceJsonlLogger logger = CURRENT.get();
        if (logger != null) {
            logger.close();
            CURRENT.remove();
        }
    }

    public void event(String type, Map<String, Object> details) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("time", Instant.now().toString());
        record.put("eventId", EVENT_ID.incrementAndGet());
        record.put("type", type);
        record.put("details", details == null ? new LinkedHashMap<>() : details);
        writer.println(gson.toJson(record));
        writer.flush();
    }

    public static Map<String, Object> details(Object... values) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            details.put(String.valueOf(values[i]), values[i + 1]);
        }
        return details;
    }

    public void close() {
        writer.flush();
        writer.close();
    }
}
