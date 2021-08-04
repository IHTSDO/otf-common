package org.snomed.otf.script;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.NotImplementedException;
import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Project;
import org.ihtsdo.otf.utils.ExceptionUtils;
import org.ihtsdo.otf.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.scheduler.domain.JobRun;
import org.snomed.otf.script.dao.DataUploader;
import org.snomed.otf.script.dao.RF2Manager;
import org.snomed.otf.script.dao.ReportConfiguration;
import org.snomed.otf.script.dao.ReportManager;

public abstract class Script implements RF2Constants {
	
	protected static final String REPORT_OUTPUT_TYPES = "ReportOutputTypes";
	protected static final String REPORT_FORMAT_TYPE = "ReportFormatType";
	
	static Logger sLogger = LoggerFactory.getLogger(Script.class);
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected ReportManager reportManager;
	protected RF2Manager rf2Manager;
	protected Project project;
	protected Date startTime;
	protected Map<String, Object> summaryDetails = new TreeMap<String, Object>();
	protected boolean quiet = false;
	protected boolean suppressOutput = false;
	protected ReportConfiguration reportConfiguration;
	
	public static void info (String msg) {
		sLogger.info(msg);
	}
	
	public static void debug (Object obj) {
		sLogger.debug(obj==null?"NULL":obj.toString());
	}
	
	public static void warn (Object obj) {
		sLogger.warn("*** " + (obj==null?"NULL":obj.toString()));
	}
	
	public static void error (Object obj, Exception e) {
		System.err.println ("*** " + (obj==null?"NULL":obj.toString()));
		if (e != null) 
			sLogger.error(ExceptionUtils.getStackTrace(e));
	}
	
	public static void print (Object msg) {
		System.out.print (msg.toString());
	}
	
	public static void println (Object msg) {
		System.out.println (msg.toString());
	}
	
	public static String getMessage (Exception e) {
		String msg = e.getMessage();
		Throwable cause = e.getCause();
		if (cause != null) {
			msg += " caused by " + cause.getMessage();
			if (cause.getMessage() != null && cause.getMessage().length() < 6) {
				msg += " @ " + cause.getStackTrace()[0];
			}
		}
		return msg;
	}

	public abstract boolean isOffline();

	public abstract JobRun getJobRun();

	public abstract String getReportName();

	public abstract String detectReleaseBranch();

	public abstract String getEnv();

	public String getReportComplexName() {
		throw new NotImplementedException("Complex name currently only required by SummaryComponentStats");
	}

	public DataUploader getReportDataUploader() throws TermServerScriptException {
		throw new NotImplementedException("ReportDataUploader should be provided by TermServerScript.");
	}
	
	public ReportManager getReportManager() {
		return reportManager;
	}
	
	public RF2Manager getRF2Manager() {
		if (rf2Manager == null) {
			rf2Manager = new RF2Manager();
		}
		return rf2Manager;
	}

	public void setReportManager(ReportManager reportManager) {
		this.reportManager = reportManager;
	}
	
	protected void reportSafely (int reportIdx, Object... details) {
		try {
			report (reportIdx, details);
		} catch (TermServerScriptException e) {
			throw new IllegalStateException("Failed to write to report", e);
		}
	}
	
	public Project getProject() {
		return project;
	}
	
	public void setProject(Project project) {
		this.project = project;
	}

	public void startTimer() {
		startTime = new Date();
	}
	
	public void addSummaryInformation(String item, Object detail) {
		info(item + ": " + detail);
		summaryDetails.put(item, detail);
	}
	
	public void incrementSummaryInformation(String key) {
		if (!quiet) {
			incrementSummaryInformation(key, 1);
		}
	}
	
	public int getSummaryInformationInt(String key) {
		Object info = summaryDetails.get(key);
		if (info == null || !(info instanceof Integer)) {
			return 0;
		}
		return (Integer)info;
	}
	
	public void incrementSummaryInformationQuiet(String key) {
		//There are occasions where we can only capture all information when doing the first pass
		//When we're looking at ALL information eg which concepts do not require changes.
		if (quiet) {
			incrementSummaryInformation(key, 1);
		}
	}
	
	public void initialiseSummaryInformation(String key) {
		summaryDetails.put(key, Integer.valueOf(0));
	}
	
	public void incrementSummaryInformation(String key, int incrementAmount) {
		if (!summaryDetails.containsKey(key)) {
			summaryDetails.put(key, Integer.valueOf(0));
		}
		int newValue = ((Integer)summaryDetails.get(key)).intValue() + incrementAmount;
		summaryDetails.put(key, newValue);
	}
	
	public void flushFilesSoft() throws TermServerScriptException {
		getReportManager().flushFilesSoft();
	}
	
	public void flushFiles(boolean andClose, boolean withWait) throws TermServerScriptException {
		if (getRF2Manager() != null) {
			getRF2Manager().flushFiles(andClose);
		}
		if (getReportManager() != null) {
			getReportManager().flushFiles(andClose, withWait);
		}
	}
	
	public void flushFilesSafely(boolean andClose) {
		try {
			boolean andWait = false;
			flushFiles(andClose, andWait);
		} catch (Exception e) {
			error("Failed to flush files.", e);
		}
	}
	
	public void flushFilesWithWait(boolean andClose) {
		try {
			boolean andWait = true;
			flushFiles(andClose, andWait);
		} catch (Exception e) {
			error("Failed to flush files.", e);
		}
	}
	
	protected boolean report (int reportIdx, Object...details) throws TermServerScriptException {
		boolean writeSuccess = writeToReportFile (reportIdx, writeToString(details));
		if (writeSuccess) {
			incrementSummaryInformation("Report lines written");
		}
		return writeSuccess;
	}
	
	protected boolean writeToReportFile(int reportIdx, String line) throws TermServerScriptException {
		if (getReportManager() == null) {
			throw new TermServerScriptException("Attempted to write to report before Report Manager is available. Check postInit() has been called.\n Message was " + line);
		}
		return getReportManager().writeToReportFile(reportIdx, line);
	}
	
	protected boolean writeToReportFile(String line) throws TermServerScriptException {
		return writeToReportFile(0, line);
	}
	
	protected String writeToString(Object[] details) {
		StringBuffer sb = new StringBuffer();
		boolean isFirst = true;
		for (Object detail : details) {
			if (detail == null) {
				detail = "";
			}
			if (detail instanceof Boolean) {
				detail = ((Boolean)detail)?"Y":"N";
			}
			boolean isNumeric = StringUtils.isNumeric(detail.toString()) || detail.toString().startsWith(QUOTE);
			String prefix = isFirst ? QUOTE : COMMA_QUOTE;
			if (isNumeric) {
				prefix = isFirst ? "" : COMMA;
			}
			if (detail instanceof String[]) {
				String[] arr = (String[]) detail;
				for (String str : arr) {
					boolean isNestedNumeric = false;
					if (str != null) {
						isNestedNumeric = StringUtils.isNumeric(str) || str.startsWith(QUOTE);
						str = isNestedNumeric ? str : str.replaceAll("\"", "\"\"");
					}
					sb.append((isNestedNumeric?"":prefix) + str + (isNestedNumeric?"":QUOTE));
					prefix = COMMA_QUOTE;
				}
			} else if (detail instanceof Object []) {
				addObjectArray(sb,detail, prefix, isNumeric);
			} else if (detail instanceof int[]) {
				prefix = isFirst ? "" : COMMA;
				boolean isNestedFirst = true;
				int[] arr = (int[]) detail;
				for (int i : arr) {
					sb.append(isNestedFirst?"":COMMA);
					sb.append(prefix + i );
					isNestedFirst = false;
				}
			} else if (detail instanceof String) {
				String str = (String) detail;
				str = isNumeric ? str : str.replaceAll("\"", "\"\"");
				sb.append(prefix + str + (isNumeric?"":QUOTE));
			} else {
				sb.append(prefix + detail + (isNumeric?"":QUOTE));
			}
			isFirst = false;
		}
		return sb.toString();
	}

	private void addObjectArray(StringBuffer sb, Object detail, String prefix, boolean isNumeric) {
		Object[] arr = (Object[]) detail;
		for (Object obj : arr) {
			if (obj instanceof String[] || obj instanceof Object[]) {
				addObjectArray(sb,obj, prefix, isNumeric);
			} else if (obj instanceof int[]) {
				for (int data : ((int[])obj)) {
					sb.append(COMMA + data);
				}
			} else {
				if (obj instanceof Boolean) {
					obj = ((Boolean)obj)?"Y":"N";
				}
				String data = (obj==null?"":obj.toString());
				data = isNumeric ? data : data.replaceAll("\"", "\"\"");
				sb.append(prefix + data + (isNumeric?"":QUOTE));
				prefix = COMMA_QUOTE;
			}
		}
	}

	public void postInit(String[] tabNames, String[] columnHeadings, boolean csvOutput) throws TermServerScriptException {
		if (!suppressOutput) {
			debug ("Initialising Report Manager");
			reportManager = ReportManager.create(this, getReportConfiguration());
			if (tabNames != null) {
				reportManager.setTabNames(tabNames);
			}
			if (csvOutput) {
				reportManager.setWriteToFile(true);
				reportManager.setWriteToSheet(false);
				reportManager.setWriteToS3(false);
			}
			
			getReportManager().initialiseReportFiles(columnHeadings);
			debug ("Report Manager initialisation complete");
		}
		
	}

	private ReportConfiguration getReportConfiguration() {
		return reportConfiguration;
	}
	
	protected void initialiseReportConfiguration(JobRun jobRun) {
		try {
			if (jobRun != null) {
				reportConfiguration = new ReportConfiguration(
						jobRun.getParamValue(REPORT_OUTPUT_TYPES),
						jobRun.getParamValue(REPORT_FORMAT_TYPE));
			}
		} catch (Exception e) {
			// In case of any error we don't care as this is not default for the reports.
		}

		// if it's not valid default to the the current mode of operation
		if (reportConfiguration == null || !reportConfiguration.isValid()) {
			info("Using default ReportConfiguration (Google/Sheet)...");
			reportConfiguration = new ReportConfiguration(
					ReportConfiguration.ReportOutputType.GOOGLE,
					ReportConfiguration.ReportFormatType.CSV);
		}
	}
	
}