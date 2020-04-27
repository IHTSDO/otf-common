package org.ihtsdo.otf.rest.client.terminologyserver;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.ihtsdo.sso.integration.SecurityUtil;

import java.util.concurrent.TimeUnit;

public class SnowstormRestClientFactory {

	private String snowstormUrl;
	private String reasonerId;
	private final Cache<String, SnowstormRestClient> clientCache;

	public SnowstormRestClientFactory(String snowstormUrl, String reasonerId) {
		this.snowstormUrl = snowstormUrl;
		this.reasonerId = reasonerId;
		clientCache = CacheBuilder.newBuilder()
				.expireAfterAccess(5, TimeUnit.MINUTES)
				.build();
	}

	/**
	 * Creates a Snow Owl client using the authentication context of the current thread.
	 * @return
	 */
	public SnowstormRestClient getClient() {
		String authenticationToken = SecurityUtil.getAuthenticationToken();
		SnowstormRestClient client = clientCache.getIfPresent(authenticationToken);
		if (client == null) {
			synchronized (clientCache) {
				client = clientCache.getIfPresent(authenticationToken);
				if (client == null) {
					client = new SnowstormRestClient(snowstormUrl, authenticationToken);
					client.setReasonerId(reasonerId);
					client.setUseExternalClassificationService(true);
					clientCache.put(authenticationToken, client);
				}
			}
		}
		return client;
	}

}
