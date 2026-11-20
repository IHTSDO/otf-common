package org.ihtsdo.otf.naming;

/**
 * The parts of a release package name, as parsed by {@link NamingConventions#parsePackageName(String, boolean)}.
 * Optional parts are null when they are absent from the name or not captured by the configured pattern.
 *
 * @param name             the full name that was parsed
 * @param convention       the convention the name was parsed against
 * @param beta             true when the name carries the {@code x} beta prefix
 * @param product          the product element, without any package type tag, module id or format,
 *                         e.g. {@code International}, {@code US}, {@code ManagedServiceIE}
 * @param packageType      Edition / Extension / Derivative tag, or null
 * @param moduleId         module id carried in the product element (current convention only), or null
 * @param format           {@code RF1} / {@code RF2} (legacy convention only), or null
 * @param releaseStatus    e.g. {@code PRODUCTION}, {@code BETA}, {@code DAILYBUILD_BETA}
 * @param countryNamespace the fourth element, e.g. {@code IE1000220} (legacy convention only), or null
 * @param releaseDate      {@code yyyyMMdd}
 * @param releaseTime      {@code HHmmss}
 * @param extension        e.g. {@code .zip}, or null when the name is a folder name
 */
public record PackageName(
		String name,
		NamingConvention convention,
		boolean beta,
		String product,
		PackageType packageType,
		String moduleId,
		String format,
		String releaseStatus,
		String countryNamespace,
		String releaseDate,
		String releaseTime,
		String extension) {
}
