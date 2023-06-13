package org.ihtsdo.otf.utils;

import java.io.File;
import java.io.IOException;

import org.ihtsdo.otf.exception.TermServerScriptException;
import org.ihtsdo.otf.RF2Constants;

public class SnomedUtils implements RF2Constants {

	public static File ensureFileExists(String fileName) throws TermServerScriptException {
		if (StringUtils.isEmpty(fileName)) {
			throw new TermServerScriptException ("Blank file name specified");
		}
		
		File file = new File(fileName);
		try {
			if (!file.exists()) {
				if (file.getParentFile() != null) {
					file.getParentFile().mkdirs();
				}
				file.createNewFile();
			}
		} catch (IOException e) {
			throw new TermServerScriptException("Failed to create file " + fileName,e);
		}
		return file;
	}
	
	public static String[] deconstructFSN(String fsn) {
		return deconstructFSN(fsn, false);  //Noisy by default
	}

	public static String[] deconstructFSN(String fsn, boolean quiet) {
		String[] elements = new String[2];
		if (fsn == null) {
			return elements;
		}
		int cutPoint = fsn.lastIndexOf(SEMANTIC_TAG_START);
		if (cutPoint == -1) {
			if (!quiet) {
				System.out.println("'" + fsn + "' does not contain a semantic tag!");
			}
			elements[0] = fsn;
		} else {
			elements[0] = fsn.substring(0, cutPoint).trim();
			elements[1] = fsn.substring(cutPoint);
		}
		return elements;
	}
	
	public static boolean isConceptSctid(String componentId) {
		//A zero in the penultimate character indicates a concept SCTID
		if (StringUtils.isEmpty(componentId)) {
			return false;
		}
		return componentId.charAt(componentId.length()-2) == '0';
	}

	public static String getNamespace(String sctId) {
		//We'll reverse the string to make the calculations easier
		String revSctId = new StringBuffer(sctId).reverse().toString();
		if (revSctId.charAt(2) == '0') {
			return "0";
		}
		if (revSctId.length() < 11) {
			throw new IllegalArgumentException("Non 0 partition SCTID missing namespace: " + sctId);
		}
		String revNameSpace = revSctId.substring(3,10);
		return new StringBuffer(revNameSpace).reverse().toString();
	}
}
