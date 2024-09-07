package microbat.instrumentation.dataflowrecovery;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import microbat.instrumentation.AgentLogger;
import microbat.instrumentation.output.TraceOutputReader;
import microbat.instrumentation.output.TraceOutputWriter;
import microbat.instrumentation.precheck.PrecheckInfo;
import microbat.instrumentation.utils.FileUtils;
import microbat.model.ClassLocation;
import sav.common.core.SavRtException;

/**
 * @author hongshuwang
 */
public class DependencyRecoveryInfo {
	
	public Map<String, Set<String>> libraryCalls;
	
	public DependencyRecoveryInfo() {
		this.libraryCalls = new HashMap<>();
	}
	
	public DependencyRecoveryInfo(Map<String, Set<String>> libraryCalls) {
		this.libraryCalls = libraryCalls;
	}
	
	public void saveToFile(String dumpFile, boolean append) {
		OutputStream bufferedStream = null;
		TraceOutputWriter outputWriter = null;
		FileOutputStream fileStream = null;
		
		try {
			File file = FileUtils.getFileCreateIfNotExist(dumpFile);
			fileStream = new FileOutputStream(file, append);
			try {
				// Avoid concurrent writes from other processes:
				fileStream.getChannel().lock();
			} catch (IOException e)  {
				// ignore
				AgentLogger.error(e);
			}
			bufferedStream = new BufferedOutputStream(fileStream);
			outputWriter = new TraceOutputWriter(bufferedStream);
			outputWriter.writeLibraryCalls(libraryCalls);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufferedStream != null) {
					bufferedStream.close();
				}
				if (outputWriter != null) {
					outputWriter.close();
				}
				if (fileStream != null) {
					fileStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static DependencyRecoveryInfo readFromFile(String filePath) {
		FileInputStream stream = null;
		TraceOutputReader reader = null;
		try {
			stream = new FileInputStream(filePath);
			reader = new TraceOutputReader(new BufferedInputStream(stream));
			DependencyRecoveryInfo info = new DependencyRecoveryInfo();
			
			int numOfRows = reader.readVarInt();
			Map<String, Set<String>> libraryCalls = new HashMap<>();
			String className = "";
			for (int i = 0; i < numOfRows; i++) {
				String line = reader.readString();
				if (line.charAt(0) == '#') {
					className = line.substring(1);
					if (!libraryCalls.containsKey(className)) {
						libraryCalls.put(className, new HashSet<String>());
					}
				} else {
					libraryCalls.get(className).add(line);
				}
			}
			info.libraryCalls = libraryCalls;
			return info;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
				if (reader != null) {
					reader.close();
				}
			} catch (Exception e) {
				// ignore
			}
		}
	}
}
