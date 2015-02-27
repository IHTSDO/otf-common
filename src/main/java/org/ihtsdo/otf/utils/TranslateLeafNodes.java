package org.ihtsdo.otf.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class TranslateLeafNodes {

	private static String newLang;
	private static boolean nullTranslate = false;

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			echo("Usage TranslateLeafNodes <json ENGLISH filename> <output language> [-null]");
			echo("The null flag will recreate the file without trasnslation for consistency in ordering");
			System.exit(1);
		}

		if (args.length == 3 && args[2].equalsIgnoreCase("-null")) {
			nullTranslate = true;
		}
		String enFileName = args[0];
		newLang = args[1];
		File enFile = new File(enFileName);
		if (enFile.exists() && !enFile.isDirectory()) {
			String parentPath = FilenameUtils.getFullPathNoEndSeparator(enFile.getAbsolutePath());
			String newFilename = parentPath + File.separator + newLang + ".json";
			echo("Translating " + enFileName + " into " + newLang);
			echo("Saving as " + newFilename);

			String jsonStr = FileUtils.readFileToString(enFile);
			// json.org does not preseve order. We'll dip into JSON to obtain an ordered map
			// JsonParser parser = new JsonParser();
			// java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map>() {
			// }.getType();
			// Map jsonMap = new Gson().fromJson(jsonStr, type);

			JSONObject json = new JSONObject(jsonStr);
			translateJSONObject(json);

			FileWriter file = new FileWriter(newFilename);
			String tidiedString = tidy(json.toString());
			file.write(tidiedString);
			file.close();
		} else {
			echo("Unable to find " + enFileName);
			System.exit(2);
		}

	}

	private static void translateObject(JSONObject parent, String key, Object data) throws Exception {
		if (data instanceof JSONObject) {
			translateJSONObject((JSONObject) data);
		} else if (data instanceof JSONArray) {
			translateJSONArray(parent, (JSONArray) data);
		} else {
			String translation = translateLeaf(data.toString());
			// Replace the original with the translation
			parent.put(key, translation);
		}
	}

	private static void translateJSONObject(JSONObject parent) throws Exception {
		Iterator it = parent.keys();
		while (it.hasNext()) {
			String key = (String) it.next();
			Object child = parent.get(key);
			translateObject(parent, key, child);
		}
	}

	private static void translateJSONArray(JSONObject parent, JSONArray json) throws Exception {
		for (int i = 0; i < json.length(); i++) {
			Object data = json.get(i);
			translateObject(parent, null, data);
		}
	}

	private static String translateLeaf(String translateMe) throws InterruptedException, IOException {

		if (nullTranslate) {
			return translateMe;
		}
		// return "[" + newLang + "] " + translateMe;
		String trans = URLEncoder.encode(translateMe, "UTF-8");
		String urlStr = "https://translate.google.co.uk/translate_a/single?client=t&sl=en&tl=" + newLang
				+ "&hl=en&dt=bd&dt=ex&dt=ld&dt=md&dt=qc&dt=rw&dt=rm&dt=ss&dt=t&dt=at&ie=UTF-8&oe=UTF-8"
				+ "&otf=2&ssel=0&tsel=0&tk=518871|866149&q=" + trans;
		URL url = new URL(urlStr);

		URLConnection conn = url.openConnection();
		conn.setRequestProperty("Referer", "https://translate.google.co.uk/?ie=UTF-8&hl=en&client=tw-ob");
		conn.setRequestProperty("User-Agent",
				"User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.93 Safari/537.36");

		String response = "";
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
			for (String line; (line = reader.readLine()) != null;) {
				response += line;
			}
		}

		// Sample response [[["traduire quelque chose de cool","translate something cool"]],,"en",,,[["t
		// So read from char 4 until the first quote comma quote
		int cutPoint = response.indexOf("\",\"");
		String translation = response.substring(4, cutPoint);

		// Now sleep for 200ms so we don't hit google too hard
		Thread.sleep(200);
		echo(translation);
		return translation;
	}

	private static void echo(String msg) {
		System.out.println(msg);
	}

	public static String tidy(String tidyMe) {
		return tidyMe.replace("\\\\u003c ", "<").replace("\\\\u003c", "<").replace(" \\\\u003e", ">").replace("\\\\u003e", ">")
				.replace(" .", ".").replace(" \"", "\"").replace(" ,", ",").replace("( ", "(").replace(" )", ")").replace("/ ", "/")
				.replace("{ ", "{");
	}
}
