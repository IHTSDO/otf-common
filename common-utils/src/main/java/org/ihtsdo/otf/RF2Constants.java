package org.ihtsdo.otf;

import java.util.Set;

public interface RF2Constants {

	int NOT_SET = -1;
	int NOT_FOUND = -1;
	int IMMEDIATE_CHILD = 1;
	int IMMEDIATE_PARENT = 1;
	int NA = -1;
	int NO_CHANGES_MADE = 0;
	int CHANGE_MADE = 1;
	Long SCTID_ROOT_CONCEPT = 138875005L;
	Long SCTID_IS_A_CONCEPT = 116680003L;

	int SCTID_MAX_LENGTH = 18;

	String GFOLDER_FALLBACK = "1bIRADym0omCgbD7064U-D24XGqAEg3gt"; //Reporting Engine Fallback

	Set<Long> NEVER_GROUPED_ATTRIBUTES = Set.of(
			1142139005L, 1142140007L, 1142141006L, 1142143009L,
			1148793005L, 1148965004L, 1148967007L, 1148968002L,
			1148969005L, 1149367008L, 1230370004L, 272741003L,
			320091000221107L, 411116001L, 726542003L, 733928003L,
			733930001L, 733931002L, 733932009L, 733933004L,
			736472000L, 736473005L, 736474004L, 736475003L,
			736476002L, 736518005L, 738774007L, 763032000L,
			766939001L, 774081006L, 774158006L, 774159003L,
			827081001L, 836358009L, 840560000L, 860781008L);

	int TAB_0 = 0;
	int TAB_1 = 1;
	int TAB_2 = 2;
	int TAB_3 = 3;
	int TAB_4 = 4;
	int TAB_5 = 5;

	enum ReportActionType {	API_ERROR, DEBUG_INFO, INFO, UNEXPECTED_CONDITION,
		CONCEPT_CHANGE_MADE, CONCEPT_ADDED, CONCEPT_INACTIVATED, CONCEPT_DELETED,
		AXIOM_CHANGE_MADE, AXIOM_INACTIVATED,
		EFFECTIVE_TIME_REVERTED,
		DESCRIPTION_CHANGE_MADE, DESCRIPTION_ACCEPTABILIY_CHANGED, DESCRIPTION_REACTIVATED,
		DESCRIPTION_ADDED, DESCRIPTION_INACTIVATED, DESCRIPTION_DELETED,
		CASE_SIGNIFICANCE_CHANGE_MADE, MODULE_CHANGE_MADE,
		RELATIONSHIP_ADDED, RELATIONSHIP_REPLACED, RELATIONSHIP_INACTIVATED, RELATIONSHIP_DELETED, RELATIONSHIP_MODIFIED,
		RELATIONSHIP_GROUP_ADDED,RELATIONSHIP_GROUP_REMOVED,RELATIONSHIP_GROUP_MODIFIED,
		NO_CHANGE, VALIDATION_ERROR, VALIDATION_CHECK, SKIPPING,
		REFSET_MEMBER_ADDED, REFSET_MEMBER_MODIFIED, REFSET_MEMBER_DELETED, REFSET_MEMBER_INACTIVATED, REFSET_MEMBER_REMOVED, REFSET_MEMBER_REACTIVATED,
		UNKNOWN, RELATIONSHIP_REACTIVATED,
		ASSOCIATION_ADDED, ASSOCIATION_REMOVED, ASSOCIATION_CHANGED,
		INACT_IND_ADDED, INACT_IND_MODIFIED, INACT_IND_INACTIVATED, INACT_IND_DELETED,
		LANG_REFSET_CREATED, LANG_REFSET_CLONED, LANG_REFSET_MODIFIED, LANG_REFSET_INACTIVATED,  LANG_REFSET_REACTIVATED,  LANG_REFSET_DELETED,
		CONFIGURATION_UPDATED, COMPONENT_REVERTED, COMPONENT_ADDED, COMPONENT_UPDATED, COMPONENT_DELETED,
		ANNOTATION_ADDED, ANNOTATION_REMOVED, ANNOTATION_CHANGED}

	enum Severity { NONE, LOW, MEDIUM, HIGH, CRITICAL }

	String SCTID_CORE_MODULE = "900000000000207008";
	String SCTID_MODEL_MODULE = "900000000000012004"; // |SNOMED CT model component module (core metadata concept)|
	String[] INTERNATIONAL_MODULES = new String[] { SCTID_CORE_MODULE, SCTID_MODEL_MODULE };
	String SCTID_US_MODULE = "731000124108";
	
	String SCTID_LOINC_PROJECT_MODULE = "715515008";
	String SCTID_LOINC_EXTENSION_MODULE = "11010000107";
	String SCTID_LOINC_SCHEMA = "30051010000102"; // |LOINC code identifier (core metadata concept) - use in Alt Identifier File

	String SCTID_NUVA_SCHEMA = "21002000101";
	String SCTID_NUVA_EXTENSION_MODULE = "11002000107";
	
	String SCTID_NPU_SCHEMA = "21003000106";
	String SCTID_NPU_EXTENSION_MODULE = "11003000102";
	

	String SCTID_OWL_AXIOM_REFSET = "733073007"; // |OWL axiom reference set (foundation metadata concept)|
	String SCTID_COMP_ANNOT_REFSET = "1292992004"; // |Component annotation with string value reference set (foundation metadata concept)|
	String SCTID_MEMB_ANNOT_REFSET = "1292995002"; // |Member annotation with string value reference set (foundation metadata concept)|

	//ECL Constants
	String DESCENDANT = "<";
	String DESCENDANT_OR_SELF = "<<";
	String PIPE = "|";
	String ESCAPED_PIPE = "\\|";
	char PIPE_CHAR = '|';
	char SPACE_CHAR = ' ';
	String UNION = "AND";
	String ATTRIBUTE_SEPARATOR = ",";
	String DASH = "-";
	//This laxity is causing problems with these odd characters appearing inside a term
	//Let's stop being generous and be firm:  terms start and end with pipes and if you miss one out,
	//then we won't parse it correctly.
	String BREAK = "===========================================";

	//Description Type SCTIDs
	String SCTID_SYN = "900000000000013009";
	String SCTID_FSN = "900000000000003001";
	String SCTID_DEF = "900000000000550004";

	String LANG_EN = "en";

	String FULLY_DEFINED_SCTID = "900000000000073002";
	String FULLY_SPECIFIED_NAME = "900000000000003001";
	String ADDITIONAL_RELATIONSHIP = "900000000000227009";
	String SPACE = " ";
	String COMMA = ",";
	String COMMA_QUOTE = ",\"";
	String QUOTE_COMMA = "\",";
	String QUOTE_COMMA_QUOTE = "\",\"";
	String TAB = "\t";
	char TAB_CHAR = '\t';
	String CSV_FIELD_DELIMITER = COMMA;
	String TSV_FIELD_DELIMITER = TAB;
	String QUOTE = "\"";
	String INGREDIENT_SEPARATOR = "+";
	String INGREDIENT_SEPARATOR_ESCAPED = "\\+";

	String CONCEPT_INT_PARTITION = "00";
	String DESC_INT_PARTITION = "01";
	String REL_INT_PARTITION = "02";

	String GB_ENG_LANG_REFSET = "900000000000508004";
	String US_ENG_LANG_REFSET = "900000000000509007";
	String[] ENGLISH_DIALECTS = {GB_ENG_LANG_REFSET, US_ENG_LANG_REFSET};
	String[] US_DIALECT = {US_ENG_LANG_REFSET};
	String[] GB_DIALECT = {GB_ENG_LANG_REFSET};


	String SCTID_PREFERRED_TERM = "900000000000548007";
	String SCTID_ACCEPTABLE_TERM = "900000000000549004";

	String SCTID_ENTIRE_TERM_CASE_SENSITIVE = "900000000000017005";
	String SCTID_ENTIRE_TERM_CASE_INSENSITIVE = "900000000000448009";
	String SCTID_ONLY_INITIAL_CHAR_CASE_INSENSITIVE = "900000000000020002";

	final String SEMANTIC_TAG_START = "(";

	static String SCTID_STATED_RELATIONSHIP = "900000000000010007";
	static String SCTID_INFERRED_RELATIONSHIP = "900000000000011006";
	static String SCTID_QUALIFYING_RELATIONSHIP = "900000000000225001";
	static String SCTID_ADDITIONAL_RELATIONSHIP = "900000000000227009";


	//Inactivation Indicator Reasons
	enum InactivationIndicator {AMBIGUOUS, DUPLICATE, OUTDATED, ERRONEOUS, LIMITED, MOVED_ELSEWHERE,
		PENDING_MOVE, INAPPROPRIATE, CONCEPT_NON_CURRENT, RETIRED, NONCONFORMANCE_TO_EDITORIAL_POLICY,
		NOT_SEMANTICALLY_EQUIVALENT, MEANING_OF_COMPONENT_UNKNOWN, CLASSIFICATION_DERIVED_COMPONENT, GRAMMATICAL_DESCRIPTION_ERROR}

	String SCTID_INACT_AMBIGUOUS ="900000000000484002";  // |Ambiguous component (foundation metadata concept)|
	String SCTID_INACT_MOVED_ELSEWHERE  ="900000000000487009";  // |Component moved elsewhere (foundation metadata concept)|
	String SCTID_INACT_CONCEPT_NON_CURRENT  ="900000000000495008";  // |Concept non-current (foundation metadata concept)|
	String SCTID_INACT_DUPLICATE  ="900000000000482003";  // |Duplicate component (foundation metadata concept)|
	String SCTID_INACT_ERRONEOUS  ="900000000000485001";  // |Erroneous component (foundation metadata concept)|
	String SCTID_INACT_INAPPROPRIATE  ="900000000000494007";  // |Inappropriate component (foundation metadata concept)|
	String SCTID_INACT_LIMITED  ="900000000000486000";  // |Limited component (foundation metadata concept)|
	String SCTID_INACT_OUTDATED  ="900000000000483008";  // |Outdated component (foundation metadata concept)|
	String SCTID_INACT_PENDING_MOVE  ="900000000000492006";  // |Pending move (foundation metadata concept)|
	String SCTID_INACT_NON_CONFORMANCE  = "723277005"; // |Nonconformance to editorial policy component (foundation metadata concept)|
	String SCTID_INACT_NOT_SEMANTICALLY_EQUIVALENT  = "723278000";  //|Not semantically equivalent component (foundation metadata concept)|
	String SCTID_INACT_MEANING_OF_COMPONENT_UNKNOWN = "1186919006"; // |Meaning of component unknown (foundation metadata concept)|
	String SCTID_INACT_CLASS_DERIVED_COMPONENT  = "1186917008"; // |Classification derived component (foundation metadata concept)|
	String SCTID_INACT_GRAMMATICAL_DESCRIPTION_ERROR = "1217318005"; // |Grammatical description error (foundation metadata concept)|

	// Associations
	enum Association { WAS_A, REPLACED_BY, SAME_AS, POSS_EQUIV_TO, MOVED_TO, ALTERNATIVE, ANATOMY_STRUC_ENTIRE, POSS_REPLACED_BY, PARTIALLY_EQUIV_TO, ANATOMY_STRUC_PART, REFERS_TO }
	String SCTID_ASSOC_WAS_A_REFSETID = "900000000000528000"; // |WAS A association reference set (foundation metadata concept)|
	String SCTID_ASSOC_REPLACED_BY_REFSETID = "900000000000526001"; // |REPLACED BY association reference set (foundation metadata concept)|
	String SCTID_ASSOC_POSS_REPLACED_BY_REFSETID = "1186921001"; // |POSSIBLY REPLACED BY association reference set (foundation metadata concept)|
	String SCTID_ASSOC_SAME_AS_REFSETID = "900000000000527005"; // |SAME AS association reference set (foundation metadata concept)|"
	String SCTID_ASSOC_POSS_EQUIV_REFSETID = "900000000000523009" ;// |POSSIBLY EQUIVALENT TO association reference set (foundation metadata concept)|"
	String SCTID_ASSOC_PART_EQUIV_REFSETID = "1186924009" ;// |PARTIALLY EQUIVALENT TO association reference set (foundation metadata concept)|"
	String SCTID_ASSOC_MOVED_TO_REFSETID = "900000000000524003" ;// |MOVED TO association reference set (foundation metadata concept)|"
	String SCTID_ASSOC_ALTERNATIVE_REFSETID = "900000000000530003";  //ALTERNATIVE association reference set (foundation metadata concept)
	String SCTID_ASSOC_REFERS_TO_REFSETID = "900000000000531004";  //REFERS TO association reference set (foundation metadata concept)

	String SCTID_ASSOC_ANATOMY_STRUC_ENTIRE_REFSETID = "734138000";  //Anatomy structure and entire association reference set (foundation metadata concept)
	String SCTID_ASSOC_ANATOMY_STRUC_PART_REFSETID = "734139008";  //Anatomy structure and part association reference set (foundation metadata concept)

	//Module Dependency Reference Set
	String SCTID_MODULE_DEPENDENCY_REFSET = "900000000000534007"; // |Module dependency reference set (foundation metadata concept)|

	//Inactivation Indicator Refsets
	String SCTID_CON_INACT_IND_REFSET = "900000000000489007";
	String SCTID_DESC_INACT_IND_REFSET = "900000000000490003";

	enum DefinitionStatus { PRIMITIVE, FULLY_DEFINED }
	static String SCTID_PRIMITIVE = "900000000000074008";
	static String SCTID_FULLY_DEFINED = "900000000000073002";

	static int UNGROUPED = 0;
	static int SELFGROUPED = -1;
	enum Modifier { EXISTENTIAL, UNIVERSAL}
	static String SCTID_EXISTENTIAL_MODIFIER = "900000000000451002";
	static String SCTID_UNIVERSAL_MODIFIER = "900000000000450001";
	
	//Content types
	String SCTID_PRE_COORDINATED_CONTENT = "723594008"; //All precoordinated SNOMED CT content
	String SCTID_POST_COORDINATED_CONTENT = "723595009"; //"All postcoordinated SNOMED CT content
	String SCTID_ALL_CONTENT = "723596005"; // All SNOMED CT content
	String SCTID_NEW_PRE_COORDINATED_CONTENT = "723593002"; //All new precoordinated SNOMED CT content



	enum ActiveState { ACTIVE, INACTIVE, BOTH }

	enum Acceptability { ACCEPTABLE, PREFERRED, BOTH, NONE }

	enum CaseSignificance { ENTIRE_TERM_CASE_SENSITIVE, CASE_INSENSITIVE, INITIAL_CHARACTER_CASE_INSENSITIVE }
	String CS = "CS";
	String ci= "ci";
	String cI = "cI";

	String DELTA = "Delta";
	String SNAPSHOT = "Snapshot";
	String FULL = "Full";
	String TYPE = "TYPE";

	enum PartitionIdentifier {CONCEPT, DESCRIPTION, RELATIONSHIP}

	enum CharacteristicType {	STATED_RELATIONSHIP, INFERRED_RELATIONSHIP,
										QUALIFYING_RELATIONSHIP, ADDITIONAL_RELATIONSHIP, ALL}

	enum FileType { DELTA, SNAPSHOT, FULL }

	enum ConceptType { PRODUCT_STRENGTH, MEDICINAL_ENTITY, PRODUCT,
		MEDICINAL_PRODUCT_FORM, MEDICINAL_PRODUCT_FORM_ONLY,
		MEDICINAL_PRODUCT, MEDICINAL_PRODUCT_ONLY, GROUPER,
		PRODUCT_ROLE, THERAPEUTIC_ROLE, VMPF, VCD, VMP, UNKNOWN,
		ANATOMY, CLINICAL_DRUG, SUBSTANCE, STRUCTURAL_GROUPER, DISPOSITION_GROUPER,
		STRUCTURE_AND_DISPOSITION_GROUPER,
		REAL_MEDICINAL_PRODUCT_FORM, REAL_MEDICINAL_PRODUCT_FORM_ONLY, REAL_CLINICAL_DRUG}

	enum CardinalityExpressions { AT_LEAST_ONE, EXACTLY_ONE }

	enum DescriptionType { FSN, SYNONYM, TEXT_DEFINITION}

	enum ChangeStatus { CHANGE_MADE, CHANGE_NOT_REQUIRED, NO_CHANGE_MADE }

	String FIELD_DELIMITER = "\t";
	String LINE_DELIMITER = "\r\n";
	String ACTIVE_FLAG = "1";
	String INACTIVE_FLAG = "0";
	String HEADER_ROW = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId\r\n";

	// Common column headers
	String COL_ID = "id";
	String COL_EFFECTIVE_TIME = "effectiveTime";
	String COL_ACTIVE = "active";
	String COL_MODULE_ID = "moduleId";
	String COL_TYPE_ID = "typeId";
	String COL_REFSET_ID = "refsetId";
	String COL_REFERENCED_COMPONENT_ID = "referencedComponentId";
	String COL_VALUE = "value";

	//Common columns
	int IDX_ID = 0;
	int IDX_EFFECTIVETIME = 1;
	int IDX_ACTIVE = 2;
	int IDX_MODULEID = 3;

	// Relationship columns
	int REL_IDX_ID = 0;
	int REL_IDX_EFFECTIVETIME = 1;
	int REL_IDX_ACTIVE = 2;
	int REL_IDX_MODULEID = 3;
	int REL_IDX_SOURCEID = 4;
	int REL_IDX_DESTINATIONID = 5;
	int REL_IDX_VALUE = 5;
	int REL_IDX_RELATIONSHIPGROUP = 6;
	int REL_IDX_TYPEID = 7;
	int REL_IDX_CHARACTERISTICTYPEID = 8;
	int REL_IDX_MODIFIERID = 9;
	int REL_MAX_COLUMN = 9;

	// Concept columns
	// id effectiveTime active moduleId definitionStatusId
	int CON_IDX_ID = 0;
	int CON_IDX_EFFECTIVETIME = 1;
	int CON_IDX_ACTIVE = 2;
	int CON_IDX_MODULEID = 3;
	int CON_IDX_DEFINITIONSTATUSID = 4;

	// Description columns
	// id effectiveTime active moduleId conceptId languageCode typeId term caseSignificanceId
	int DES_IDX_ID = 0;
	int DES_IDX_EFFECTIVETIME = 1;
	int DES_IDX_ACTIVE = 2;
	int DES_IDX_MODULEID = 3;
	int DES_IDX_CONCEPTID = 4;
	int DES_IDX_LANGUAGECODE = 5;
	int DES_IDX_TYPEID = 6;
	int DES_IDX_TERM = 7;
	int DES_IDX_CASESIGNIFICANCEID = 8;

	// Language Refset columns
	// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	acceptabilityId
	int LANG_IDX_ID = 0;
	int LANG_IDX_EFFECTIVETIME = 1;
	int LANG_IDX_ACTIVE = 2;
	int LANG_IDX_MODULEID = 3;
	int LANG_IDX_REFSETID = 4;
	int LANG_IDX_REFCOMPID = 5;
	int LANG_IDX_ACCEPTABILITY_ID = 6;

	// Component Annotation Refset columns
	// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	languageDialectCode	typeId	value
	int COMP_ANNOT_IDX_ID = 0;
	int COMP_ANNOT_IDX_EFFECTIVETIME = 1;
	int COMP_ANNOT_IDX_ACTIVE = 2;
	int COMP_ANNOT_IDX_MODULEID = 3;
	int COMP_ANNOT_IDX_REFSETID = 4;
	int COMP_ANNOT_IDX_REFCOMPID = 5;
	int COMP_ANNOT_IDX_LANG_DIALECT_CODE = 6;
	int COMP_ANNOT_IDX_TYPEID = 7;
	int COMP_ANNOT_IDX_VALUE = 8;

	// Inactivation Refset columns
	// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	reasonId
	int INACT_IDX_ID = 0;
	int INACT_IDX_EFFECTIVETIME = 1;
	int INACT_IDX_ACTIVE = 2;
	int INACT_IDX_MODULEID = 3;
	int INACT_IDX_REFSETID = 4;
	int INACT_IDX_REFCOMPID = 5;
	int INACT_IDX_REASON_ID = 6;

	// Association Refset columns
	// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	reasonId
	int ASSOC_IDX_ID = 0;
	int ASSOC_IDX_EFFECTIVETIME = 1;
	int ASSOC_IDX_ACTIVE = 2;
	int ASSOC_IDX_MODULEID = 3;
	int ASSOC_IDX_REFSETID = 4;
	int ASSOC_IDX_REFCOMPID = 5;
	int ASSOC_IDX_TARGET = 6;

	// Module Dependency Refset columns
	// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	sourceEffectiveTime	targetEffectiveTime
	int MDRS_IDX_ID = 0;
	int MDRS_IDX_EFFECTIVETIME = 1;
	int MDRS_IDX_ACTIVE = 2;
	int MDRS_IDX_MODULEID = 3;
	int MDRS_IDX_REFSETID = 4;
	int MDRS_IDX_REFCOMPID = 5;
	int MDRS_IDX_SOURCE_EFFECTIVE_TIME = 6;
	int MDRS_IDX_TARGET_EFFECTIVE_TIME = 7;

	// Refset columns
	int REF_IDX_ID = 0;
	int REF_IDX_EFFECTIVETIME = 1;
	int REF_IDX_ACTIVE = 2;
	int REF_IDX_MODULEID = 3;
	int REF_IDX_REFSETID = 4;
	int REF_IDX_REFCOMPID = 5;
	int REF_IDX_FIRST_ADDITIONAL = 6;
	int REF_IDX_AXIOM_STR = 6;
	
	//MRCM Attribute Range columns
	int MRCM_ATTRIB_RANGE_CONTENT_TYPE = 9;

	//MRCM Attribute Domain columns
	int MRCM_ATTRIB_DOMAIN_CONTENT_TYPE = 11;
}
