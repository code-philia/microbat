package microbat.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import microbat.codeanalysis.runtime.InstrumentationExecutor;
import microbat.handler.replayexp.ConcurrentReplayJob;
import microbat.handler.replayexp.NormalTraceJob;
import microbat.handler.replayexp.ReplayJob;
import microbat.instrumentation.instr.aggreplay.ReplayMode;
import microbat.instrumentation.utils.MicrobatUtils;
import microbat.util.Settings;
import sav.common.core.utils.SingleTimer;


public class ReplayExperimentHandler extends AbstractHandler {
	private int numberOfRuns = 200;
	private static String resultLocation = "D:\\replayExperiment.xlsx";
	private static boolean skipStrict = false;
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ExperimentJob experimentJob = new ExperimentJob(numberOfRuns);
		experimentJob.schedule();
		return null;
	}
	
	private static class MemoryJob extends ReplayJob {
		private String dumpFile;
		public MemoryJob(String dumpFile) {
			super("normal memory run");
			// TODO Auto-generated constructor stub
			this.dumpFile = dumpFile;

		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			SingleTimer timer = SingleTimer.start("no trace");
			InstrumentationExecutor executor = new InstrumentExecutorSupplierImpl().get();
			String processError = executor.runMemoryMeasureMent(dumpFile);
			long runTime = timer.getExecutionTime();
			stats.setRunTime(runTime);
			stats.setStdError(processError);
			stats.memoryUsed = getMemory();
			return Status.OK_STATUS;
		}
		
		
		protected long getMemory() {
			Scanner dumpFileScanner;
			long origMemSize = -1;
			try {
				dumpFileScanner = new Scanner(new File(dumpFile));
				if (dumpFileScanner.hasNext()) origMemSize = dumpFileScanner.nextLong();
				dumpFileScanner.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return origMemSize;
		}
		
		
	}
	
	private static class ExperimentJob extends Job {

		Class<?> statClass = ReplayStats.class;
		private XSSFWorkbook workbook = new XSSFWorkbook();
		private int numberOfRuns = 100;
		public ExperimentJob(int numberOfRuns) {
			super("Run replay experiment");
			this.numberOfRuns = numberOfRuns;
		}
		
		private void writeWorkBook() {
			try {
				FileOutputStream fileOutputStream = new FileOutputStream(new File(resultLocation));
				this.workbook.write(fileOutputStream);
				fileOutputStream.flush();
				fileOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		private void runJobProducer(Function<String, ReplayJob> jobProducer, String workSheetName) {
			LinkedList<ReplayStats> stats = new LinkedList<>();
			for (int i = 0; i < numberOfRuns; ++i) {
				ReplayJob job  = jobProducer.apply("");
				job.schedule();
				try {
					job.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				stats.add(job.getReplayStats());
			}
			XSSFSheet sheet = workbook.createSheet(workSheetName);
			writeToWorkSheet(sheet, stats);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ConcurrentReplayHandler replayHandler = new ConcurrentReplayHandler();
			ReplayMode[] replayMode = ReplayMode.values();
			
			ReplayMode temp = Settings.replayMode;
			// run the experiment over the replay modes
			for (int j = 0; j < replayMode.length; ++j) {
				if (skipStrict && replayMode[j] == ReplayMode.STRICT_RW) continue;
				Settings.replayMode = replayMode[j];
				runJobProducer(v -> replayHandler.createReplayJob(), "experiment" + Settings.replayMode.toString());
			}
			runJobProducer(v -> new NormalTraceJob(), "normal runs");
			
			File tempFile = null;
			try {
				tempFile = File.createTempFile("memory", "txt");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			final File tFile = tempFile;
			runJobProducer(v -> new MemoryJob(tFile.getAbsolutePath()), "memory run");
			writeWorkBook();
			
			return Status.OK_STATUS;
		}
		
		protected void writeToWorkSheet(XSSFSheet workSheet, Collection<ReplayStats> stats) {
			int rowNum = 0;
			initHeaders(workSheet.createRow(rowNum));
			rowNum++;
			for (ReplayStats stat : stats) {
				writeToRow(workSheet.createRow(rowNum), stat);
				rowNum++;
			}
			writeWorkBook();
			
		}
		
		protected void initHeaders(XSSFRow row) {
			Field[] fields = statClass.getFields();
			int v = 0; 
			for (Field field : fields) {
				XSSFCell cell = row.createCell(v++);
				cell.setCellValue(field.getName());
			}
		}
		
		private void writeToRow(XSSFRow row, ReplayStats replayStats) {
			Field[] fields = statClass.getFields();
			int cellIdx = 0;
			for (Field field : fields) {
				try {
					Object resultObject = field.get(replayStats);
					XSSFCell cell = row.createCell(cellIdx++);
					if (resultObject == null) {
						cell.setCellValue("null");
					} else {
						cell.setCellValue(resultObject.toString());
					}
					
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
}
