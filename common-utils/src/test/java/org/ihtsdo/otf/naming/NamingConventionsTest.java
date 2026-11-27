package org.ihtsdo.otf.naming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NamingConventionsTest {

	private final NamingConventions conventions = new NamingConventions();

	@ParameterizedTest(name = "{0} {1}: {2} ({3})")
	@CsvSource(delimiter = '|', textBlock = """
			LEGACY  | true  | SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip                 | International with RF2 format
			LEGACY  | true  | SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z                     | package root folder, no .zip
			LEGACY  | true  | xSnomedCT_InternationalRF2_BETA_20250101T120000Z.zip                      | beta prefix
			LEGACY  | true  | SnomedCT_ManagedServiceIE_PRODUCTION_IE1000220_20250521T120000Z.zip       | managed service with namespace element
			LEGACY  | true  | SnomedCT_USEditionRF2_PRODUCTION_20250301T120000Z.zip                     | Edition tag plus format
			LEGACY  | true  | SnomedCT_SpanishRelease-es_PRODUCTION_20250331T120000Z.zip                | hyphen in product
			LEGACY  | true  | SnomedCT_InternationalRF2_DAILYBUILD_BETA_20250101T120000Z.zip            | SRS daily build
			LEGACY  | true  | SnomedCT_MySimplexEdition_Production_20250101T120000Z.zip                 | Simplex mixed-case status
			LEGACY  | false | SnomedCT_InternationalRF2_20250101T120000Z.zip                            | missing release status
			LEGACY  | false | SnomedCT_InternationalRF2_PRODUCTION_20250101.zip                         | missing release time
			LEGACY  | false | snomed-MAIN-20250101-SNAPSHOT.zip                                         | Snowstorm export name, not a release package
			CURRENT | true  | SnomedCT_International_PRODUCTION_20270701T120000Z.zip                    | International without format element
			CURRENT | true  | xSnomedCT_International_BETA_20270701T120000Z.zip                         | beta prefix
			CURRENT | true  | SnomedCT_USEdition_PRODUCTION_20270901T120000Z.zip                        | Edition tag
			CURRENT | true  | SnomedCT_IEExtension-11000220105_PRODUCTION_20271021T120000Z.zip          | Extension tag with module id
			CURRENT | true  | SnomedCT_LOINCDerivative_PRODUCTION_20271001T120000Z.zip                  | Derivative tag
			CURRENT | true  | SnomedCT_International_PRODUCTION_20270701T120000Z                        | package root folder, no .zip
			CURRENT | false | SnomedCT_InternationalRF2_PRODUCTION_20270701T120000Z.zip                 | RF2 format element removed
			CURRENT | false | SnomedCT_ManagedServiceIE_PRODUCTION_20271021T120000Z.zip                 | ManagedService option removed
			CURRENT | false | SnomedCT_IEExtension_PRODUCTION_IE1000220_20271021T120000Z.zip            | fourth element removed
			CURRENT | false | SnomedCT_International_20270701T120000Z.zip                               | missing release status
			""")
	void validatePackageNames(NamingConvention convention, boolean expectedValid, String name, String note) {
		List<String> errors = conventions.validatePackageName(name, convention == NamingConvention.LEGACY);
		assertEquals(expectedValid, errors.isEmpty(), note + " -> " + errors);
	}

	@ParameterizedTest(name = "{0} {1}: {2} ({3})")
	@CsvSource(delimiter = '|', textBlock = """
			LEGACY  | true  | sct2_Concept_Snapshot_INT_20250101.txt                                    | core file
			LEGACY  | true  | xsct2_Concept_Snapshot_INT_20250101.txt                                   | beta prefix
			LEGACY  | true  | sct2_Description_Snapshot-en_INT_20250101.txt                             | language code
			LEGACY  | true  | der2_cRefset_LanguageDelta-en-GB_GB1000000_20250101.txt                   | dialect code on language refset
			LEGACY  | true  | der2_cRefset_AssociationReferenceSnapshot_US1000124_20250301.txt          | AssociationReference allowed
			LEGACY  | true  | der2_cRefset_AssociationSnapshot_INT_20250101.txt                         | Association also seen in legacy packages
			LEGACY  | true  | der2_Refset_SimpleRefsetFull_IE1000220_20250521.txt                       | SimpleRefset allowed
			LEGACY  | true  | der2_iisssccRefset_ExtendedMapFull_INT_20250101.txt                       | map refset
			LEGACY  | true  | SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z/Snapshot/Terminology/sct2_Concept_Snapshot_INT_20250101.txt | path is ignored
			LEGACY  | false | sct2_Concept_Snapshot_20250101.txt                                        | only four elements
			LEGACY  | false | sct2_Concept_INT_20250101.txt                                             | no release type
			LEGACY  | false | doc_Readme_INT_20250101.txt                                               | not an RF2 data file
			CURRENT | true  | sct2_Concept_Snapshot_INT_20270701.txt                                    | no tag
			CURRENT | true  | sct2_ConceptEdition_Full_INT_20270701.txt                                 | Edition tag after content type
			CURRENT | true  | sct2_DescriptionEdition_Full-en_INT_20270701.txt                          | Edition tag with language code
			CURRENT | true  | sct2_StatedRelationshipEdition_Full_INT_20270701.txt                      | stated relationships
			CURRENT | true  | sct2_RelationshipEdition_Full_INT_20270701.txt                            | inferred relationships
			CURRENT | true  | sct2_RelationshipConcreteValuesEdition_Full_INT_20270701.txt              | concrete relationships
			CURRENT | true  | xsct2_ConceptEdition_Delta_INT_20270701.txt                               | beta prefix with tag
			CURRENT | true  | der2_cRefset_DICOMLanguageEditionSnapshot-en_INT_20240701.txt             | refset tag before the release type
			CURRENT | true  | der2_cRefset_LanguageExtensionSnapshot-en_US1000124_20270901.txt          | refset Extension tag
			CURRENT | true  | sct2_sRefset_OWLExpressionEditionSnapshot_INT_20270701.txt                | OWL refset tag
			CURRENT | true  | der2_cRefset_AssociationSnapshot_INT_20270701.txt                         | renamed association refset
			CURRENT | true  | der2_Refset_SimpleDerivativeFull_IE1000220_20271021.txt                   | renamed simple refset with Derivative tag
			CURRENT | false | der2_cRefsetEdition_LanguageSnapshot-en_INT_20270701.txt                  | refset tag after the content type
			CURRENT | false | sct2_Concept_EditionSnapshot_INT_20270701.txt                             | core component tag before the release type
			CURRENT | false | sct2_Concept_Edition_Full_INT_20270701.txt                                | tag as its own element
			CURRENT | false | Edition_sct2_Concept_Full_INT_20270701.txt                                | tag before the file type
			CURRENT | false | der2_cRefset_AssociationReferenceEditionSnapshot_INT_20270701.txt         | AssociationReference renamed, with tag
			CURRENT | true  | der2_ccRefset_ExtendedAssociationSnapshot_INT_20270701.txt                | other association-named refset unaffected
			CURRENT | false | der2_cRefset_AssociationReferenceSnapshot_INT_20270701.txt                | AssociationReference renamed
			CURRENT | false | der2_Refset_SimpleRefsetDelta_INT_20270701.txt                            | SimpleRefset renamed
			CURRENT | false | sct2_Concept_Snapshot_20270701.txt                                        | only four elements
			""")
	void validateFileNames(NamingConvention convention, boolean expectedValid, String name, String note) {
		List<String> errors = conventions.validateFileName(name, convention == NamingConvention.LEGACY);
		assertEquals(expectedValid, errors.isEmpty(), note + " -> " + errors);
	}

	@Test
	void legacyNamingFlagSelectsConvention() {
		assertEquals(NamingConvention.LEGACY, NamingConvention.forCodeSystem(true));
		assertEquals(NamingConvention.CURRENT, NamingConvention.forCodeSystem(false));
		assertTrue(conventions.isValidPackageName("SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip", true));
		assertFalse(conventions.isValidPackageName("SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip", false));
	}

	@Test
	void parseLegacyPackageName() {
		PackageName name = conventions.parsePackageName("xSnomedCT_ManagedServiceIE_PRODUCTION_IE1000220_20250521T120000Z.zip", true).orElseThrow();
		assertEquals(NamingConvention.LEGACY, name.convention());
		assertTrue(name.beta());
		assertEquals("ManagedServiceIE", name.product());
		assertNull(name.packageType());
		assertNull(name.format());
		assertEquals("PRODUCTION", name.releaseStatus());
		assertEquals("IE1000220", name.countryNamespace());
		assertEquals("20250521", name.releaseDate());
		assertEquals("120000", name.releaseTime());
		assertEquals(".zip", name.extension());

		PackageName us = conventions.parsePackageName("SnomedCT_USEditionRF2_PRODUCTION_20250301T120000Z", true).orElseThrow();
		assertEquals("US", us.product());
		assertEquals(PackageType.EDITION, us.packageType());
		assertEquals("RF2", us.format());
		assertNull(us.extension());
	}

	@Test
	void parseCurrentPackageName() {
		PackageName name = conventions.parsePackageName("SnomedCT_IEExtension-11000220105_PRODUCTION_20271021T120000Z.zip", false).orElseThrow();
		assertEquals(NamingConvention.CURRENT, name.convention());
		assertFalse(name.beta());
		assertEquals("IE", name.product());
		assertEquals(PackageType.EXTENSION, name.packageType());
		assertEquals("11000220105", name.moduleId());
		assertNull(name.format());
		assertNull(name.countryNamespace());
		assertEquals("20271021", name.releaseDate());

		PackageName derivative = conventions.parsePackageName("SnomedCT_LOINCDerivative_PRODUCTION_20271001T120000Z.zip", false).orElseThrow();
		assertEquals("LOINC", derivative.product());
		assertEquals(PackageType.DERIVATIVE, derivative.packageType());
	}

	@Test
	void currentConventionErrorsExplainTheRule() {
		assertTrue(conventions.validatePackageName("SnomedCT_ManagedServiceIE_PRODUCTION_20271021T120000Z.zip", false)
				.getFirst().contains("ManagedService"));
		assertTrue(conventions.validatePackageName("SnomedCT_InternationalRF2_PRODUCTION_20270701T120000Z.zip", false)
				.getFirst().contains("RF1/RF2"));
		assertTrue(conventions.validateFileName("der2_cRefset_AssociationReferenceSnapshot_INT_20270701.txt", false)
				.getFirst().contains("'Association'"));
		assertTrue(conventions.validateFileName("der2_Refset_SimpleRefsetDelta_INT_20270701.txt", false)
				.getFirst().contains("'Simple'"));
		assertTrue(conventions.validatePackageName("nonsense.zip", false).getFirst().contains("does not match"));
	}

	@Test
	void detectPackageConvention() {
		assertEquals(NamingConvention.CURRENT, conventions.detectPackageConvention("SnomedCT_International_PRODUCTION_20270701T120000Z.zip").orElseThrow());
		assertEquals(NamingConvention.LEGACY, conventions.detectPackageConvention("SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip").orElseThrow());
		assertEquals(NamingConvention.LEGACY, conventions.detectPackageConvention("SnomedCT_ManagedServiceIE_PRODUCTION_IE1000220_20250521T120000Z.zip").orElseThrow());
		assertTrue(conventions.detectPackageConvention("not-a-package.zip").isEmpty());
	}

	@Test
	void parseFileNames() {
		ReleaseFileName legacy = conventions.parseFileName("SnomedCT_X/Delta/Refset/Language/xder2_cRefset_LanguageDelta-en-GB_GB1000000_20250101.txt", true).orElseThrow();
		assertEquals("xder2_cRefset_LanguageDelta-en-GB_GB1000000_20250101.txt", legacy.name());
		assertTrue(legacy.beta());
		assertEquals("der2", legacy.fileType());
		assertEquals("cRefset", legacy.contentType());
		assertEquals("LanguageDelta-en-GB", legacy.contentSubType());
		assertEquals("Language", legacy.summary());
		assertEquals("Delta", legacy.releaseType());
		assertEquals("en-GB", legacy.languageCode());
		assertEquals("GB1000000", legacy.countryNamespace());
		assertNull(legacy.packageType());
		assertEquals("20250101", legacy.versionDate());
		assertEquals(".txt", legacy.extension());

		ReleaseFileName current = conventions.parseFileName("sct2_ConceptEdition_Snapshot_INT_20270701.txt", false).orElseThrow();
		assertEquals("Concept", current.contentType());
		assertEquals("", current.summary());
		assertEquals("INT", current.countryNamespace());
		assertEquals(PackageType.EDITION, current.packageType());

		ReleaseFileName untagged = conventions.parseFileName("sct2_RelationshipConcreteValues_Snapshot_INT_20270701.txt", false).orElseThrow();
		assertEquals("RelationshipConcreteValues", untagged.contentType());
		assertNull(untagged.packageType());

		ReleaseFileName refset = conventions.parseFileName("der2_cRefset_DICOMLanguageEditionSnapshot-en_INT_20240701.txt", false).orElseThrow();
		assertEquals("cRefset", refset.contentType());
		assertEquals("DICOMLanguageEditionSnapshot-en", refset.contentSubType());
		assertEquals("DICOMLanguage", refset.summary());
		assertEquals("Snapshot", refset.releaseType());
		assertEquals("en", refset.languageCode());
		assertEquals("INT", refset.countryNamespace());
		assertEquals(PackageType.EDITION, refset.packageType());

		ReleaseFileName extension = conventions.parseFileName("der2_cRefset_LanguageExtensionSnapshot-en_US1000124_20270901.txt", false).orElseThrow();
		assertEquals("Language", extension.summary());
		assertEquals("US1000124", extension.countryNamespace());
		assertEquals(PackageType.EXTENSION, extension.packageType());
	}

	@Test
	void packageTypeTagPlaceErrorsExplainTheRule() {
		assertTrue(conventions.validateFileName("der2_cRefsetEdition_LanguageSnapshot-en_INT_20270701.txt", false)
				.getFirst().contains("refset files"));
		assertTrue(conventions.validateFileName("sct2_Concept_EditionSnapshot_INT_20270701.txt", false)
				.getFirst().contains("core component files"));
	}

	@Test
	void convertContentSubTypeSummary() {
		assertEquals("Association", NamingConventions.convertContentSubTypeSummary("AssociationReference", NamingConvention.CURRENT));
		assertEquals("Simple", NamingConventions.convertContentSubTypeSummary("SimpleRefset", NamingConvention.CURRENT));
		assertEquals("AssociationReference", NamingConventions.convertContentSubTypeSummary("Association", NamingConvention.LEGACY));
		assertEquals("SimpleRefset", NamingConventions.convertContentSubTypeSummary("Simple", NamingConvention.LEGACY));
		assertEquals("ExtendedAssociation", NamingConventions.convertContentSubTypeSummary("ExtendedAssociation", NamingConvention.LEGACY));
		assertEquals("ExpressionAssociation", NamingConventions.convertContentSubTypeSummary("ExpressionAssociation", NamingConvention.LEGACY));
		assertEquals("SimpleMap", NamingConventions.convertContentSubTypeSummary("SimpleMap", NamingConvention.CURRENT));
		assertEquals("Language", NamingConventions.convertContentSubTypeSummary("Language", NamingConvention.CURRENT));
		assertNull(NamingConventions.convertContentSubTypeSummary(null, NamingConvention.CURRENT));
	}

	// ---- Configuration and startup check ----

	@Test
	void defaultPatternsCompile() {
		for (NamingConvention convention : NamingConvention.values()) {
			assertNotNull(conventions.getPackagePattern(convention));
			assertNotNull(conventions.getFilePattern(convention));
		}
	}

	@Test
	void overriddenPatternIsUsed() {
		NamingConventionProperties properties = new NamingConventionProperties();
		properties.getCurrent().setPackagePattern("^Pkg_(?<product>[A-Z]+)_(?<releaseStatus>PRODUCTION)_(?<releaseDate>\\d{8})T(?<releaseTime>\\d{6})Z$");
		NamingConventions custom = new NamingConventions(properties);

		PackageName name = custom.parsePackageName("Pkg_INT_PRODUCTION_20270701T120000Z", false).orElseThrow();
		assertEquals("INT", name.product());
		assertNull(name.packageType(), "optional groups missing from the pattern are null");
		assertFalse(custom.isValidPackageName("SnomedCT_International_PRODUCTION_20270701T120000Z.zip", false));
		assertTrue(custom.isValidPackageName("SnomedCT_InternationalRF2_PRODUCTION_20250101T120000Z.zip", true), "legacy keeps its default");
	}

	@Test
	void blankOverrideFallsBackToDefault() {
		NamingConventionProperties properties = new NamingConventionProperties();
		properties.getLegacy().setFilePattern("  ");
		assertEquals(NamingConventionProperties.DEFAULT_LEGACY_FILE_PATTERN, properties.getLegacy().getFilePattern());
	}

	@Test
	void startupCheckRejectsPatternThatDoesNotCompile() {
		NamingConventionProperties properties = new NamingConventionProperties();
		properties.getCurrent().setFilePattern("^(?<fileType>sct2_(");
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> new NamingConventions(properties));
		assertTrue(e.getMessage().contains("current file pattern does not compile"), e.getMessage());
	}

	@Test
	void startupCheckRejectsPatternMissingRequiredGroups() {
		NamingConventionProperties properties = new NamingConventionProperties();
		properties.getLegacy().setPackagePattern("^SnomedCT_.*$");
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> new NamingConventions(properties));
		assertTrue(e.getMessage().contains("legacy package pattern is missing required named groups [product, releaseDate, releaseStatus, releaseTime]"), e.getMessage());
	}

	@Test
	void nullNamesAreInvalidNotErrors() {
		assertTrue(conventions.parsePackageName(null, false).isEmpty());
		assertTrue(conventions.parseFileName(null, true).isEmpty());
		assertFalse(conventions.isValidPackageName(null, false));
	}
}
