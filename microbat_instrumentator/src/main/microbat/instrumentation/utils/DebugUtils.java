package microbat.instrumentation.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DebugUtils {
		
	public final static String defaultPath = "C:\\Users\\Kwy\\Desktop\\debug.txt";
	
	public static void filePrintln(String filepath,String content) {		
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath==null?defaultPath:filepath, true))) {
	        writer.append(content);
	        writer.newLine();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
}
