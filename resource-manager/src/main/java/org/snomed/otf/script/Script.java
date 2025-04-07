package org.snomed.otf.script;

import java.util.*;

import org.apache.commons.lang3.NotImplementedException;
import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Component;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Project;
import org.ihtsdo.otf.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.scheduler.domain.JobRun;
import org.snomed.otf.script.dao.DataBroker;
import org.snomed.otf.script.dao.RF2Manager;
import org.snomed.otf.script.dao.ReportConfiguration;
import org.snomed.otf.script.dao.ReportManager;
import org.springframework.context.ApplicationContext;

public abstract class Script implements RF2Constants {

	private static final Logger LOGGER = LoggerFactory.getLogger(Script.class);

	private static final String DEFAULT_CATEGORY = "Default Category";

	protected static final String REPORT_OUTPUT_TYPES = "ReportOutputTypes";
	protected static final String REPORT_FORMAT_TYPE = "ReportFormatType";
	protected static final List<String> HISTORICAL_REFSETS = new ArrayList<>();
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
	}
	
	protected ReportManager reportManager;
	protected RF2Manager rf2Manager;
	protected Project project;
	protected String taskKey;
	protected Date startTime;

	private  Map<String, Map<String, Integer>> summaryCountsByCategory = new HashMap<>();

	protected boolean quiet = false;
	protected boolean suppressOutput = false;
	protected ReportConfiguration reportConfiguration;
	protected ApplicationContext appContext;

	/**
	 * Direct access to STDOUT without LOGGER, for cmd line menus
	 */
	public static void print(Object msg) {
		System.out.print(msg);
	}

	/**
	 * Direct access to STDOUT (with linefeed) without LOGGER, for cmd line menus
	 */
	public static void println (Object msg) {
		System.out.println(msg);
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

	public String[] getColumnWidths() {
		// Fixed column widths should be provided by individual reports (optional).
		// Returning null means all columns will be auto-resized.
		return null;
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

	public static boolean isHistoricalRefset(String refsetId) {
		return HISTORICAL_REFSETS.contains(refsetId);
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
	
	public void flushFilesWithWait(boolean andClose) {
		try {
			flushFiles(andClose);
		} catch (Exception e) {
			LOGGER.error("Failed to flush files.", e);
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
			LOGGER.debug("Tab NOT_SET to report: {}", line);
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
			if (detail instanceof Boolean booleanDetail) {
				detail = Boolean.TRUE.equals(booleanDetail)?"Y":"N";
			}
			boolean isNumeric = StringUtils.isNumeric(detail.toString()) || detail.toString().startsWith(QUOTE);
			String prefix = isFirst ? QUOTE : COMMA_QUOTE;
			if (isNumeric) {
				prefix = isFirst ? "" : COMMA;
			}
			if (detail instanceof String[] stringArrayDetail) {
				for (String str : stringArrayDetail) {
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
			} else if (obj instanceof int[] intArr) {
				for (int data : intArr) {
					sb.append(COMMA).append(data);
				}
			} else {
				if (obj instanceof Boolean bool) {
					obj = Boolean.TRUE.equals(bool)?"Y":"N";
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
			LOGGER.debug("Initialising Report Manager");
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
			LOGGER.debug("Report Manager initialisation complete");
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

		// if it's not valid, default to the current mode of operation
		if (reportConfiguration == null || !reportConfiguration.isValid()) {
			LOGGER.info("Using default ReportConfiguration (Google/Sheet)...");
			reportConfiguration = new ReportConfiguration(
					ReportConfiguration.ReportOutputType.GOOGLE,
					ReportConfiguration.ReportFormatType.CSV);
		}
	}

	public ApplicationContext getApplicationContext() {
		return appContext;
	}

	public void incrementSummaryCount(String category, String summaryItem) {
		incrementSummaryCount(category, summaryItem, 1);
	}

	public void incrementSummaryCount(String summaryItem) {
		incrementSummaryCount(DEFAULT_CATEGORY, summaryItem, 1);
	}

	public void incrementSummaryCount(String summaryItem, int increment) {
		incrementSummaryCount(DEFAULT_CATEGORY, summaryItem, increment);
	}

	public void addSummaryInformation(String item, Object detail) {
		LOGGER.info("{}: {}", item, detail);
		Map<String, Integer> summaryCounts = summaryCountsByCategory.computeIfAbsent(item, k -> new HashMap<>());
		summaryCounts.put((String)detail, NOT_SET);
	}

	public void setSummaryInformation(String item, Object detail) {
		addSummaryInformation(item, detail);
	}

	public Integer getSummaryCount(String key) {
		return getSummaryInformationInt(key);
	}

	public int getSummaryInformationInt(String key) {
		Map<String, Integer> summaryCounts = summaryCountsByCategory.computeIfAbsent(DEFAULT_CATEGORY, k -> new HashMap<>());
		return summaryCounts.getOrDefault(key,0);
	}

	public String getTaskKey() {
		return taskKey;
	}

	public void incrementSummaryInformationQuiet(String key) {
		//There are occasions where we can only capture all information when doing the first pass
		//When we're looking at ALL information eg which concepts do not require changes.
		if (quiet) {
			incrementSummaryInformation(key, 1);
		}
	}

	public void initialiseSummaryInformation(String key) {
		Map<String, Integer> summaryCounts = summaryCountsByCategory.computeIfAbsent(DEFAULT_CATEGORY, k -> new HashMap<>());
		summaryCounts.put(key, 0);
	}

	public Map<String, Integer> getSummaryDetails() {
		return summaryCountsByCategory.computeIfAbsent(DEFAULT_CATEGORY, k -> new HashMap<>());
	}

	public void incrementSummaryInformation(String key, int incrementAmount) {
		incrementSummaryCount(DEFAULT_CATEGORY, key, incrementAmount);
	}

	protected boolean summaryContainsKey(String key) {
		Map<String, Integer> defaultSummaryCounts = summaryCountsByCategory.computeIfAbsent(DEFAULT_CATEGORY, k -> new HashMap<>());
		return defaultSummaryCounts.containsKey(key);
	}

	public void incrementSummaryCount(String category, String summaryItem, int increment) {
		//Increment the count for this summary item, in the appropriate category
		Map<String, Integer> summaryCounts = summaryCountsByCategory.computeIfAbsent(category, k -> new HashMap<>());
		summaryCounts.merge(summaryItem, increment, Integer::sum);
	}

	public void incrementSummaryInformation(String key) {
		if (!quiet) {
			incrementSummaryCount(DEFAULT_CATEGORY, key, 1);
		}
	}

	public void populateSummaryTab(int tabIdx) throws TermServerScriptException {
		populateSummaryTabAndTotal(tabIdx);
	}

	protected void initialiseSummary(String issue) {
		Map<String, Integer> summaryCounts = summaryCountsByCategory.computeIfAbsent(DEFAULT_CATEGORY, k -> new HashMap<>());
		summaryCounts.put(issue, 0);
	}

	protected void populateSummaryTabAndTotal(int tabIdx) {
		//Do we only have one category?  No need to report the category in that case
		boolean isSingleCategory  = summaryCountsByCategory.size() == 1;
		for (Map.Entry<String, Map<String, Integer>> entry : summaryCountsByCategory.entrySet()) {
			if (!isSingleCategory) {
				reportSafely(tabIdx, entry.getKey());
			}
			entry.getValue().entrySet().stream()
					.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
					.forEach(e -> reportSafely(tabIdx, (Component) null, e.getKey(), e.getValue()));

			int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
			reportSafely(tabIdx, (Component) null, "TOTAL", total);
		}
	}

	protected void reportSummaryCounts(int summaryTabIdx) throws TermServerScriptException {
		report(summaryTabIdx, "");
		//Work through each category (sorted) and then output each summary Count for that category
		summaryCountsByCategory.keySet().stream()
				.sorted()
				.forEach(cat -> {
					reportSafely(summaryTabIdx, cat);
					Map<String, Integer> summaryCounts = summaryCountsByCategory.get(cat);
					summaryCounts.keySet().stream()
							.sorted()
							.forEach(summaryItem -> reportSafely(summaryTabIdx, "", summaryItem, summaryCounts.get(summaryItem)));
				});
	}
}
