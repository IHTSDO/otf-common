package org.snomed.otf.script.dao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.ihtsdo.otf.utils.SnomedUtilsBase;
import org.snomed.otf.script.Script;

public class ReportFileManager implements RF2Constants, ReportProcessor {

	public static final String REPORTS_DIRECTORY = "reports";

	protected File[] reportFiles;
	protected Map<String, PrintWriter> printWriterMap = new HashMap<>();
	protected String currentTimeStamp;
	ReportManager owner;
	SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");

	public ReportFileManager() {
	}

	public ReportFileManager(ReportManager owner) {
		this.owner = owner;
	}

	protected PrintWriter getPrintWriter(String fileName) throws TermServerScriptException {
		try {
			PrintWriter pw = printWriterMap.get(fileName);
			if (pw == null) {
				File file = SnomedUtilsBase.ensureFileExists(fileName);
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
				BufferedWriter bw = new BufferedWriter(osw);
				pw = new PrintWriter(bw);
				printWriterMap.put(fileName, pw);
			}
			return pw;
		} catch (Exception e) {
			throw new TermServerScriptException("Unable to initialise " + fileName + " due to " + e.getMessage(), e);
		}
	}

	public void flushFiles(boolean andClose) {
		if ( printWriterMap != null && !printWriterMap.isEmpty()) {
			for (PrintWriter pw : printWriterMap.values()) {
				try {
					if (pw != null) {
						pw.flush();
						if (andClose) {
							pw.close();
						}
					}
				} catch (Exception e) {
				}
			}
		}
		if (andClose) {
			printWriterMap = new HashMap<>();
		}
	}

	@Override
	public void initialiseReportFiles(String[] columnHeaders) throws TermServerScriptException {
		currentTimeStamp = df.format(new Date());
		reportFiles = new File[owner.getNumberOfDistinctReports()];
		String reportName = getReportName();
		String directory = REPORTS_DIRECTORY + File.separator + owner.getScript().getEnv() + File.separator;
		for (int reportIdx = 0; reportIdx < owner.getNumberOfDistinctReports(); reportIdx++) {
			String idxStr = reportIdx == 0 ? "" : "_" + reportIdx;
			String reportFilename = directory + "results_" + reportName + "_" + currentTimeStamp + "_" + owner.getEnv()  + idxStr + ".csv";
			reportFiles[reportIdx] = new File(reportFilename);
			Script.info("Outputting Report to " + reportFiles[reportIdx].getAbsolutePath());
			writeToReportFile (reportIdx, columnHeaders[reportIdx], false);
		}
		flushFiles(false);
	}

	@Override
	public boolean writeToReportFile(int reportIdx, String line, boolean delayWrite) throws TermServerScriptException {
		try {
			PrintWriter pw = getPrintWriter(reportFiles[reportIdx].getAbsolutePath());
			pw.println(line);
		} catch (Exception e) {
			throw new IllegalStateException("Unable to output report line: " + line, e);
		}
		return true;
	}

	public Map<String, PrintWriter> getPrintWriterMap() {
		return printWriterMap;
	}

	public void setPrintWriterMap(Map<String, PrintWriter> printWriterMap) {
		this.printWriterMap = printWriterMap;
	}

	public String getFileName() {
		return reportFiles[0].getAbsolutePath();
	}

	public String getReportName() {
		return owner.getScript().getClass().getSimpleName().replaceAll(" ", "_");
	}
}
