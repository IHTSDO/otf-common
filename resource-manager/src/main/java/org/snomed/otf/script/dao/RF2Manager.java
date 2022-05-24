package org.snomed.otf.script.dao;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.ihtsdo.otf.utils.SnomedUtils;
import org.snomed.otf.script.Script;

public class RF2Manager implements RF2Constants {
	
	protected File[] reportFiles;
	protected Map<String, PrintWriter> printWriterMap = new HashMap<>();
	protected String currentTimeStamp;
	
	PrintWriter getPrintWriter(String fileName) throws TermServerScriptException {
		try {
			PrintWriter pw = printWriterMap.get(fileName);
			if (pw == null) {
				File file = SnomedUtils.ensureFileExists(fileName);
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
		for (PrintWriter pw : printWriterMap.values()) {
			try {
				pw.flush();
				if (andClose) {
					pw.close();
				}
			} catch (Exception e) {}
		}
		if (andClose) {
			printWriterMap = new HashMap<>();
		}
	}

	public void writeToRF2File(String fileName, Object[] columns) throws TermServerScriptException {
		StringBuffer line = new StringBuffer();
		for (int x=0; x<columns.length; x++) {
			if (x > 0) {
				line.append(TSV_FIELD_DELIMITER);
			}
			line.append(columns[x]==null?"":columns[x]);
		}
		writeToRF2File(fileName, line.toString());
	}
	
	public void writeToRF2File(String fileName, String line) throws TermServerScriptException {
		PrintWriter out = getPrintWriter(fileName);
		try {
			out.print(line + LINE_DELIMITER);
		} catch (Exception e) {
			Script.info ("Unable to output report rf2 line due to " + e.getMessage());
		}
	}
	
	public Map<String, PrintWriter> getPrintWriterMap() {
		return printWriterMap;
	}

	public void setPrintWriterMap(Map<String, PrintWriter> printWriterMap) {
		this.printWriterMap = printWriterMap;
	}
}
