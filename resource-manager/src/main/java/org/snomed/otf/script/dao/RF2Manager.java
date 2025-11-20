package org.snomed.otf.script.dao;

import java.io.*;

import org.ihtsdo.otf.exception.TermServerScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RF2Manager extends CommonFileWriter {

	private String patchEffectiveTime = null;

	private final Logger logger = LoggerFactory.getLogger(RF2Manager.class);

	public void writeToRF2File(String fileName, Object[] columns) throws TermServerScriptException {
		StringBuilder line = new StringBuilder();
		for (int x=0; x<columns.length; x++) {
			String field = columns[x]==null?"":columns[x].toString();
			if (x > 0) {
				line.append(TSV_FIELD_DELIMITER);
			}
			if (patchEffectiveTime != null && x == IDX_EFFECTIVETIME) {
				field = patchEffectiveTime;
			}
			line.append(field);
		}
		writeToRF2File(fileName, line.toString());
	}
	
	public void writeToRF2File(String fileName, String line) throws TermServerScriptException {
		PrintWriter out = getPrintWriter(fileName);
		try {
			out.print(line + LINE_DELIMITER);
		} catch (Exception e) {
			logger.info("Unable to output report rf2 line due to {}", e.getMessage());
		}
	}

	public void setPatchEffectiveTime(String patchEffectiveTime) {
		this.patchEffectiveTime = patchEffectiveTime;
	}

}
