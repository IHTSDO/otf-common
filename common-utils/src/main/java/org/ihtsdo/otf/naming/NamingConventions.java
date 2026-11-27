package org.ihtsdo.otf.naming;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parses and validates SNOMED CT release package names and RF2 file names under both the legacy and the current
 * naming conventions. The convention is chosen per call from the code system's legacy naming flag.
 * <p>
 * This replaces the per-service naming regexes in SRS, RVF, Snowstorm and Package-Manager. Patterns come from
 * {@link NamingConventionProperties}; constructing this class compiles them and fails fast if any is invalid, so
 * creating it as a bean at application start acts as the startup check.
 * <p>
 * Instances are immutable and thread-safe.
 */
public class NamingConventions {

	/** Named groups every package pattern must define. */
	public static final Set<String> REQUIRED_PACKAGE_GROUPS = Set.of("product", "releaseStatus", "releaseDate", "releaseTime");

	/** Named groups every file pattern must define. */
	public static final Set<String> REQUIRED_FILE_GROUPS = Set.of("fileType", "contentType", "contentSubType", "countryNamespace", "versionDate");

	static final String MANAGED_SERVICE = "ManagedService";
	static final String LEGACY_ASSOCIATION = "AssociationReference";
	static final String CURRENT_ASSOCIATION = "Association";
	static final String LEGACY_SIMPLE = "SimpleRefset";
	static final String CURRENT_SIMPLE = "Simple";
	private static final String REFSET = "Refset";
	private static final Pattern FORMAT_SUFFIX = Pattern.compile("RF[12]$");

	private final Map<NamingConvention, Pattern> packagePatterns = new EnumMap<>(NamingConvention.class);
	private final Map<NamingConvention, Pattern> filePatterns = new EnumMap<>(NamingConvention.class);

	/**
	 * Uses the built-in default patterns.
	 */
	public NamingConventions() {
		this(new NamingConventionProperties());
	}

	/**
	 * @throws IllegalStateException if any configured pattern does not compile or lacks a required named group
	 */
	public NamingConventions(NamingConventionProperties properties) {
		List<String> problems = new ArrayList<>();
		for (NamingConvention convention : NamingConvention.values()) {
			NamingConventionProperties.Patterns patterns = properties.get(convention);
			compile(convention, "package", patterns.getPackagePattern(), REQUIRED_PACKAGE_GROUPS, problems)
					.ifPresent(p -> packagePatterns.put(convention, p));
			compile(convention, "file", patterns.getFilePattern(), REQUIRED_FILE_GROUPS, problems)
					.ifPresent(p -> filePatterns.put(convention, p));
		}
		if (!problems.isEmpty()) {
			throw new IllegalStateException("Invalid naming convention configuration: " + String.join("; ", problems));
		}
	}

	// ---- Package names ----

	/**
	 * @param packageName  a package zip name or root folder name, without any path
	 * @param legacyNaming the code system's legacy naming flag
	 * @return the parsed parts, or empty if the name does not match the convention's pattern
	 */
	public Optional<PackageName> parsePackageName(String packageName, boolean legacyNaming) {
		return parsePackageName(packageName, NamingConvention.forCodeSystem(legacyNaming));
	}

	public Optional<PackageName> parsePackageName(String packageName, NamingConvention convention) {
		if (packageName == null) {
			return Optional.empty();
		}
		Matcher m = packagePatterns.get(convention).matcher(packageName);
		if (!m.matches()) {
			return Optional.empty();
		}
		return Optional.of(new PackageName(
				packageName,
				convention,
				group(m, "betaPrefix") != null,
				m.group("product"),
				PackageType.fromTag(group(m, "packageType")).orElse(null),
				group(m, "moduleId"),
				group(m, "format"),
				m.group("releaseStatus"),
				group(m, "countryNamespace"),
				m.group("releaseDate"),
				m.group("releaseTime"),
				group(m, "extension")));
	}

	/**
	 * @return the reasons the name is invalid under the code system's convention; empty if it is valid
	 */
	public List<String> validatePackageName(String packageName, boolean legacyNaming) {
		return validatePackageName(packageName, NamingConvention.forCodeSystem(legacyNaming));
	}

	public List<String> validatePackageName(String packageName, NamingConvention convention) {
		Optional<PackageName> parsed = parsePackageName(packageName, convention);
		if (parsed.isEmpty()) {
			return List.of(String.format("Package name '%s' does not match the %s naming convention pattern %s",
					packageName, convention, packagePatterns.get(convention).pattern()));
		}
		List<String> errors = new ArrayList<>();
		if (convention == NamingConvention.CURRENT) {
			PackageName name = parsed.get();
			if (name.product().startsWith(MANAGED_SERVICE)) {
				errors.add(String.format("Package name '%s': the '%s' product option is not allowed in the %s naming convention",
						packageName, MANAGED_SERVICE, convention));
			}
			if (name.format() != null || FORMAT_SUFFIX.matcher(name.product()).find()) {
				errors.add(String.format("Package name '%s': the RF1/RF2 format element is not allowed in the %s naming convention",
						packageName, convention));
			}
			if (name.countryNamespace() != null) {
				errors.add(String.format("Package name '%s': the country/namespace element is not allowed in the %s naming convention; use the module id in the product element instead",
						packageName, convention));
			}
		}
		return errors;
	}

	public boolean isValidPackageName(String packageName, boolean legacyNaming) {
		return validatePackageName(packageName, legacyNaming).isEmpty();
	}

	/**
	 * Works out which convention a package name follows, for example when reading a previously published package
	 * during the transition. The current convention is tried first.
	 *
	 * @return the first convention under which the name is valid, or empty if neither
	 */
	public Optional<NamingConvention> detectPackageConvention(String packageName) {
		if (validatePackageName(packageName, NamingConvention.CURRENT).isEmpty()) {
			return Optional.of(NamingConvention.CURRENT);
		}
		if (validatePackageName(packageName, NamingConvention.LEGACY).isEmpty()) {
			return Optional.of(NamingConvention.LEGACY);
		}
		return Optional.empty();
	}

	// ---- RF2 file names ----

	/**
	 * @param fileName     an RF2 file name; any leading folder path ({@code /} or {@code \}) is ignored
	 * @param legacyNaming the code system's legacy naming flag
	 * @return the parsed parts, or empty if the name does not match the convention's pattern
	 */
	public Optional<ReleaseFileName> parseFileName(String fileName, boolean legacyNaming) {
		return parseFileName(fileName, NamingConvention.forCodeSystem(legacyNaming));
	}

	public Optional<ReleaseFileName> parseFileName(String fileName, NamingConvention convention) {
		if (fileName == null) {
			return Optional.empty();
		}
		String name = stripPath(fileName);
		Matcher m = filePatterns.get(convention).matcher(name);
		if (!m.matches()) {
			return Optional.empty();
		}
		return Optional.of(new ReleaseFileName(
				name,
				convention,
				group(m, "betaPrefix") != null,
				m.group("fileType"),
				m.group("contentType"),
				m.group("contentSubType"),
				group(m, "summary"),
				group(m, "releaseType"),
				group(m, "languageCode"),
				m.group("countryNamespace"),
				PackageType.fromTag(contentTypeTag(m) != null ? contentTypeTag(m) : contentSubTypeTag(m)).orElse(null),
				m.group("versionDate"),
				group(m, "extension")));
	}

	/**
	 * @return the reasons the name is invalid under the code system's convention; empty if it is valid
	 */
	public List<String> validateFileName(String fileName, boolean legacyNaming) {
		return validateFileName(fileName, NamingConvention.forCodeSystem(legacyNaming));
	}

	public List<String> validateFileName(String fileName, NamingConvention convention) {
		Optional<ReleaseFileName> parsed = parseFileName(fileName, convention);
		if (parsed.isEmpty()) {
			return List.of(String.format("File name '%s' does not match the %s naming convention pattern %s",
					fileName, convention, filePatterns.get(convention).pattern()));
		}
		List<String> errors = new ArrayList<>();
		if (convention == NamingConvention.CURRENT) {
			String summary = summaryOf(parsed.get());
			if (summary.endsWith(LEGACY_ASSOCIATION)) {
				errors.add(String.format("File name '%s': use '%s' instead of '%s' in the %s naming convention",
						fileName, CURRENT_ASSOCIATION, LEGACY_ASSOCIATION, convention));
			}
			if (summary.endsWith(LEGACY_SIMPLE)) {
				errors.add(String.format("File name '%s': use '%s' instead of '%s' in the %s naming convention",
						fileName, CURRENT_SIMPLE, LEGACY_SIMPLE, convention));
			}
			errors.addAll(validatePackageTypeTagPlace(fileName, parsed.get(), convention));
		}
		return errors;
	}

	/**
	 * Core component files carry the package type tag after the content type ({@code sct2_ConceptEdition_Full_...});
	 * refset files carry it before the release type ({@code der2_cRefset_LanguageEditionSnapshot-en_...}).
	 */
	private List<String> validatePackageTypeTagPlace(String fileName, ReleaseFileName parsed, NamingConvention convention) {
		Matcher m = filePatterns.get(convention).matcher(parsed.name());
		if (!m.matches()) {
			return List.of();
		}
		boolean refset = parsed.contentType().endsWith(REFSET);
		if (refset && contentTypeTag(m) != null) {
			return List.of(String.format("File name '%s': in refset files the package type tag goes before the release type, e.g. %s, not after the content type",
					fileName, "der2_cRefset_LanguageEditionSnapshot-en_INT_20270701.txt"));
		}
		if (!refset && contentSubTypeTag(m) != null) {
			return List.of(String.format("File name '%s': in core component files the package type tag goes directly after the content type, e.g. %s",
					fileName, "sct2_ConceptEdition_Snapshot_INT_20270701.txt"));
		}
		return List.of();
	}

	public boolean isValidFileName(String fileName, boolean legacyNaming) {
		return validateFileName(fileName, legacyNaming).isEmpty();
	}

	/**
	 * Converts a content sub type summary between conventions for the renamed refset files:
	 * {@code AssociationReference} &harr; {@code Association} and {@code SimpleRefset} &harr; {@code Simple}.
	 * Converting to CURRENT renames those legacy suffixes; converting to LEGACY renames only an exact
	 * {@code Association} or {@code Simple}. Other values are returned unchanged.
	 *
	 * @param summary the part of the content sub type before the release type, e.g. {@code AssociationReference}
	 * @param target  the convention to convert to
	 */
	public static String convertContentSubTypeSummary(String summary, NamingConvention target) {
		if (summary == null) {
			return null;
		}
		if (target == NamingConvention.CURRENT) {
			if (summary.endsWith(LEGACY_ASSOCIATION)) {
				return replaceSuffix(summary, LEGACY_ASSOCIATION, CURRENT_ASSOCIATION);
			}
			if (summary.endsWith(LEGACY_SIMPLE)) {
				return replaceSuffix(summary, LEGACY_SIMPLE, CURRENT_SIMPLE);
			}
		} else {
			// Exact match only: other refset types such as ExpressionAssociation and ExtendedAssociation also end in
			// "Association" and must not be renamed.
			if (summary.equals(CURRENT_ASSOCIATION)) {
				return LEGACY_ASSOCIATION;
			}
			if (summary.equals(CURRENT_SIMPLE)) {
				return LEGACY_SIMPLE;
			}
		}
		return summary;
	}

	/**
	 * @return the compiled package pattern for the convention
	 */
	public Pattern getPackagePattern(NamingConvention convention) {
		return packagePatterns.get(convention);
	}

	/**
	 * @return the compiled file pattern for the convention
	 */
	public Pattern getFilePattern(NamingConvention convention) {
		return filePatterns.get(convention);
	}

	// ---- Helpers ----

	private static Optional<Pattern> compile(NamingConvention convention, String kind, String regex, Set<String> requiredGroups, List<String> problems) {
		String label = convention.name().toLowerCase() + " " + kind + " pattern";
		if (regex == null || regex.isBlank()) {
			problems.add(label + " is empty");
			return Optional.empty();
		}
		Pattern pattern;
		try {
			pattern = Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			problems.add(label + " does not compile: " + e.getDescription() + " near index " + e.getIndex());
			return Optional.empty();
		}
		List<String> missing = requiredGroups.stream()
				.filter(g -> !pattern.namedGroups().containsKey(g))
				.sorted()
				.toList();
		if (!missing.isEmpty()) {
			problems.add(label + " is missing required named groups " + missing);
			return Optional.empty();
		}
		return Optional.of(pattern);
	}

	private static String contentTypeTag(Matcher m) {
		return group(m, "packageType");
	}

	private static String contentSubTypeTag(Matcher m) {
		return group(m, "subTypePackageType");
	}

	private static String group(Matcher m, String name) {
		return m.pattern().namedGroups().containsKey(name) ? m.group(name) : null;
	}

	private static String summaryOf(ReleaseFileName file) {
		if (file.summary() != null) {
			return file.summary();
		}
		// Overridden pattern without a summary group: strip the release type and language code ourselves
		return file.contentSubType().replaceFirst("(Full|Snapshot|Delta)(-.*)?$", "");
	}

	private static String stripPath(String fileName) {
		int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		return slash >= 0 ? fileName.substring(slash + 1) : fileName;
	}

	private static String replaceSuffix(String value, String suffix, String replacement) {
		return value.substring(0, value.length() - suffix.length()) + replacement;
	}
}
