package microbat.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

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

import microbat.instrumentation.instr.aggreplay.ReplayMode;
import microbat.util.Settings;


public class ReplayExperimentHandler extends AbstractHandler {
	private int numberOfRuns = 100;
	private static String resultLocation = "D:\\replayExperiment.xlsx";
	private boolean skipStrict = false;
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ExperimentJob experimentJob = new ExperimentJob(numberOfRuns);
		experimentJob.schedule();
		return null;
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

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ConcurrentReplayHandler replayHandler = new ConcurrentReplayHandler();
			ReplayMode[] replayMode = ReplayMode.values();
			
			ReplayMode temp = Settings.replayMode;
			// run the experiment over the replay modes
			for (int j = 0; j < replayMode.length; ++j) {
				Settings.replayMode = replayMode[j];
				LinkedList<ReplayStats> stats = new LinkedList<>();
				for (int i = 0; i < numberOfRuns; ++i) {
					ConcurrentReplayJob newJob = replayHandler.createReplayJob();
					newJob.schedule();
					try {
						newJob.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					stats.add(newJob.getReplayStats());
				}
				XSSFSheet spreadSheetWorkbook = workbook.createSheet("replay experiment data " + replayMode[j].toString());
				writeToWorkSheet(spreadSheetWorkbook, stats);
			}
			Settings.replayMode = temp;
			LinkedList<ReplayStats> normalStats = new LinkedList<ReplayStats>(); 
			for (int i = 0; i < numberOfRuns; ++i) {
				NormalTraceGeneration normalTraceGeneration = new NormalTraceGeneration();
				normalTraceGeneration.schedule();
				try {
					normalTraceGeneration.join();
					normalStats.add(normalTraceGeneration.getReplayStats());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			XSSFSheet normalSheet = workbook.createSheet("normal runs");
			writeToWorkSheet(normalSheet, normalStats);
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
