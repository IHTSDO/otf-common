package org.snomed.otf.script;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Guards against a silent Jackson 3 regression in this module's beans.
 *
 * <p>Deliberately duplicated per module rather than shared via a test-jar: each module then guards
 * its own compiled output with no extra published artifact. Keep the copies in step.
 *
 * <p>Jackson 3 infers property-based creators from constructor parameter names, and this project
 * compiles with {@code -parameters}. So a bean with both a no-arg constructor and a convenience
 * constructor is bound through the convenience constructor instead of setters, which fails as soon
 * as a JSON payload omits a primitive. Jackson 2 never did this (it needed the parameter-names
 * module registered), so the change is invisible until a consumer's payload happens to omit a field.
 *
 * <p>These classes are deserialized by mappers this library does not own - most importantly the
 * mapper Spring Boot 4 auto-configures in consuming applications. The mapper below is therefore
 * deliberately bare: configuring it (for example with a {@code ConstructorDetector}) would make
 * this test pass while consumers still broke. Fix a failure on the class, not on the mapper -
 * annotate the convenience constructor {@code @JsonCreator(mode = JsonCreator.Mode.DISABLED)}.
 *
 * <p>A bean exposing a public no-arg constructor is asserted to bind an empty JSON object, since
 * that is the contract Jackson 2 honoured and consumers still rely on.
 */
class JacksonBeanBindingTest {

	private static final ObjectMapper BARE_MAPPER = JsonMapper.builder().build();

	@TestFactory
	Stream<DynamicTest> everyBeanBindsAnEmptyJsonObject() throws IOException, URISyntaxException {
		List<Class<?>> beans = scanModuleClasses().stream()
				.filter(JacksonBeanBindingTest::isCandidateBean)
				.sorted(Comparator.comparing(Class::getName))
				.toList();

		// Fail loudly rather than silently vacuously passing if class discovery breaks.
		if (beans.isEmpty()) {
			throw new IllegalStateException("No candidate beans found - class discovery is broken");
		}

		return beans.stream().map(bean -> DynamicTest.dynamicTest(bean.getSimpleName(),
				() -> assertDoesNotThrow(() -> BARE_MAPPER.readValue("{}", bean),
						bean.getName() + " cannot be bound from '{}' by a bare Jackson 3 mapper. "
								+ "If it has a convenience constructor, annotate that constructor "
								+ "@JsonCreator(mode = JsonCreator.Mode.DISABLED) so Jackson keeps "
								+ "using the no-arg constructor and setters.")));
	}

	/**
	 * A bean worth guarding: concrete, publicly instantiable with no arguments, and not a test or
	 * synthetic type. Types without a public no-arg constructor are intentionally skipped - they
	 * never relied on setter binding in the first place.
	 */
	private static boolean isCandidateBean(Class<?> type) {
		if (type.isEnum() || type.isInterface() || type.isAnnotation() || type.isAnonymousClass()
				|| type.isLocalClass() || type.isSynthetic()) {
			return false;
		}
		if (Modifier.isAbstract(type.getModifiers()) || !Modifier.isPublic(type.getModifiers())) {
			return false;
		}
		// Inner (non-static nested) classes cannot be constructed without their enclosing instance.
		if (type.getEnclosingClass() != null && !Modifier.isStatic(type.getModifiers())) {
			return false;
		}
		if (Throwable.class.isAssignableFrom(type)) {
			return false;
		}
		// A type declaring its own @JsonCreator has made a deliberate binding choice and does not
		// promise to bind '{}' - RestResponsePage, for instance, exists to bind Spring Data page
		// payloads. Mode.DISABLED is the opposite: it opts out of creator binding, so those types
		// are still expected to bind through the no-arg constructor and setters.
		if (declaresExplicitJsonCreator(type)) {
			return false;
		}
		return hasPublicNoArgConstructor(type);
	}

	private static boolean declaresExplicitJsonCreator(Class<?> type) {
		return Stream.concat(Stream.of(type.getDeclaredConstructors()),
						Stream.of(type.getDeclaredMethods()))
				.map(member -> member.getAnnotation(JsonCreator.class))
				.filter(Objects::nonNull)
				.anyMatch(creator -> creator.mode() != JsonCreator.Mode.DISABLED);
	}

	private static boolean hasPublicNoArgConstructor(Class<?> type) {
		for (Constructor<?> constructor : type.getConstructors()) {
			if (constructor.getParameterCount() == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Scans this module's own compiled output, resolved from the test class's code source, so the
	 * test never picks up classes belonging to a dependency.
	 */
	private static List<Class<?>> scanModuleClasses() throws IOException, URISyntaxException {
		Path testClasses = Path.of(JacksonBeanBindingTest.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());
		Path mainClasses = testClasses.resolveSibling("classes");
		if (!Files.isDirectory(mainClasses)) {
			throw new IllegalStateException("Expected compiled classes at " + mainClasses);
		}

		List<Class<?>> classes = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(mainClasses)) {
			for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
				String className = mainClasses.relativize(path).toString()
						.replace(java.io.File.separatorChar, '.')
						.replaceAll("\\.class$", "");
				try {
					classes.add(Class.forName(className, false,
							JacksonBeanBindingTest.class.getClassLoader()));
				} catch (Throwable ignored) {
					// Not loadable in isolation (missing optional dependency); nothing to guard.
				}
			}
		}
		return classes;
	}
}
