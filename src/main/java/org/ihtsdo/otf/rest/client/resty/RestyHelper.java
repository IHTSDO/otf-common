package org.ihtsdo.otf.rest.client.resty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.json.JSONObject;
import us.monoid.web.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;

public class RestyHelper extends RestyMod {

	public static final String UTF_8 = "UTF-8";
	private static final Logger LOGGER = LoggerFactory.getLogger(RestyHelper.class);

	private static final int[] HTTP_SUCCESSFULL_ARRAY = { 200, 201, 202, 203, 204 };

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

	public JSONResource json(String url, AbstractContent content) throws IOException {
		boolean allowErrors = false;
		return json(url, content, allowErrors);
	}

	public JSONResource json(String url, AbstractContent content, boolean allowErrors) throws IOException {
		JSONResource response = super.json(url, content);
		if (!allowErrors) {
			boolean httpSuccessfull = false;
			// Not easy to recover the status as header keys are inconsistent.
			// Check against all known acceptable statuses instead.
			for (int thisStatus : HTTP_SUCCESSFULL_ARRAY) {
				if (response.status(thisStatus)) {
					httpSuccessfull = true;
				}
			}

			if (!httpSuccessfull) {
				String body = "";
				try {
					body = response.object().toString(2);
				} catch (Exception e) {
					body = "Unable to parse body: ";
					body += e.getMessage();
				}
				throw new IOException("Call to " + url + " returned unacceptable status with body " + body);
			}
		}
		return response;
	}
}
