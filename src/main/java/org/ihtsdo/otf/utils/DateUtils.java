package org.ihtsdo.otf.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtils {
	
	public static final String DATE_SEPARATOR = "-";

	public static final String YYYYMMDD = "yyyyMMdd";

	public static final String YYYYMMDD_HHMMSS = "yyyyMMdd_HHmmss";

	public static String formatAsISO (String dateAsYYYYMMDD) {
		if (dateAsYYYYMMDD == null || dateAsYYYYMMDD.length() != 8) {
			throw new NumberFormatException ("Date '" + dateAsYYYYMMDD + "' cannot be formatted as ISO YYYY-MM-DD");
		}
		StringBuffer buff = new StringBuffer();
		buff.append(dateAsYYYYMMDD.substring(0, 4))
			.append(DATE_SEPARATOR)
			.append(dateAsYYYYMMDD.substring(4, 6))
			.append(DATE_SEPARATOR)
			.append(dateAsYYYYMMDD.substring(6));
		return buff.toString();
	}

	public static String today(String formatStr) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat format = new SimpleDateFormat(formatStr);
		return format.format(cal.getTime());
	}
}
