package org.ihtsdo.otf.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
	
	public static final String DATE_SEPARATOR = "-";

	public static final String YYYYMMDD = "yyyyMMdd";

	public static final String YYYYMMDD_HHMMSS = "yyyyMMdd_HHmmss";

	public static String formatAsISO (String dateAsYYYYMMDD) {
		if (dateAsYYYYMMDD == null || dateAsYYYYMMDD.length() != 8) {
			throw new NumberFormatException ("Date '" + dateAsYYYYMMDD + "' cannot be formatted as ISO YYYY-MM-DD");
		}
		StringBuilder buff = new StringBuilder();
		buff.append(dateAsYYYYMMDD.substring(0, 4))
			.append(DATE_SEPARATOR)
			.append(dateAsYYYYMMDD.substring(4, 6))
			.append(DATE_SEPARATOR)
			.append(dateAsYYYYMMDD.substring(6));
		return buff.toString();
	}

	public static String now(String formatStr) {
		return new SimpleDateFormat(formatStr).format(new Date());
	}

	public static String getCurrentMonthName() {
		return LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
	}
}
