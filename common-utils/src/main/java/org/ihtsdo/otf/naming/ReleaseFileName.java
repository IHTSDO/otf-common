package org.ihtsdo.otf.naming;

/**
 * The parts of an RF2 release file name ({@code <FileType>_<ContentType>_<ContentSubType>_<Country|Namespace>_<VersionDate>.<Extension>}, with an optional package type tag),
 * as parsed by {@link NamingConventions#parseFileName(String, boolean)}. Optional parts are null when they are absent
 * from the name or not captured by the configured pattern.
 *
 * @param name             the full name that was parsed (without any folder path)
 * @param convention       the convention the name was parsed against
 * @param beta             true when the name carries the {@code x} beta prefix
 * @param fileType         {@code sct2}, {@code der2} or {@code rel2}
 * @param contentType      e.g. {@code Concept}, {@code cRefset}, without any package type tag
 * @param contentSubType   the whole content sub type element, e.g. {@code AssociationSnapshot}, {@code LanguageDelta-en}
 * @param summary          the part of the content sub type before the release type, e.g. {@code Association}; may be empty
 * @param releaseType      {@code Full}, {@code Snapshot} or {@code Delta}
 * @param languageCode     e.g. {@code en}, {@code en-GB}, or null
 * @param countryNamespace e.g. {@code INT}, {@code US1000124}
 * @param packageType      Edition / Extension / Derivative tag (current convention only), or null: after the content type
 *                         in core component files, before the release type in refset files (not part of {@code summary})
 * @param versionDate      {@code yyyyMMdd}
 * @param extension        e.g. {@code .txt}, or null
 */
public record ReleaseFileName(
		String name,
		NamingConvention convention,
		boolean beta,
		String fileType,
		String contentType,
		String contentSubType,
		String summary,
		String releaseType,
		String languageCode,
		String countryNamespace,
		PackageType packageType,
		String versionDate,
		String extension) {
}
