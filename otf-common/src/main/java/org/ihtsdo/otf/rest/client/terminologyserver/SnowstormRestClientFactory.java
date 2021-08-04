package org.ihtsdo.otf.rest.client.terminologyserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SnowstormRestClientFactory {
	private final String snowstormUrl;
	private final String reasonerId;
	private final Cache<String, SnowstormRestClient> clientCache;
	private final ObjectMapper objectMapper;

	public SnowstormRestClientFactory(String snowstormUrl, String reasonerId) {
		this.snowstormUrl = snowstormUrl;
		this.reasonerId = reasonerId;
		clientCache = CacheBuilder.newBuilder()
				.expireAfterAccess(5, TimeUnit.MINUTES)
				.build();
		objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	/**
	 * Creates a Snowstorm client using the authentication context of the current thread.
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
