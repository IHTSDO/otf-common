package org.snomed.otf.script.dao;

import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.ihtsdo.otf.utils.SnomedUtilsBase;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CommonFileWriter implements RF2Constants {

	protected Map<String, PrintWriter> printWriterMap = new HashMap<>();

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

	public void flushFiles(boolean andClose) throws TermServerScriptException {
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
					//Well, we tried
				}
			}
		}
		if (andClose) {
			printWriterMap = new HashMap<>();
		}
	}

}
