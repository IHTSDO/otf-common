package org.ihtsdo.otf.rest.client.resty;

import us.monoid.json.JSONObject;
import us.monoid.web.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;

public class RestyHelper extends RestyMod {

	public static final String UTF_8 = "UTF-8";

	public RestyHelper(final String defaultAcceptableContent) {

		super(new Resty.Option() {
			@Override
			public void apply(URLConnection aConnection) {
				aConnection.addRequestProperty("Accept", defaultAcceptableContent);
			}
		});
	}

	public static Content content(JSONObject someJson, String aMimeType) {
		try {
			return new Content(aMimeType, someJson.toString().getBytes(UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(UTF_8 + " encoding not supported!", e);
		}
	}

	public JSONResource put(String url, JSONObject jsonObj, String contentType) throws IOException {
		return json(url, put(content(jsonObj, contentType)));

	}

	public JSONResource json(String url, JSONObject jsonObj, String contentType) throws IOException {
		return json(url, content(jsonObj, contentType));
	}

}
