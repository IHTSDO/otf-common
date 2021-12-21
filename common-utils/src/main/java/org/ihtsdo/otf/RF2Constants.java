package org.ihtsdo.otf;

import java.util.ArrayList;
import java.util.List;

public interface RF2Constants {

	static int NOT_SET = -1;
	static int NOT_FOUND = -1;
	static int IMMEDIATE_CHILD = 1;
	static int IMMEDIATE_PARENT = 1;
	static int NA = -1;
	static int NO_CHANGES_MADE = 0;
	static int CHANGE_MADE = 1;
	final Long SCTID_ROOT_CONCEPT = 138875005L;
	final Long SCTID_IS_A_CONCEPT = 116680003L;
	
	final static int TAB_0 = 0;
	final static int TAB_1 = 1;
	
	public enum ReportActionType {	API_ERROR, DEBUG_INFO, INFO, UNEXPECTED_CONDITION,
		CONCEPT_CHANGE_MADE, CONCEPT_ADDED, CONCEPT_INACTIVATED, CONCEPT_DELETED,
		AXIOM_CHANGE_MADE,
		DESCRIPTION_CHANGE_MADE, DESCRIPTION_ACCEPTABILIY_CHANGED, DESCRIPTION_REACTIVATED,
		DESCRIPTION_ADDED, DESCRIPTION_INACTIVATED, DESCRIPTION_DELETED,
		CASE_SIGNIFICANCE_CHANGE_MADE, MODULE_CHANGE_MADE, 
		RELATIONSHIP_ADDED, RELATIONSHIP_REPLACED, RELATIONSHIP_INACTIVATED, RELATIONSHIP_DELETED, RELATIONSHIP_MODIFIED, 
		RELATIONSHIP_GROUP_ADDED,RELATIONSHIP_GROUP_REMOVED,
		NO_CHANGE, VALIDATION_ERROR, VALIDATION_CHECK, SKIPPING,
		REFSET_MEMBER_ADDED, REFSET_MEMBER_MODIFIED, REFSET_MEMBER_DELETED, REFSET_MEMBER_INACTIVATED, REFSET_MEMBER_REMOVED, REFSET_MEMBER_REACTIVATED,
		UNKNOWN, RELATIONSHIP_REACTIVATED, 
		ASSOCIATION_ADDED, ASSOCIATION_REMOVED, ASSOCIATION_CHANGED, 
		INACT_IND_ADDED, INACT_IND_MODIFIED,
		LANG_REFSET_CREATED, LANG_REFSET_MODIFIED, LANG_REFSET_INACTIVATED, LANG_REFSET_DELETED,
		CONFIGURATION_UPDATED};
		
	public enum Severity { NONE, LOW, MEDIUM, HIGH, CRITICAL }; 
	
	static String SCTID_CORE_MODULE = "900000000000207008";
	static String SCTID_MODEL_MODULE = "900000000000012004"; // |SNOMED CT model component module (core metadata concept)|
	static String[] INTERNATIONAL_MODULES = new String[] { SCTID_CORE_MODULE, SCTID_MODEL_MODULE };
	static String SCTID_US_MODULE = "731000124108";
	static String SCTID_LOINC_MODULE = "715515008";
	static String SCTID_OWL_AXIOM_REFSET = "733073007"; // |OWL axiom reference set (foundation metadata concept)|"
	
	//ECL Constants
	static String DESCENDANT = "<";
	static String DESCENDANT_OR_SELF = "<<";
	static String PIPE = "|";
	static String ESCAPED_PIPE = "\\|";
	static char PIPE_CHAR = '|';
	static char SPACE_CHAR = ' ';
	static String UNION = "AND";
	static String ATTRIBUTE_SEPARATOR = ",";
	static String DASH = "-";
	static char[] termTerminators = new char[] {'|', ':', '+', '{', ',', '}', '=' };
	static String BREAK = "===========================================";

	//Description Type SCTIDs
	static String SYN = "900000000000013009";
	static String FSN = "900000000000003001";
	static String DEF = "900000000000550004"; 
	
	static String LANG_EN = "en";
	
	static final String FULLY_DEFINED_SCTID = "900000000000073002";
	static final String FULLY_SPECIFIED_NAME = "900000000000003001";
	final String ADDITIONAL_RELATIONSHIP = "900000000000227009";
	final String SPACE = " ";
	final String COMMA = ",";
	final String COMMA_QUOTE = ",\"";
	final String QUOTE_COMMA = "\",";
	final String QUOTE_COMMA_QUOTE = "\",\"";
	final String TAB = "\t";
	final String CSV_FIELD_DELIMITER = COMMA;
	final String TSV_FIELD_DELIMITER = TAB;
	final String QUOTE = "\"";
	final String INGREDIENT_SEPARATOR = "+";
	final String INGREDIENT_SEPARATOR_ESCAPED = "\\+";
	
	final String CONCEPT_INT_PARTITION = "00";
	final String DESC_INT_PARTITION = "01";
	final String REL_INT_PARTITION = "02";
	
	static final String GB_ENG_LANG_REFSET = "900000000000508004";
	static final String US_ENG_LANG_REFSET = "900000000000509007";
	static final String[] ENGLISH_DIALECTS = {GB_ENG_LANG_REFSET, US_ENG_LANG_REFSET};
	static final String[] US_DIALECT = {US_ENG_LANG_REFSET};
	static final String[] GB_DIALECT = {GB_ENG_LANG_REFSET};
	
	
	static final String SCTID_PREFERRED_TERM = "900000000000548007";
	static final String SCTID_ACCEPTABLE_TERM = "900000000000549004";
	
	static final String SCTID_ENTIRE_TERM_CASE_SENSITIVE = "900000000000017005";
	static final String SCTID_ENTIRE_TERM_CASE_INSENSITIVE = "900000000000448009";
	static final String SCTID_ONLY_INITIAL_CHAR_CASE_INSENSITIVE = "900000000000020002";
	
	final public String SEMANTIC_TAG_START = "(";
	
	public static String SCTID_STATED_RELATIONSHIP = "900000000000010007";
	public static String SCTID_INFERRED_RELATIONSHIP = "900000000000011006";
	public static String SCTID_QUALIFYING_RELATIONSHIP = "900000000000225001";
	public static String SCTID_ADDITIONAL_RELATIONSHIP = "900000000000227009";
	
	
	//Inactivation Indicator Reasons
	enum InactivationIndicator {AMBIGUOUS, DUPLICATE, OUTDATED, ERRONEOUS, LIMITED, MOVED_ELSEWHERE, 
		PENDING_MOVE, INAPPROPRIATE, CONCEPT_NON_CURRENT, RETIRED, NONCONFORMANCE_TO_EDITORIAL_POLICY, NOT_EQUIVALENT};
	public static final String SCTID_INACT_AMBIGUOUS ="900000000000484002";  // |Ambiguous component (foundation metadata concept)|
	public static final String SCTID_INACT_MOVED_ELSEWHERE  ="900000000000487009";  // |Component moved elsewhere (foundation metadata concept)|
	public static final String SCTID_INACT_CONCEPT_NON_CURRENT  ="900000000000495008";  // |Concept non-current (foundation metadata concept)|
	public static final String SCTID_INACT_DUPLICATE  ="900000000000482003";  // |Duplicate component (foundation metadata concept)|
	public static final String SCTID_INACT_ERRONEOUS  ="900000000000485001";  // |Erroneous component (foundation metadata concept)|
	public static final String SCTID_INACT_INAPPROPRIATE  ="900000000000494007";  // |Inappropriate component (foundation metadata concept)|
	public static final String SCTID_INACT_LIMITED  ="900000000000486000";  // |Limited component (foundation metadata concept)|
	public static final String SCTID_INACT_OUTDATED  ="900000000000483008";  // |Outdated component (foundation metadata concept)|
	public static final String SCTID_INACT_PENDING_MOVE  ="900000000000492006";  // |Pending move (foundation metadata concept)|
	public static final String SCTID_INACT_NON_CONFORMANCE  = "723277005"; // |Nonconformance to editorial policy component (foundation metadata concept)|
	public static final String SCTID_INACT_NOT_EQUIVALENT  = "723278000";  //|Not semantically equivalent component (foundation metadata concept)|
	public static final String SCTID_INACT_COMPONENT_MEANING_UNKNOWN  = "1186919006"; // |Meaning of component unknown (foundation metadata concept)|
	public static final String SCTID_INACT_CLASS_DERIVED_COMPONENT  = "1186917008"; // |Classification derived component (foundation metadata concept)|
	
	// Associations
	enum Association { WAS_A, REPLACED_BY, SAME_AS, POSS_EQUIV_TO, MOVED_TO, ALTERNATIVE, ANATOMY_STRUC_ENTIRE, POSS_REPLACED_BY, PARTIALLY_EQUIV_TO, ANATOMY_STRUC_PART }
	public static final String SCTID_ASSOC_WAS_A_REFSETID = "900000000000528000"; // |WAS A association reference set (foundation metadata concept)|
	public static final String SCTID_ASSOC_REPLACED_BY_REFSETID = "900000000000526001"; // |REPLACED BY association reference set (foundation metadata concept)|
	public static final String SCTID_ASSOC_POSS_REPLACED_BY_REFSETID = "1186921001"; // |POSSIBLY REPLACED BY association reference set (foundation metadata concept)|
	public static final String SCTID_ASSOC_SAME_AS_REFSETID = "900000000000527005"; // |SAME AS association reference set (foundation metadata concept)|"
	public static final String SCTID_ASSOC_POSS_EQUIV_REFSETID = "900000000000523009" ;// |POSSIBLY EQUIVALENT TO association reference set (foundation metadata concept)|"
	public static final String SCTID_ASSOC_PARTIALLY_EQUIV_REFSETID = "1186924009" ;// |PARTIALLY EQUIVALENT TO association reference set (foundation metadata concept)|"
	public static final String SCTID_ASSOC_MOVED_TO_REFSETID = "900000000000524003" ;// |MOVED TO association reference set (foundation metadata concept)|"
	public static final String SCTID_ASSOC_ALTERNATIVE_REFSETID = "900000000000530003";  //ALTERNATIVE association reference set (foundation metadata concept)
	
	public static final String SCTID_ASSOC_ANATOMY_STRUC_ENTIRE_REFSETID = "734138000";  //Anatomy structure and entire association reference set (foundation metadata concept)
	public static final String SCTID_ASSOC_ANATOMY_STRUC_PART_REFSETID = "734139008";  //Anatomy structure and part association reference set (foundation metadata concept)

	
	//Inactivation Indicator Refsets
	public static final String SCTID_CON_INACT_IND_REFSET = "900000000000489007";
	public static final String SCTID_DESC_INACT_IND_REFSET = "900000000000490003";
	
	public enum DefinitionStatus { PRIMITIVE, FULLY_DEFINED };
	public static String SCTID_PRIMITIVE = "900000000000074008";
	public static String SCTID_FULLY_DEFINED = "900000000000073002";
	
	public static int UNGROUPED = 0;
	public static int SELFGROUPED = -1;
	public enum Modifier { EXISTENTIAL, UNIVERSAL};
	public static String SCTID_EXISTENTIAL_MODIFIER = "900000000000451002";
	public static String SCTID_UNIVERSAL_MODIFIER = "900000000000450001";
	
	
	public enum ActiveState { ACTIVE, INACTIVE, BOTH };
	
	public enum Acceptability { ACCEPTABLE, PREFERRED, BOTH, NONE };
	
	public enum CaseSignificance { ENTIRE_TERM_CASE_SENSITIVE, CASE_INSENSITIVE, INITIAL_CHARACTER_CASE_INSENSITIVE };
	public static String CS = "CS";
	public static String ci= "ci";
	public static String cI = "cI";
	
	public static final String DELTA = "Delta";
	public static final String SNAPSHOT = "Snapshot";
	public static final String FULL = "Full";
	public static final String TYPE = "TYPE";
	
	public enum PartitionIdentifier {CONCEPT, DESCRIPTION, RELATIONSHIP};
	
	public enum CharacteristicType {	STATED_RELATIONSHIP, INFERRED_RELATIONSHIP, 
										QUALIFYING_RELATIONSHIP, ADDITIONAL_RELATIONSHIP, ALL};
	
	public enum FileType { DELTA, SNAPSHOT, FULL };

	public enum ChangeType { NEW, INACTIVATION, REACTIVATION, MODIFIED, UNKNOWN }
	
	public enum ConceptType { PRODUCT_STRENGTH, MEDICINAL_ENTITY, PRODUCT, 
		MEDICINAL_PRODUCT_FORM, MEDICINAL_PRODUCT_FORM_ONLY,
		MEDICINAL_PRODUCT, MEDICINAL_PRODUCT_ONLY, GROUPER, 
		PRODUCT_ROLE, THERAPEUTIC_ROLE, VMPF, VCD, VMP, UNKNOWN, 
		ANATOMY, CLINICAL_DRUG, SUBSTANCE, STRUCTURAL_GROUPER, DISPOSITION_GROUPER, STRUCTURE_AND_DISPOSITION_GROUPER};
	
	public enum CardinalityExpressions { AT_LEAST_ONE, EXACTLY_ONE };
	
	public enum DescriptionType { FSN, SYNONYM, TEXT_DEFINITION};
	
	public enum ChangeStatus { CHANGE_MADE, CHANGE_NOT_REQUIRED, NO_CHANGE_MADE };
	
	public static final String FIELD_DELIMITER = "\t";
	public static final String LINE_DELIMITER = "\r\n";
	public static final String ACTIVE_FLAG = "1";
	public static final String INACTIVE_FLAG = "0";
	public static final String HEADER_ROW = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId\r\n";

	//Common columns
	public static final int IDX_ID = 0;
	public static final int IDX_EFFECTIVETIME = 1;
	public static final int IDX_ACTIVE = 2;
	public static final int IDX_MODULEID = 3;
	
	// Relationship columns
	public static final int REL_IDX_ID = 0;
	public static final int REL_IDX_EFFECTIVETIME = 1;
	public static final int REL_IDX_ACTIVE = 2;
	public static final int REL_IDX_MODULEID = 3;
	public static final int REL_IDX_SOURCEID = 4;
	public static final int REL_IDX_DESTINATIONID = 5;
	public static final int REL_IDX_VALUE = 5;
	public static final int REL_IDX_RELATIONSHIPGROUP = 6;
	public static final int REL_IDX_TYPEID = 7;
	public static final int REL_IDX_CHARACTERISTICTYPEID = 8;
	public static final int REL_IDX_MODIFIERID = 9;
	public static final int REL_MAX_COLUMN = 9;

	// Concept columns
	// id effectiveTime active moduleId definitionStatusId
	public static final int CON_IDX_ID = 0;
	public static final int CON_IDX_EFFECTIVETIME = 1;
	public static final int CON_IDX_ACTIVE = 2;
	public static final int CON_IDX_MODULID = 3;
	public static final int CON_IDX_DEFINITIONSTATUSID = 4;

	// Description columns
	// id effectiveTime active moduleId conceptId languageCode typeId term caseSignificanceId
	public static final int DES_IDX_ID = 0;
	public static final int DES_IDX_EFFECTIVETIME = 1;
	public static final int DES_IDX_ACTIVE = 2;
	public static final int DES_IDX_MODULID = 3;
	public static final int DES_IDX_CONCEPTID = 4;
	public static final int DES_IDX_LANGUAGECODE = 5;
	public static final int DES_IDX_TYPEID = 6;
	public static final int DES_IDX_TERM = 7;
	public static final int DES_IDX_CASESIGNIFICANCEID = 8;
	
	// Language Refset columns
	// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	acceptabilityId
	public static final int LANG_IDX_ID = 0;
	public static final int LANG_IDX_EFFECTIVETIME = 1;
	public static final int LANG_IDX_ACTIVE = 2;
	public static final int LANG_IDX_MODULID = 3;
	public static final int LANG_IDX_REFSETID = 4;
	public static final int LANG_IDX_REFCOMPID = 5;
	public static final int LANG_IDX_ACCEPTABILITY_ID = 6;
	
	// Inactivation Refset columns
	// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	reasonId
	public static final int INACT_IDX_ID = 0;
	public static final int INACT_IDX_EFFECTIVETIME = 1;
	public static final int INACT_IDX_ACTIVE = 2;
	public static final int INACT_IDX_MODULID = 3;
	public static final int INACT_IDX_REFSETID = 4;
	public static final int INACT_IDX_REFCOMPID = 5;
	public static final int INACT_IDX_REASON_ID = 6;
	
	// Association Refset columns
	// id	effectiveTime	active	moduleId	refsetId	referencedComponentId	reasonId
	public static final int ASSOC_IDX_ID = 0;
	public static final int ASSOC_IDX_EFFECTIVETIME = 1;
	public static final int ASSOC_IDX_ACTIVE = 2;
	public static final int ASSOC_IDX_MODULID = 3;
	public static final int ASSOC_IDX_REFSETID = 4;
	public static final int ASSOC_IDX_REFCOMPID = 5;
	public static final int ASSOC_IDX_TARGET = 6;
	
	// Refset columns
	public static final int REF_IDX_ID = 0;
	public static final int REF_IDX_EFFECTIVETIME = 1;
	public static final int REF_IDX_ACTIVE = 2;
	public static final int REF_IDX_MODULEID = 3;
	public static final int REF_IDX_REFSETID = 4;
	public static final int REF_IDX_REFCOMPID = 5;
	public static final int REF_IDX_FIRST_ADDITIONAL = 6;
	public static final int REF_IDX_AXIOM_STR = 6;
}
