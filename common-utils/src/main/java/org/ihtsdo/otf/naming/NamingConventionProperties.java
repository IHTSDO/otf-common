package org.ihtsdo.otf.naming;

/**
 * The package and file name patterns for each {@link NamingConvention}, with built-in defaults that can be
 * overridden from configuration. JavaBean setters let Spring bind this directly, e.g. from the shared Consul section
 * {@code config/application/} under the prefix {@code naming-convention}:
 * <pre>
 * naming-convention.legacy.package-pattern=...
 * naming-convention.legacy.file-pattern=...
 * naming-convention.current.package-pattern=...
 * naming-convention.current.file-pattern=...
 * </pre>
 * A blank or missing value falls back to the default. Patterns are compiled and checked when
 * {@link NamingConventions} is constructed.
 * <p>
 * Patterns must define the named groups listed in {@link NamingConventions#REQUIRED_PACKAGE_GROUPS} and
 * {@link NamingConventions#REQUIRED_FILE_GROUPS}; all other groups documented on {@link PackageName} and
 * {@link ReleaseFileName} are optional.
 */
public class NamingConventionProperties {

	private static final String RELEASE_STATUS = "(?<releaseStatus>(?i:DAILYBUILD|ALPHA|BETA|PREPRODUCTION|PRODUCTION|MEMBER)(?:_(?i:BETA))?)";
	private static final String RELEASE_DATE_TIME = "(?<releaseDate>\\d{8})T(?<releaseTime>\\d{6})Z(?<extension>\\.zip)?";
	private static final String FILE_PREFIX = "^(?<betaPrefix>x)?(?<fileType>sct2|der2|rel2)_";
	private static final String PACKAGE_TYPE_TAG = "Edition|Extension|Derivative";
	private static final String RELEASE_TYPE_AND_LANGUAGE =
			"(?<releaseType>Full|Snapshot|Delta)(?:-(?<languageCode>[A-Za-z]{2,3}(?:-[A-Za-z0-9]+)*))?)_";
	private static final String FILE_CONTENT_SUB_TYPE = "_(?<contentSubType>(?<summary>[^_]*?)" + RELEASE_TYPE_AND_LANGUAGE;
	private static final String FILE_CONTENT_SUB_TYPE_WITH_TAG =
			"_(?<contentSubType>(?<summary>[^_]*?)(?<subTypePackageType>" + PACKAGE_TYPE_TAG + ")?" + RELEASE_TYPE_AND_LANGUAGE;
	private static final String FILE_END = "_(?<versionDate>\\d{8})(?<extension>\\.[A-Za-z0-9]+)?$";

	/**
	 * Legacy package naming. Equivalent to SRS {@code srs.release.package.pattern}, with named groups:
	 * {@code [x]SnomedCT_[Product][Edition|Extension][RF1|RF2]_[ReleaseStatus][_CountryNamespace]_[Date]T[Time]Z[.zip]}
	 */
	public static final String DEFAULT_LEGACY_PACKAGE_PATTERN =
			"^(?<betaPrefix>x)?SnomedCT_(?<product>[^_]+?)(?<packageType>Edition|Extension)?(?<format>RF1|RF2)?_"
					+ RELEASE_STATUS + "(?:_(?<countryNamespace>[^_]+))?_" + RELEASE_DATE_TIME + "$";

	/**
	 * Legacy RF2 file naming: {@code [x]<FileType>_<ContentType>_<ContentSubType>_<Country|Namespace>_<VersionDate>.<Extension>}
	 */
	public static final String DEFAULT_LEGACY_FILE_PATTERN =
			FILE_PREFIX + "(?<contentType>[^_]+)" + FILE_CONTENT_SUB_TYPE + "(?<countryNamespace>[^_]+)" + FILE_END;

	/**
	 * Current package naming:
	 * {@code [x]SnomedCT_[Product][Edition|Extension|Derivative][-ModuleId]_[ReleaseStatus]_[Date]T[Time]Z[.zip]}.
	 * No format element and no fourth (country/namespace) element. The ban on {@code ManagedService} and on an
	 * RF1/RF2 suffix in the product is enforced by {@link NamingConventions}, not by this pattern.
	 */
	public static final String DEFAULT_CURRENT_PACKAGE_PATTERN =
			"^(?<betaPrefix>x)?SnomedCT_(?<product>[A-Za-z0-9-]+?)(?<packageType>Edition|Extension|Derivative)?(?:-(?<moduleId>\\d{6,18}))?_"
					+ RELEASE_STATUS + "_" + RELEASE_DATE_TIME + "$";

	/**
	 * Current RF2 file naming: the legacy layout plus an optional Edition/Extension/Derivative tag. Core component files
	 * carry it directly after the content type, e.g. {@code sct2_ConceptEdition_Full_INT_20270701.txt}. Refset files keep
	 * their content type and carry it in the content sub type, before the release type, e.g.
	 * {@code der2_cRefset_DICOMLanguageEditionSnapshot-en_INT_20240701.txt}.
	 * <p>
	 * The pattern accepts the tag in either place (groups {@code packageType} and {@code subTypePackageType});
	 * {@link NamingConventions} rejects a tag in the wrong place for the file's content type, and the
	 * {@code AssociationReference} and {@code SimpleRefset} sub types.
	 */
	public static final String DEFAULT_CURRENT_FILE_PATTERN =
			FILE_PREFIX + "(?<contentType>[^_]+?)(?<packageType>" + PACKAGE_TYPE_TAG + ")?" + FILE_CONTENT_SUB_TYPE_WITH_TAG
					+ "(?<countryNamespace>[^_]+)" + FILE_END;

	private final Patterns legacy = new Patterns(DEFAULT_LEGACY_PACKAGE_PATTERN, DEFAULT_LEGACY_FILE_PATTERN);
	private final Patterns current = new Patterns(DEFAULT_CURRENT_PACKAGE_PATTERN, DEFAULT_CURRENT_FILE_PATTERN);

	public Patterns getLegacy() {
		return legacy;
	}

	public Patterns getCurrent() {
		return current;
	}

	public Patterns get(NamingConvention convention) {
		return convention == NamingConvention.LEGACY ? legacy : current;
	}

	/**
	 * The package and file name patterns for one convention.
	 */
	public static class Patterns {

		private final String defaultPackagePattern;
		private final String defaultFilePattern;
		private String packagePattern;
		private String filePattern;

		Patterns(String defaultPackagePattern, String defaultFilePattern) {
			this.defaultPackagePattern = defaultPackagePattern;
			this.defaultFilePattern = defaultFilePattern;
			this.packagePattern = defaultPackagePattern;
			this.filePattern = defaultFilePattern;
		}

		public String getPackagePattern() {
			return packagePattern;
		}

		public void setPackagePattern(String packagePattern) {
			this.packagePattern = isBlank(packagePattern) ? defaultPackagePattern : packagePattern;
		}

		public String getFilePattern() {
			return filePattern;
		}

		public void setFilePattern(String filePattern) {
			this.filePattern = isBlank(filePattern) ? defaultFilePattern : filePattern;
		}

		private static boolean isBlank(String value) {
			return value == null || value.isBlank();
		}
	}
}
