package org.ihtsdo.otf.naming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring wiring for the shared naming convention API. Import it into a service with
 * {@code @Import(NamingConventionConfiguration.class)}.
 * <p>
 * Patterns are bound from properties under {@value #PREFIX}. Put any overrides in the shared Consul section
 * ({@code config/application/naming-convention/...}) so that every service uses the same patterns; a service's own
 * Consul section can still override them. Without any configuration the built-in defaults in
 * {@link NamingConventionProperties} are used.
 * <p>
 * Creating the {@link NamingConventions} bean compiles every pattern and checks its named groups, so a bad override
 * stops the application at startup rather than failing on the first release.
 */
@Configuration(proxyBeanMethods = false)
public class NamingConventionConfiguration {

	public static final String PREFIX = "naming-convention";

	private static final Logger LOGGER = LoggerFactory.getLogger(NamingConventionConfiguration.class);

	@Bean
	@ConfigurationProperties(prefix = PREFIX)
	public NamingConventionProperties namingConventionProperties() {
		return new NamingConventionProperties();
	}

	@Bean
	public NamingConventions namingConventions(NamingConventionProperties properties) {
		NamingConventions conventions = new NamingConventions(properties);
		for (NamingConvention convention : NamingConvention.values()) {
			LOGGER.info("Naming convention {}: package pattern {} , file pattern {}", convention,
					conventions.getPackagePattern(convention).pattern(), conventions.getFilePattern(convention).pattern());
		}
		return conventions;
	}
}
