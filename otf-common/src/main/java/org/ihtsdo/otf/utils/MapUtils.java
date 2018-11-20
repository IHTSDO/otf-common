package org.ihtsdo.otf.utils;

import java.util.Map;
import java.util.Map.Entry;

//TODO When this class is mature, move it to OTF Common
public class MapUtils {
	
	public static String SEPARATOR = ": ";
	public static String NEWLINE = "\n";

	public static String toString(Map<String, String> map) {
		StringBuffer response = new StringBuffer();
		boolean isFirstEntry = true;
		for (Entry<String, String> entry : map.entrySet()) {
			if (!isFirstEntry) {
				response.append(NEWLINE);
			}
			response.append(entry.getKey());
			response.append(SEPARATOR);
			response.append(entry.getValue());
			isFirstEntry = false;
		}
		return response.toString();
	}
}
