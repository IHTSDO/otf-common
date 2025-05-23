package org.snomed.otf.script.dao;

import java.util.*;

import org.ihtsdo.otf.exception.TermServerScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.Script;
import org.snomed.otf.script.dao.ReportConfiguration.ReportFormatType;
import org.snomed.otf.script.dao.ReportConfiguration.ReportOutputType;
import org.snomed.otf.script.dao.transformer.CSVToJSONDataTransformer;

public class ReportManager extends CommonFileWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportManager.class);

	boolean writeToFile = false;
	ReportFileManager reportFileManager;
	
	boolean writeToSheet = false;
	ReportSheetManager reportSheetManager;

	boolean writeToS3 = false;
	ReportS3FileManager reportS3FileManager;
	
	protected int numberOfDistinctReports = 1;
	Script script;
	
	protected DataBroker dataUploader;
	List<String> tabNames;
	
	private ReportManager() {}
	
	Set<Integer> disabledTabs = new HashSet<>();
	Set<Integer> disableTabAdvised = new HashSet<>();
	
	public static ReportManager create(Script script, ReportConfiguration reportConfiguration) throws TermServerScriptException {
		ReportManager rm = new ReportManager();
		rm.script = script;
		rm.dataUploader = script.getReportDataUploader();
		rm.init(reportConfiguration);
		return rm;
	}
	
	private void init(ReportConfiguration reportConfiguration) {
		reportFileManager = new ReportFileManager(this);
		Set<ReportOutputType> reportOutputTypes = reportConfiguration.getReportOutputTypes();
		Set<ReportConfiguration.ReportFormatType> reportFormatTypes = reportConfiguration.getReportFormatTypes();

		if (script.isOffline() || reportOutputTypes.contains(ReportOutputType.LOCAL_FILE) ) {
			LOGGER.info("Running in offline mode. Outputting to file rather than google sheets");
			writeToSheet = false;
			writeToS3 = false;
			writeToFile = true;
		} else {
			if (reportOutputTypes.contains(ReportOutputType.GOOGLE) && reportFormatTypes.contains(ReportFormatType.CSV)) {
				reportSheetManager = new ReportSheetManager(this);
				writeToSheet = true;
			}
			if (reportOutputTypes.contains(ReportOutputType.S3) && reportFormatTypes.contains(ReportFormatType.JSON)) {
				// NOTE: This will exclude the last row(totals). As this is only used for
				// SummaryComponentStats and isn't generic this is fine. If this changes
				// in the future extra work is required to know if the last row in whichever
				// report is indeed a total row. For now this is fine.
				reportS3FileManager = new ReportS3FileManager(this,
						dataUploader, new CSVToJSONDataTransformer(true));
				writeToS3 = true;
			}
		}
		tabNames = Arrays.asList("Sheet1");
	}
	
	public boolean writeToReportFile(int reportIdx, String line) throws TermServerScriptException {
		if (disabledTabs.contains(reportIdx)) {
			if (!disableTabAdvised.contains(reportIdx)) {
				reportSheetManager.writeToReportFile(reportIdx, "Tab disabled programmatically.  See 'getReportManager().disableTab'", false);
				disableTabAdvised.add(reportIdx);
			}
			return true;  //Skip this output
		}
		
		boolean writeSuccess = false;
		if (writeToFile) {
			writeSuccess = reportFileManager.writeToReportFile(reportIdx, line, false);
		}
		
		if (writeToSheet) {
			writeSuccess = reportSheetManager.writeToReportFile(reportIdx, line, false);
		}

		if (writeToS3) {
			writeSuccess = reportS3FileManager.writeToReportFile(reportIdx, line, false);
		}
		return writeSuccess;
	}

	@Override
	public void flushFiles(boolean andClose) throws TermServerScriptException {
		//Watch that we might have written to RF2 files, even if writeToFile is set to false
		if (writeToFile) {
			reportFileManager.flushFiles(andClose);
		}
		
		if (writeToSheet) {
			reportSheetManager.flushWithWait();
			if (andClose) {
				// format the columns in the spreadsheet
				reportSheetManager.formatSpreadSheetColumns();
				LOGGER.info("See Google Sheet: {}", reportSheetManager.getUrl());
			}
		}

		if (writeToS3) {
			reportS3FileManager.flushFiles(andClose);
			if (andClose) {
				try {
					reportS3FileManager.postProcess();
				} catch (Exception e) {
					throw new TermServerScriptException(e);
				}
			}
		}
	}
	
	public void flushFilesSoft() throws TermServerScriptException {
		if (writeToFile) {
			reportFileManager.flushFiles(false);
		}
		
		if (writeToSheet) {
			reportSheetManager.flushSoft();
		}

		if (writeToS3) {
			reportS3FileManager.flushFiles(false);
		}
	}
	
	public void initialiseReportFiles(String[] columnHeaders) throws TermServerScriptException {
		//Trim all the column headers to remove leading/trailing spaces
		for (int i=0; i<columnHeaders.length; i++) {
			columnHeaders[i] = columnHeaders[i].trim();
		}

		if (writeToFile) {
			reportFileManager.initialiseReportFiles(columnHeaders);
		}

		if (writeToSheet) {
			reportSheetManager.initialiseReportFiles(columnHeaders);
		}

		if (writeToS3) {
			reportS3FileManager.initialiseReportFiles(columnHeaders);
		}
	}
	
	public void setNumberOfDistinctReports(int x) {
		numberOfDistinctReports = x;
	}

	public int getNumberOfDistinctReports() {
		return numberOfDistinctReports;
	}

	public Script getScript() {
		return script;
	}

	public List<String> getTabNames() {
		return tabNames;
	}
	
	public void setTabNames(String[] tabNames) {
		this.tabNames = Arrays.asList(tabNames);
		this.numberOfDistinctReports = tabNames.length;
	}

	public void setFileOnly() {
		writeToFile = true;
		writeToSheet = false;
	}

	public String getUrl() {
		if (writeToSheet) {
			return reportSheetManager.getUrl();
		} else if (writeToS3) {
			return reportS3FileManager.getURL();
		} else {
			return reportFileManager.getFileName();
		}
	}
	
	public void setWriteToFile(boolean flag) {
		writeToFile = flag;
	}
	
	public void setWriteToSheet(boolean flag) {
		writeToSheet = flag;
	}

	public void setWriteToS3(boolean flag) {
		this.writeToS3 = flag;
	}
	
	public boolean isWriteToS3() {
		return this.writeToS3;
	}

	public boolean isWriteToSheet() {
		return writeToSheet;
	}

	public String getEnv() {
		//If we're working against a published release, then the environment isn't relevant
		String releaseBranch = script.detectReleaseBranch();
		return releaseBranch == null ? script.getEnv() : releaseBranch;
	}

	public void disableTab(int tabIdx) {
		disabledTabs.add(tabIdx);
	}

}
