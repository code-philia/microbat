package microbat.instrumentation.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DebugUtils {
	public static void filePrintln(String filepath,String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath, true))) {
            writer.append(content);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
