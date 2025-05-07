package microbat.runconfigs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionInfo<T> {
    private String taskName;
    private String descriptionFilePath;
    private String configFilePath;
    private String workingFolder;

    private T config;

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        return gson.toJson(this);
    }

    public static boolean isAbsolutePath(String path) {
        return (path.length() > 1 && path.charAt(1) == ':') || path.charAt(0) == '/' || path.charAt(0) == '\\';
    }

    public String resolvePath(String path) {
        if (path.equals(".")) {
            return workingFolder;
        }
        if (isAbsolutePath(path)) {
            return path;
        } else {
            return workingFolder + File.separator + path;
        }
    }

    public String resolveAndSetConfigPath(String descriptionFilePath, String selectedTest) {
        this.descriptionFilePath = descriptionFilePath;
        this.workingFolder = Paths.get(descriptionFilePath).getParent().toString();
        this.configFilePath = resolvePath(selectedTest);
        return this.configFilePath;
    }
}
