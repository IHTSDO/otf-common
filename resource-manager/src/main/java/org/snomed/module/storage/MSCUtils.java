package org.snomed.module.storage;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.ihtsdo.otf.RF2Constants.SCTID_CORE_MODULE;

public class MSCUtils {

	public static final String SLASH = "/";

	private static final String EDITION_URI_TEMPLATE =
			"http://snomed.info/sct/%s";

	private static final String VERSIONED_EDITION_URI_TEMPLATE =
			"http://snomed.info/sct/%s/version/%s";

	public static final URI CORE_MODULE_URI = editionUri(SCTID_CORE_MODULE, null);

	public static URI editionUri(String moduleId, Object effectiveTime) {
		return effectiveTime == null
				? URI.create(EDITION_URI_TEMPLATE.formatted(moduleId))
				: URI.create(VERSIONED_EDITION_URI_TEMPLATE.formatted(moduleId, effectiveTime));
	}

	public static ModuleMetadata asModuleMetadata(URI uri) throws ModuleStorageCoordinatorException {
		if (uri == null) {
			throw new ModuleStorageCoordinatorException.InvalidArgumentsException("URI must not be null.");
		}
		// Accepted forms:
		//   /sct/<moduleId>
		//   /sct/<moduleId>/version/<effectiveTime>
		String[] uriSegments = uri.getPath().split("/");
		if (uriSegments.length < 3 || !"sct".equals(uriSegments[1])) {
			throw new ModuleStorageCoordinatorException.InvalidArgumentsException("Unrecognised URI format: " + uri);
		}
		String moduleId = uriSegments[2];
		String effectiveTime = (uriSegments.length >= 5 && "version".equals(uriSegments[3])) ? uriSegments[4] : null;
		return new ModuleMetadata()
				.withIdentifyingModuleId(moduleId)
				.withEffectiveTime(effectiveTime);
	}

	public static String getBaseResourcePath(String directory, ModuleMetadata m) {
		return directory + SLASH + m.getCodeSystemShortName() + "_" + m.getIdentifyingModuleId() + SLASH + m.getEffectiveTimeString();
	}

	public static String convertArchivePathToMetadataPath(String rf2ArchiveResourcePath) {
		String[] splits = rf2ArchiveResourcePath.split(SLASH);
		splits = Arrays.copyOf(splits, splits.length - 1); // Remove last segment, i.e. a/b/c/d => a/b/c

		return String.join(SLASH, splits) + "/metadata.json";
	}

	public static List<URI> getURIsContained(ModuleMetadata metadata) {
		List<URI> uris = new ArrayList<>();
		uris.add(editionUri(metadata.getIdentifyingModuleId(), metadata.getEffectiveTime()));
		for (String moduleId : metadata.getCompositionModuleIds()) {
			uris.add(editionUri(moduleId, metadata.getEffectiveTime()));
		}
		return uris;
	}
}
