package org.ihtsdo.otf.naming;

import java.util.Arrays;
import java.util.Optional;

/**
 * The Edition / Extension / Derivative tag that may appear in package names and (under the current convention) in
 * RF2 file names.
 */
public enum PackageType {

	EDITION("Edition"),
	EXTENSION("Extension"),
	DERIVATIVE("Derivative");

	private final String tag;

	PackageType(String tag) {
		this.tag = tag;
	}

	/**
	 * @return the tag exactly as it appears in a package or file name, e.g. {@code Edition}
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @param tag a tag as it appears in a name, e.g. {@code Extension}; may be null
	 * @return the matching type, or empty if the tag is null or not recognised
	 */
	public static Optional<PackageType> fromTag(String tag) {
		if (tag == null) {
			return Optional.empty();
		}
		return Arrays.stream(values()).filter(t -> t.tag.equals(tag)).findFirst();
	}
}
