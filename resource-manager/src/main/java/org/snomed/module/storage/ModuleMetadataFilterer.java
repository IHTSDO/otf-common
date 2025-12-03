package org.snomed.module.storage;

import org.apache.jena.atlas.lib.Pair;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Utility to help with ModuleMetadata filtering.
 */
public class ModuleMetadataFilterer {
	private static final Map<String, BiPredicate<ModuleMetadata, RF2Row>> PREDICATES = Map.of(
			"1", ModuleMetadataFilterer::identifyingModuleIdEqualsReferencedComponentIdOrModuleId,
			"2", ModuleMetadataFilterer::identifyingModuleIdEqualsReferencedComponentIdAndEffectiveTimeEqualsTargetEffectiveTime,
			"3", ModuleMetadataFilterer::filterByModuleIdAndSourceEffectiveTimeOrReferencedComponentIdAndTargetEffectiveTime,
			"4", ModuleMetadataFilterer::compositionModulesContainReferencedComponentIdAndEffectiveTimeEqualsTargetEffectiveTime
	);

	private static final RF2Service RF2_SERVICE = new RF2Service();

	private ModuleMetadataFilterer() {

	}

	/**
	 * Return a filtered collection of ModuleMetadata. Only entries whose identifyingModuleId matches either referencedComponentId or moduleId
	 * will be returned.
	 *
	 * @param rf2Packages Collection of ModuleMetadata to filter.
	 * @param mdrs        Collection of RF2Row to help with filtering.
	 * @return Filtered collection of ModuleMetadata.
	 */
	public static Set<ModuleMetadata> filterByReferencedComponentIdOrModuleId(Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrs) {
		return filter(rf2Packages, mdrs, PREDICATES.get("1"));
	}

	/**
	 * Return a filtered collection of ModuleMetadata. Only entries whose identifyingModuleId matches the referencedComponentId and
	 * effectiveTime matching the targetEffectiveTime will be returned.
	 *
	 * @param rf2Packages Collection of ModuleMetadata to filter.
	 * @param mdrs        Collection of RF2Row to help with filtering.
	 * @return Filtered collection of ModuleMetadata.
	 */
	public static Set<ModuleMetadata> filterByReferencedComponentIdAndTargetEffectiveTime(Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrs) {
		return filter(rf2Packages, mdrs, PREDICATES.get("2"));
	}

	/**
	 * Return a filtered collection of ModuleMetadata. Only entries whose identifyingModuleId and effectiveTime matching the moduleId
	 * and sourceEffectiveTime or identifyingModuleId and effectiveTime matching the referencedComponentId and targetEffectiveTime
	 * will be returned.
	 *
	 * @param rf2Packages Collection of ModuleMetadata to filter.
	 * @param mdrs        Collection of RF2Row to help with filtering.
	 *                       @param transientSourceEffectiveTimes        Collection of RF2Row to help with filtering.
	 * @return Filtered collection of ModuleMetadata.
	 */
	public static Set<ModuleMetadata> filterByModuleIdAndSourceEffectiveTimeOrReferencedComponentIdAndTargetEffectiveTime(Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrs, Set<String> transientSourceEffectiveTimes) {
		Set<ModuleMetadata> filter = filter(rf2Packages, mdrs, PREDICATES.get("3"));

		// Try again if an entry has had no hits
		if (transientSourceEffectiveTimes != null && !transientSourceEffectiveTimes.isEmpty()) {
			filter = tryAgain(filter, rf2Packages, mdrs, transientSourceEffectiveTimes);
		}

		return filter;
	}

	/**
	 * Return a filtered collection of ModuleMetadata.Only entries whose compositionModuleIds contain the referencedComponentId and
	 * effectiveTime matching the targetEffectiveTime will be returned.
	 *
	 * @param rf2Packages Collection of ModuleMetadata to filter.
	 * @param mdrs        Collection of RF2Row to help with filtering.
	 * @return Filtered collection of ModuleMetadata.
	 */
	public static Set<ModuleMetadata> filterByCompositionModulesContainReferencedComponentIdAndTargetEffectiveTime(Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrs) {
		return filter(rf2Packages, mdrs, PREDICATES.get("4"));
	}

	/**
	 * Return a filtered collection of ModuleMetadata. Only entries whose identifyingModuleId matches the referencedComponentId and targetEffectiveTime matching the effectiveTime
	 * will be returned.
	 *
	 * @param rf2Packages Collection of ModuleMetadata to filter.
	 * @param mdrs        Collection of RF2Row to help with filtering.
	 * @return Filtered collection of ModuleMetadata.
	 */
	public static Set<ModuleMetadata> keepReferencedComponentIdMatchingIdentifyingModuleIdAndTargetEffectiveTimeMatchingEffectiveTime(Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrs) {
		if (rf2Packages == null || rf2Packages.isEmpty() || mdrs == null || mdrs.isEmpty()) {
			return Collections.emptySet();
		}

		Set<Pair<String, String>> referencedComponentIdTargetEffectiveTimes = mdrs.stream().map(entry -> {
			String referencedComponentId = entry.getColumn(RF2Service.REFERENCED_COMPONENT_ID);
			String targetEffectiveTime = entry.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);

			return Pair.create(referencedComponentId, targetEffectiveTime);
		}).collect(Collectors.toSet());

		return rf2Packages.stream().filter(rf2Package -> {
			Pair<String, String> identifyingModuleIdEffectiveTime = Pair.create(rf2Package.getIdentifyingModuleId(), rf2Package.getEffectiveTimeString());
			return referencedComponentIdTargetEffectiveTimes.contains(identifyingModuleIdEffectiveTime);
		}).collect(Collectors.toSet());
	}

	private static Set<ModuleMetadata> filter(Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrs, BiPredicate<ModuleMetadata, RF2Row> predicate) {
		if (rf2Packages == null || rf2Packages.isEmpty() || mdrs == null || mdrs.isEmpty()) {
			return Collections.emptySet();
		}

		Set<ModuleMetadata> filtered = new HashSet<>();
		for (ModuleMetadata rf2Package : rf2Packages) {
			for (RF2Row row : mdrs) {
				if (predicate.test(rf2Package, row)) {
					filtered.add(rf2Package);
				}
			}
		}

		return filtered;
	}

	private static boolean identifyingModuleIdEqualsReferencedComponentIdOrModuleId(ModuleMetadata rf2Package, RF2Row row) {
		String identifyingModuleId = rf2Package.getIdentifyingModuleId();
		String moduleId = row.getColumn(RF2Service.MODULE_ID);
		String referencedComponentId = row.getColumn(RF2Service.REFERENCED_COMPONENT_ID);

		return Objects.equals(identifyingModuleId, referencedComponentId) || Objects.equals(identifyingModuleId, moduleId);
	}

	private static boolean identifyingModuleIdEqualsReferencedComponentIdAndEffectiveTimeEqualsTargetEffectiveTime(ModuleMetadata rf2Package, RF2Row row) {
		String identifyingModuleId = rf2Package.getIdentifyingModuleId();
		String effectiveTimeString = rf2Package.getEffectiveTimeString();
		String referencedComponentId = row.getColumn(RF2Service.REFERENCED_COMPONENT_ID);
		String targetEffectiveTime = row.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);
		List<String> compositionModuleIds = rf2Package.getCompositionModuleIds();

		boolean matchReferencedComponentId = Objects.equals(identifyingModuleId, referencedComponentId) || compositionModuleIds.contains(referencedComponentId);
		boolean matchTargetEffectiveTime = targetEffectiveTime.isEmpty() || Objects.equals(effectiveTimeString, targetEffectiveTime);

		return matchReferencedComponentId && matchTargetEffectiveTime;
	}

	private static boolean filterByModuleIdAndSourceEffectiveTimeOrReferencedComponentIdAndTargetEffectiveTime(ModuleMetadata rf2Package, RF2Row row) {
		String identifyingModuleId = rf2Package.getIdentifyingModuleId();
		String effectiveTimeString = rf2Package.getEffectiveTimeString();

		String moduleId = row.getColumn(RF2Service.MODULE_ID);
		String referencedComponentId = row.getColumn(RF2Service.REFERENCED_COMPONENT_ID);
		String sourceEffectiveTime = row.getColumn(RF2Service.SOURCE_EFFECTIVE_TIME);
		String targetEffectiveTime = row.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);

		boolean matchModuleId = Objects.equals(identifyingModuleId, moduleId);
		boolean matchSourceEffectiveTime = sourceEffectiveTime.isEmpty() || Objects.equals(effectiveTimeString, sourceEffectiveTime);
		boolean matchReferencedComponentId = Objects.equals(identifyingModuleId, referencedComponentId);
		boolean matchTargetEffectiveTime = targetEffectiveTime.isEmpty() || Objects.equals(effectiveTimeString, targetEffectiveTime);

		return (matchModuleId && matchSourceEffectiveTime) || (matchReferencedComponentId && matchTargetEffectiveTime);
	}

	private static boolean compositionModulesContainReferencedComponentIdAndEffectiveTimeEqualsTargetEffectiveTime(ModuleMetadata rf2Package, RF2Row row) {
		String effectiveTimeString = rf2Package.getEffectiveTimeString();
		String referencedComponentId = row.getColumn(RF2Service.REFERENCED_COMPONENT_ID);
		String targetEffectiveTime = row.getColumn(RF2Service.TARGET_EFFECTIVE_TIME);
		List<String> compositionModuleIds = rf2Package.getCompositionModuleIds();

		boolean matchReferencedComponentId = compositionModuleIds.contains(referencedComponentId);
		boolean matchTargetEffectiveTime = targetEffectiveTime.isEmpty() || Objects.equals(effectiveTimeString, targetEffectiveTime);

		return matchReferencedComponentId && matchTargetEffectiveTime;
	}

	private static Set<ModuleMetadata> tryAgain(Set<ModuleMetadata> filter, Set<ModuleMetadata> rf2Packages, Set<RF2Row> mdrs, Set<String> transientSourceEffectiveTimes) {
		for (RF2Row entry : mdrs) {
			String moduleId = entry.getColumn(RF2Service.MODULE_ID);
			String sourceEffectiveTime = entry.getColumn(RF2Service.SOURCE_EFFECTIVE_TIME);
			boolean found = false;

			for (ModuleMetadata moduleMetadata : filter) {
				boolean matchModuleId = moduleMetadata.getIdentifyingModuleId().equals(moduleId);
				boolean matchSourceEffectiveTime = moduleMetadata.getEffectiveTimeString().equals(sourceEffectiveTime);
				if (matchModuleId && matchSourceEffectiveTime) {
					found = true;
				}
			}

			// Prepare entry for transient replacement
			if (!found) {
				entry.addRow(RF2Service.SOURCE_EFFECTIVE_TIME, "");
			}
		}

		mdrs = RF2_SERVICE.setTransientSourceEffectiveTimes(mdrs, transientSourceEffectiveTimes);
		return filter(rf2Packages, mdrs, PREDICATES.get("3"));
	}
}
