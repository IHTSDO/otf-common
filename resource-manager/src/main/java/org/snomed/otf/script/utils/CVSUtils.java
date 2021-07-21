package org.snomed.otf.script.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;

public class CVSUtils {

	private static Pattern csvPattern = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

	public static List<String> csvSplit(String line) {
		List<String> list = new ArrayList<>();
		for(String item : Splitter.on(csvPattern).split(line)) {
			//Trim leading and trailing double quotes - not needed as already split
			if (line.charAt(0)=='"') {
				item = item.substring(1,item.length()-1);
			}
			list.add(item);
		}
		return list;
	}

	public static List<Object> csvSplitAsObject(String line) {
		List<Object> list = new ArrayList<>();
		for(String item : Splitter.on(csvPattern).split(line)) {
			//Trim leading and trailing double quotes - not needed as already split
			if (item.length() > 0) {
				if (item.charAt(0)=='"') {
					item = item.substring(1,item.length()-1);
				}
			} else {
				item = "";
			}
			// This is required as valid CSV will have "" for a quote
			// i.e "F1", "F2", "F-""sometect""-END"
			item = item.replaceAll("\"\"", "\"");
			list.add(item);
		}
		return list;
	}

}
