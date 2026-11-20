package org.ihtsdo.otf.naming;

/**
 * The SNOMED CT release package and RF2 file naming conventions that systems must support.
 */
public enum NamingConvention {

	/** The convention in use before the file and package naming changes. */
	LEGACY,

	/** The convention introduced by the file and package naming changes. */
	CURRENT;

	/**
	 * @param legacyNaming the code system's legacy naming flag
	 * @return {@link #LEGACY} when the flag is set, otherwise {@link #CURRENT}
	 */
	public static NamingConvention forCodeSystem(boolean legacyNaming) {
		return legacyNaming ? LEGACY : CURRENT;
	}
}
