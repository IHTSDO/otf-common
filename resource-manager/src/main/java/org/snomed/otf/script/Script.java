package org.snomed.otf.script;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.snomed.otf.script.dao.DataBroker;
import org.snomed.otf.script.dao.RF2Manager;
import org.snomed.otf.script.dao.ReportConfiguration;
import org.snomed.otf.script.dao.ReportManager;

public abstract class Script implements RF2Constants {
	
	protected static final String REPORT_OUTPUT_TYPES = "ReportOutputTypes";
	protected static final String REPORT_FORMAT_TYPE = "ReportFormatType";
	
	static Logger sLogger = LoggerFactory.getLogger(Script.class);
	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public static List<String> HISTORICAL_REFSETS = new ArrayList<>();
	static {
		HISTORICAL_REFSETS.add(SCTID_ASSOC_WAS_A_REFSETID);
		HISTORICAL_REFSETS.add(SCTID_ASSOC_REPLACED_BY_REFSETID);
		HISTORICAL_REFSETS.add(SCTID_ASSOC_POSS_REPLACED_BY_REFSETID);
		HISTORICAL_REFSETS.add(SCTID_ASSOC_POSS_EQUIV_REFSETID);
		HISTORICAL_REFSETS.add(SCTID_ASSOC_SAME_AS_REFSETID);
		HISTORICAL_REFSETS.add(SCTID_ASSOC_PART_EQUIV_REFSETID);
		HISTORICAL_REFSETS.add(SCTID_ASSOC_MOVED_TO_REFSETID);
		HISTORICAL_REFSETS.add(SCTID_ASSOC_ALTERNATIVE_REFSETID);
		HISTORICAL_REFSETS.add(SCTID_ASSOC_WAS_A_REFSETID);
	};
	
	protected ReportManager reportManager;
	protected RF2Manager rf2Manager;
	protected Project project;
	protected String taskKey;
	protected Date startTime;
	protected Map<String, Object> summaryDetails = new TreeMap<>();
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

	/**
	 * These methods are used when we want something to appear in STDOUT but not as 
	 * a log entry.  For example, menu options for user interaction, or - particularly
	 * in the case of full stops to indicate progress, where we don't want a new line 
	 * per update.
	 */
	public static void print(Object msg) {
		System.out.print(msg.toString());
	}

	public static void println (Object msg) {
		System.out.println(msg.toString());
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

	public DataBroker getReportDataUploader() throws TermServerScriptException {
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
	
	public String getTaskKey() {
		return taskKey;
	}

	public void setTaskKey(String taskKey) {
		this.taskKey = taskKey;
	}

	public void incrementSummaryInformationQuiet(String key) {
		//There are occasions where we can only capture all information when doing the first pass
		//When we're looking at ALL information eg which concepts do not require changes.
		if (quiet) {
			incrementSummaryInformation(key, 1);
		}
	}
	
	public void initialiseSummaryInformation(String key) {
		summaryDetails.put(key, 0);
	}
	
	public void incrementSummaryInformation(String key, int incrementAmount) {
		if (!summaryDetails.containsKey(key)) {
			summaryDetails.put(key, 0);
		}
		int newValue = (Integer) summaryDetails.get(key) + incrementAmount;
		summaryDetails.put(key, newValue);
	}
	
	public void flushFilesSoft() throws TermServerScriptException {
		getReportManager().flushFilesSoft();
	}
	
	public void flushFiles(boolean andClose) throws TermServerScriptException {
		if (getRF2Manager() != null) {
			getRF2Manager().flushFiles(andClose);
		}
		if (getReportManager() != null) {
			getReportManager().flushFiles(andClose);
		}
	}
	
	public void flushFilesSafely(boolean andClose) {
		try {
			flushFiles(andClose);
		} catch (Exception e) {
			error("Failed to flush files.", e);
		}
	}
	
	public void flushFilesWithWait(boolean andClose) {
		try {
			flushFiles(andClose);
		} catch (Exception e) {
			error("Failed to flush files.", e);
		}
	}
	
	public boolean report (int reportIdx, Object...details) throws TermServerScriptException {
		boolean writeSuccess = writeToReportFile(reportIdx, writeToString(details));
		if (writeSuccess) {
			incrementSummaryInformation("Report lines written");
		}
		return writeSuccess;
	}
	
	protected boolean writeToReportFile(int reportIdx, String line) throws TermServerScriptException {
		if (getReportManager() == null) {
			throw new TermServerScriptException("Attempted to write to report before Report Manager is available. Check postInit() has been called.\n Message was " + line);
		}
		
		if (reportIdx == NOT_SET) {
			debug("Tab NOT_SET to report: " + line);
			return false;
		}
		return getReportManager().writeToReportFile(reportIdx, line);
	}
	
	protected boolean writeToReportFile(String line) throws TermServerScriptException {
		return writeToReportFile(0, line);
	}
	
	protected String writeToString(Object[] details) {
		StringBuilder sb = new StringBuilder();
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
					boolean isAlreadyQuoted = (str != null) && str.startsWith("\"") && str.endsWith("\"");
					if (isAlreadyQuoted) {
						prefix = "";
					}
					if (str != null) {
						isNestedNumeric = StringUtils.isNumeric(str);
						str = (isNestedNumeric || isAlreadyQuoted) ? str : str.replaceAll("\"", "\"\"");
					} else {
						str = "";
					}
					sb.append((isNestedNumeric && !isFirst) ? "," : prefix).append(str).append((isNestedNumeric || isAlreadyQuoted) ? "" : QUOTE);
					prefix = COMMA_QUOTE;
				}
			} else if (detail instanceof Object []) {
				addObjectArray(sb,detail, prefix, isNumeric);
			} else if (detail instanceof int[] arr) {
				prefix = isFirst ? "" : COMMA;
				boolean isNestedFirst = true;
				for (int i : arr) {
					sb.append(isNestedFirst?"":COMMA);
					sb.append(prefix).append(i);
					isNestedFirst = false;
					prefix = "";
				}
			} else if (detail instanceof String str) {
				str = isNumeric ? str : str.replaceAll("\"", "\"\"");
				sb.append(prefix).append(str).append(isNumeric ? "" : QUOTE);
			} else {
				sb.append(prefix).append(detail).append(isNumeric ? "" : QUOTE);
			}
			isFirst = false;
		}
		return sb.toString();
	}

	private void addObjectArray(StringBuilder sb, Object detail, String prefix, boolean isNumeric) {
		Object[] arr = (Object[]) detail;
		for (Object obj : arr) {
			if (obj instanceof Object[]) {
				addObjectArray(sb,obj, prefix, isNumeric);
			} else if (obj instanceof int[]) {
				for (int data : ((int[])obj)) {
					sb.append(COMMA).append(data);
				}
			} else {
				if (obj instanceof Boolean) {
					obj = ((Boolean)obj)?"Y":"N";
				}
				String data = (obj==null?"":obj.toString());
				data = isNumeric ? data : data.replaceAll("\"", "\"\"");
				sb.append(prefix).append(data).append(isNumeric ? "" : QUOTE);
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
