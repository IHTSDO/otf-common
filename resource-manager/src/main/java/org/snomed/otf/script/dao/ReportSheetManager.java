package org.snomed.otf.script.dao;

import com.google.api.services.sheets.v4.model.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.GoogleClient;
import org.snomed.otf.script.utils.CVSUtils;
import org.springframework.context.ApplicationContext;

public class ReportSheetManager implements RF2Constants, ReportProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(ReportSheetManager.class);

	private static final String RAW = "RAW";
	private static final int MAX_REQUEST_RATE = 9;
	private static final int MAX_CELLS = 5000000 - 5000;
	private static final int MAX_ROW_INCREMENT = 10000;

	private Map<Integer, Integer> tabRowsCount;
	private Map<Integer, Integer> maxTabColumns;
	private int currentCellCount = 0;

	private final ReportManager owner;
	private GoogleClient googleClient;

	private Spreadsheet sheet;
	public static String targetFolderId = GFOLDER_FALLBACK;
	private Date lastWriteTime;
	private final List<ValueRange> dataToBeWritten = new ArrayList<>();
	private final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private Map<Integer, Integer> tabLineCount;
	private final Map<Integer, Integer> linesWrittenPerTab = new HashMap<>();
	private int numberOfSheets = 0;

	public ReportSheetManager(ReportManager owner) {
		this.owner = owner;
	}

	public static void setTargetFolderId(String targetFolderId) {
		ReportSheetManager.targetFolderId = targetFolderId;
	}

	private void init() throws TermServerScriptException {
		try {
			//Are we working in a Spring context?
			ApplicationContext appContext = owner.getScript().getApplicationContext();
			if (appContext == null) {
				googleClient = new GoogleClient();
				LOGGER.info("ReportSheetManager initializing in a locally run context");
			} else {
				googleClient = appContext.getBean(GoogleClient.class);
				LOGGER.info("ReportSheetManager initializing in a Spring context");
			}
			//Are we re-intialising?  Flush last data if so
			if (sheet != null) {
				flush();
			}
			googleClient.setupServices();
			sheet = googleClient.createSpreadSheet();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to initialise Google Sheets connection",e);
		}
	}



	private String constructReportTitle() {
		String titleStr = owner.getScript().getReportName() + " " + df.format(new Date()) + "_" + owner.getEnv();
		if (owner.getScript().getJobRun() != null && owner.getScript().getJobRun().getUser() != null) {
			titleStr += "_" + owner.getScript().getJobRun().getUser().toLowerCase();
		}
		return titleStr;
	}

	@Override
	public void initialiseReportFiles(String[] columnHeaders) throws TermServerScriptException {
		tabLineCount = new HashMap<>();
		init();
		try {
			List<Request> requests = new ArrayList<>();
			// Set the title of the spreadsheet
			requests.add(new Request()
					.setUpdateSpreadsheetProperties(new UpdateSpreadsheetPropertiesRequest()
							.setProperties(new SpreadsheetProperties()
									.setTitle(constructReportTitle()))
									.setFields("title")));
			tabRowsCount = new HashMap<>();
			populateColumnsPerTab(columnHeaders);
			int tabIdx = 0;
			for (String header : columnHeaders) {
				//Calculate the number of columns from the header, 
				//divide the total max cells by both columns and tab count
				int maxColumns = header.split(COMMA).length;
				int maxRows = MAX_ROW_INCREMENT ;
				tabRowsCount.put(tabIdx, maxRows);
				currentCellCount += maxColumns * maxRows;
				LOGGER.debug("Tab {} rows/cols set to {}/{}",tabIdx, maxRows, maxColumns);

				String sheetTitle = owner.getTabNames().get(tabIdx);

				GridProperties gridProperties = new GridProperties()
						.setRowCount(maxRows)
						.setColumnCount(maxColumns)
						.setFrozenRowCount(1);

				SheetProperties sheetProperties = new SheetProperties()
						.setSheetId(tabIdx)
						.setTitle(sheetTitle)
						.setGridProperties(gridProperties);

				if (tabIdx == 0) {
					// Sheet 0 already exists, just update it
					requests.add(new Request()
							.setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
									.setProperties(sheetProperties)
									.setFields("title, gridProperties")));
				} else {
					// Add a new sheet
					requests.add(new Request()
							.setAddSheet(new AddSheetRequest()
									.setProperties(sheetProperties)));
				}

				// Set text format and background colour of the header line
				requests.add(new Request()
						.setRepeatCell(new RepeatCellRequest()
								.setRange(new GridRange()
										.setSheetId(tabIdx)
										.setStartRowIndex(0)
										.setEndRowIndex(1))
								.setCell(new CellData()
										.setUserEnteredFormat(new CellFormat()
												.setTextFormat(new TextFormat()
														.setBold(true))
												.setBackgroundColor(new Color()
														.setRed(0.93f)
														.setGreen(0.93f)
														.setBlue(0.93f))))
								.setFields("userEnteredFormat(textFormat,backgroundColor)")));

				// Format fixed width columns
				String[] columnWidths = owner.getScript().getColumnWidths();
				if (columnWidths != null) {

					String[] widths = columnWidths[tabIdx].split(COMMA);

					for (int i = 0; i < maxColumns; i++) {
						Integer columnWidth = Integer.valueOf(widths[i].trim());
						if (columnWidth > 0) {
							requests.add(new Request()
									.setRepeatCell(new RepeatCellRequest()
											.setRange(new GridRange()
													.setSheetId(tabIdx)
													.setStartRowIndex(0)
													.setEndRowIndex(1)
													.setStartColumnIndex(i)
													.setEndColumnIndex(i + 1))
											.setCell(new CellData()
													.setUserEnteredFormat(new CellFormat()
															.setWrapStrategy("WRAP")))
											.setFields("userEnteredFormat(wrapStrategy)")));
							requests.add(new Request()
									.setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
											.setRange(new DimensionRange()
													.setSheetId(tabIdx)
													.setDimension("COLUMNS")
													.setStartIndex(i)
													.setEndIndex(i + 1))
											.setProperties(new DimensionProperties()
													.setPixelSize(columnWidth))
											.setFields("pixelSize")));
						}
					}
				}

				writeToReportFile(tabIdx, header, true);
				tabIdx++;
				numberOfSheets++;
			}
			
			googleClient.executeRequests(sheet, requests, true);
			flush();
			googleClient.moveFile(sheet.getSpreadsheetId(), targetFolderId);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			throw new TermServerScriptException ("Unable to initialise Google Sheet headers", e);
		}
	}

	private void populateColumnsPerTab(String[] columnHeaders) {
		maxTabColumns = new HashMap<>();
		for (int tabIdx = 0; tabIdx < columnHeaders.length; tabIdx++) {
			int tabColumns = columnHeaders[tabIdx].split(COMMA).length;
			maxTabColumns.put(tabIdx, tabColumns);
		}
	}

	@Override
	public boolean writeToReportFile(int tabIdx, String line, boolean delayWrite) throws TermServerScriptException {
		if (linesWrittenPerTab.get(tabIdx) != null && linesWrittenPerTab.get(tabIdx) >= tabRowsCount.get(tabIdx)) {
			//We're already hit the max and will have reported failure
			//Do not attempt to add any more data.
			return false;
		}
		
		if (lastWriteTime == null) {
			lastWriteTime = new Date();
		}
		
		addDataToBWritten(tabIdx, line);
		
		//Are we getting close to the limits of what can be written?
		if (dataToBeWritten.size() > 2000) {
			LOGGER.info("Attempting to write > 2000 rows to sheets, pausing...");
			try { Thread.sleep(MAX_REQUEST_RATE*1000L); } catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		
		//Do we have a total count for this tab?  Save 3 to allow for Data truncated message
		int maxRows = tabRowsCount.get(tabIdx);
		linesWrittenPerTab.merge(tabIdx, 1, Integer::sum);
		
		if (linesWrittenPerTab.get(tabIdx) >= (maxRows-1)) {
			//Do we have sufficient cells left to extend this tab further?
			int requestCellIncrement = maxTabColumns.get(tabIdx) * MAX_ROW_INCREMENT;
			if (currentCellCount + requestCellIncrement < MAX_CELLS) {
				currentCellCount += requestCellIncrement;
				tabRowsCount.put(tabIdx, maxRows + MAX_ROW_INCREMENT);
				LOGGER.debug("Expanding rows in tabIdx {} to {}", tabIdx, tabRowsCount.get(tabIdx));
				expandTabRows(tabIdx);
			} else if (linesWrittenPerTab.get(tabIdx) == (maxRows - 1)) {
				linesWrittenPerTab.merge(tabIdx, 1, Integer::sum);
				addDataToBWritten(tabIdx, "Data truncated at " + maxRows + " rows");
				LOGGER.warn("Number of rows written to tab idx {} hit limit of {}", tabIdx, maxRows);
			}
			flushWithWait();
			return false;
		}
		
		if (!delayWrite) {
			flushSoft();
		}
		return true;
	}
	
	private void expandTabRows(int tabIdx) throws TermServerScriptException {
		AppendDimensionRequest dimRequest = new AppendDimensionRequest();
		dimRequest.setDimension("ROWS")
			.setSheetId(tabIdx)
			.setLength(MAX_ROW_INCREMENT);
		
		Request request = new Request();
		request.setAppendDimension(dimRequest);
		try {
			googleClient.executeRequests(sheet, Collections.singletonList(request), true);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			throw new TermServerScriptException("Unable to expand rows in tabIdx: " + tabIdx, e);
		}
		
	}

	private void addDataToBWritten(int tabIdx, String line) throws TermServerScriptException {
		List<Object> data = CVSUtils.csvSplitAsObject(line);
		List<List<Object>> cells = List.of(data);
		//Increment the current row position so we create the correct range
		tabLineCount.merge(tabIdx, 1, Integer::sum);
		if (!maxTabColumns.containsKey(tabIdx)) {
			throw new TermServerScriptException("Attempt to write to sheet " + tabIdx + " but sheet not known to SheetManager. Check list of tab names initialised");
		}
		int maxColumn = maxTabColumns.get(tabIdx);
		String beyondZ = "";
		int alphabetsPassed = maxColumn / 26;
		if (alphabetsPassed > 0) {
			beyondZ = Character.toString((char)('A' + alphabetsPassed - 1));
			maxColumn %= 26;
		}
		String maxColumnStr = beyondZ + Character.toString((char)('A' + maxColumn));
		String range = "'" + owner.getTabNames().get(tabIdx) + "'!A" + tabLineCount.get(tabIdx) + ":" + maxColumnStr +  tabLineCount.get(tabIdx); 
		dataToBeWritten.add(new ValueRange()
					.setRange(range)
					.setValues(cells));
		
	}

	public void flushWithWait() throws TermServerScriptException {
		flush(false, true);  //Not optional, also wait
	}
	
	public void flush() throws TermServerScriptException {
		flush(false, false);  //Not optional, don't wait
	}

	public void flushSoft() throws TermServerScriptException {
		flush(true, false); //optional, don't wait
	}

	private synchronized void flush(boolean optional, boolean withWait) throws TermServerScriptException {
		//Are we ready to flush?
		//How long is it since we last wrote to the file?  Write every 5 seconds
		if (lastWriteTime != null) {
			long secondsSinceLastWrite = (System.currentTimeMillis() - lastWriteTime.getTime()) / 1000;
			long remaining = MAX_REQUEST_RATE - secondsSinceLastWrite;
			if (remaining > 0) {
				if (withWait) {
					try {
						Thread.sleep(remaining * 1000); // convert to ms if MAX_REQUEST_RATE is in seconds
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				} else if (optional) {
					return;
				}
			}
		}
		
		//Execute update of data values
		if (sheet != null) {
			BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
				.setValueInputOption(RAW)
				.setData(dataToBeWritten);
			try {
				LOGGER.info("{} flushing to sheets", new Date());
				googleClient.writeToSheet(sheet, body);
			} catch (IOException e) {
				//Output some sample of the data so get an idea of where it's coming from.
				int dataCutPoint = Math.min(dataToBeWritten.size(), 1000);
				LOGGER.info("Last data attempted: {}", dataToBeWritten.toString().substring(0, dataCutPoint));
				throw new TermServerScriptException("Unable to update spreadsheet " + sheet.getSpreadsheetUrl(), e);
			} finally {
				lastWriteTime = new Date();
				dataToBeWritten.clear();
			}
		}
	}

	public String getUrl() {
		return sheet == null ? null : sheet.getSpreadsheetUrl();
	}

	public void formatSpreadSheetColumns() {
		List<Request> requests = new ArrayList<>();
		String[] columnWidths = owner.getScript().getColumnWidths();

		for (int tabIdx = 0; tabIdx < numberOfSheets; tabIdx++) {
			int maxColumn = maxTabColumns.get(tabIdx);
			String[] widths = (columnWidths != null ? columnWidths[tabIdx].split(COMMA) : null);

			for (int columnIdx = 0; columnIdx < maxColumn; columnIdx++) {
				// Set the columns without fixed width to be auto sized
				if (widths == null || Integer.parseInt(widths[columnIdx].trim()) <= 0) {
					DimensionRange dimensionRange = new DimensionRange();
					dimensionRange.setDimension("COLUMNS");
					dimensionRange.setSheetId(tabIdx);
					dimensionRange.setStartIndex(columnIdx);
					dimensionRange.setEndIndex(columnIdx + 1);

					requests.add(new Request()
							.setAutoResizeDimensions(new AutoResizeDimensionsRequest()
									.setDimensions(dimensionRange)));
				}
			}
		}

		LOGGER.info("Formatting Google SpreadSheet Sheet/s (Columns to auto size).");
		new Thread(() -> {
			try {
				googleClient.executeRequests(sheet, requests, false);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				LOGGER.error("Column size formatting attempt failed. Don't care.", e);
			}
		}).start();
	}
}
