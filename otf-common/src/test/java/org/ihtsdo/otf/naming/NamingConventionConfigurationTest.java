package org.ihtsdo.otf.naming;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NamingConventionConfigurationTest {

	@Configuration
	@EnableConfigurationProperties
	@Import(NamingConventionConfiguration.class)
	static class TestApp {
	}

	private static AnnotationConfigApplicationContext start(Map<String, Object> properties) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
		context.register(TestApp.class);
		context.refresh();
		return context;
	}

	@Test
	void defaultsApplyWithoutConfiguration() {
		try (AnnotationConfigApplicationContext context = start(Map.of())) {
			NamingConventions conventions = context.getBean(NamingConventions.class);
			assertTrue(conventions.isValidPackageName("SnomedCT_International_PRODUCTION_20270701T120000Z.zip", false));
			assertTrue(conventions.isValidPackageName("SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip", true));
		}
	}

	@Test
	void sharedConfigOverridesPattern() {
		String pattern = "^Pkg_(?<product>[A-Z]+)_(?<releaseStatus>PRODUCTION)_(?<releaseDate>\\d{8})T(?<releaseTime>\\d{6})Z$";
		try (AnnotationConfigApplicationContext context = start(Map.of("naming-convention.current.package-pattern", pattern))) {
			NamingConventions conventions = context.getBean(NamingConventions.class);
			assertEquals(pattern, conventions.getPackagePattern(NamingConvention.CURRENT).pattern());
			assertTrue(conventions.isValidPackageName("Pkg_INT_PRODUCTION_20270701T120000Z", false));
			assertEquals(NamingConventionProperties.DEFAULT_LEGACY_PACKAGE_PATTERN, conventions.getPackagePattern(NamingConvention.LEGACY).pattern());
		}
	}

	@Test
	void invalidPatternFailsStartup() {
		BeanCreationException e = assertThrows(BeanCreationException.class,
				() -> start(Map.of("naming-convention.legacy.file-pattern", "^(unclosed")));
		Throwable root = e.getMostSpecificCause();
		assertInstanceOf(IllegalStateException.class, root);
		assertTrue(root.getMessage().contains("legacy file pattern does not compile"), root.getMessage());
	}
}
