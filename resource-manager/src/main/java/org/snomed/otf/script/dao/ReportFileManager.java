package org.snomed.otf.script.dao;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.ihtsdo.otf.exception.TermServerScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportFileManager extends CommonFileWriter implements ReportProcessor {

	private final Logger logger = LoggerFactory.getLogger(ReportFileManager.class);

	public static final String REPORTS_DIRECTORY = "reports";

	protected File[] reportFiles;
	protected String currentTimeStamp;
	ReportManager owner;
	SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");

	public ReportFileManager(ReportManager owner) {
		this.owner = owner;
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
			logger.info("Outputting Report to {}", reportFiles[reportIdx].getAbsolutePath());
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

	public String getFileName() {
		return reportFiles[0].getAbsolutePath();
	}

	public String getReportName() {
		return owner.getScript().getClass().getSimpleName().replace(" ", "_");
	}
}
