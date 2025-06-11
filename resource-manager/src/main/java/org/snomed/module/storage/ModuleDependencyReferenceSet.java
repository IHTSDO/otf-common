package org.snomed.module.storage;

import org.ihtsdo.otf.rest.client.terminologyserver.pojo.MdrsEntry;

import java.util.*;

public class ModuleDependencyReferenceSet {
	private Map<String, List<MdrsEntry>> moduleMap = new HashMap<>();
	private Map<String, MdrsEntry> idMap = new HashMap<>();
	private Map<String, List<String>> dependencyCache = new HashMap<>();

	public List<String> getDependencies(String moduleId) {
		if (dependencyCache.containsKey(moduleId)) {
			return dependencyCache.get(moduleId);
		}
		List<String> dependencies = moduleMap.getOrDefault(moduleId, Collections.emptyList())
				.stream()
				.map(MdrsEntry::getReferencedComponentId)
				.toList();
		dependencyCache.put(moduleId, dependencies);
		return dependencies;
	}

	public void addMdrsRow(MdrsEntry entry) {
		moduleMap.computeIfAbsent(entry.getModuleId(), k -> new ArrayList<>()).add(entry);
		idMap.put(entry.getId(), entry);

		//Also populate this entry by the module, into the moduleMap
		List<MdrsEntry> entries = moduleMap.computeIfAbsent(entry.getModuleId(), k -> new ArrayList<>());
		entries.remove(entry);
		entries.add(entry);
	}

	public MdrsEntry getMdrsRow(String id) {
		return idMap.get(id);
	}

}
