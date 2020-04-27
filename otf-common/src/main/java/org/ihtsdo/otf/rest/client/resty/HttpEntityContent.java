package org.ihtsdo.otf.rest.client.resty;

import org.apache.http.HttpEntity;
import us.monoid.web.AbstractContent;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * Please replace use of Resty and RestyMod with the Spring Framework RestTemplate.
 */
@Deprecated
public class HttpEntityContent extends AbstractContent {

	private final HttpEntity httpEntity;

	public HttpEntityContent(HttpEntity httpEntity) {
		this.httpEntity = httpEntity;
	}

	@Override
	protected void addContent(URLConnection con) throws IOException {
		sendEntity(con, httpEntity);
	}

	@Override
	public void writeHeader(OutputStream os) throws IOException {
	}

	@Override
	public void writeContent(OutputStream os) throws IOException {
	}

	private void sendEntity(URLConnection urlConnection, HttpEntity entity) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) urlConnection;
		conn.setReadTimeout(10000);
		conn.setConnectTimeout(15000);
		conn.setRequestMethod("POST");
		conn.setUseCaches(false);
		conn.setDoInput(true);
		conn.setDoOutput(true);

		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.addRequestProperty("Content-length", entity.getContentLength() + "");
		conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());

		OutputStream os = conn.getOutputStream();
		entity.writeTo(conn.getOutputStream());
		os.close();
	}

}
