package org.ihtsdo.otf.resourcemanager;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResourceConfigurationTest {
	@Test
	public void normalisePath() throws Exception {
		// Should always end up with either a blank path
		// or a path with no slash at the beginning and a slash at the end
		assertEquals("", ResourceConfiguration.normalisePath(null));
		assertEquals("", ResourceConfiguration.normalisePath(""));
		assertEquals("", ResourceConfiguration.normalisePath("/"));
		assertEquals("a/", ResourceConfiguration.normalisePath("a"));
		assertEquals("a/", ResourceConfiguration.normalisePath("/a/"));
		assertEquals("a/", ResourceConfiguration.normalisePath("/a"));
		assertEquals("a/", ResourceConfiguration.normalisePath("a/"));
		assertEquals("a/b/", ResourceConfiguration.normalisePath("a/b"));
		assertEquals("a/b/", ResourceConfiguration.normalisePath("/a/b/"));
		assertEquals("a/b/", ResourceConfiguration.normalisePath("/a/b"));
		assertEquals("a/b/", ResourceConfiguration.normalisePath("a/b/"));
	}

}
