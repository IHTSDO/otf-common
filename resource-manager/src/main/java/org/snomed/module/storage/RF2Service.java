package org.snomed.module.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RF2Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(RF2Service.class);

    public static final Integer MODULE_ID = 3;
    public static final Integer REFERENCED_COMPONENT_ID = 5;
    public static final Integer TARGET_EFFECTIVE_TIME = 7;

    public Set<String> getUniqueModuleIds(File file, boolean rf2DeltaOnly) {
        if (file == null) {
            return Collections.emptySet();
        }

        LOGGER.debug("Getting unique module IDs from: {}", file.getName());
        List<List<String>> rows = getRows(file, rf2DeltaOnly, null, MODULE_ID);
        Set<String> result = new HashSet<>();
        for (List<String> row : rows) {
            result.addAll(row);
        }
        LOGGER.debug("Collected {} unique module IDs from: {}", rows.size(), file.getName());
        return result;
    }

    public Set<RF2Row> getMDRS(File file, boolean rf2DeltaOnly) {
        if (file == null) {
            return Collections.emptySet();
        }

        List<List<String>> rows = getRows(file, rf2DeltaOnly, "ModuleDependency", REFERENCED_COMPONENT_ID, TARGET_EFFECTIVE_TIME);
        Set<RF2Row> rf2Rows = new HashSet<>();
        for (List<String> row : rows) {
            rf2Rows.add(
                    new RF2Row()
                            .addRow(REFERENCED_COMPONENT_ID, row.get(0))
                            .addRow(TARGET_EFFECTIVE_TIME, row.get(1))
            );
        }

        return rf2Rows;
    }

    private List<List<String>> getRows(File file, boolean rf2DeltaOnly, String fileNamePattern, Integer... columnIndices) {
        List<List<String>> rows = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String[] resourcePathSegments = entry.getName().split("/");

                if (resourcePathSegments.length >= 2) {
                    boolean snapshot = "Snapshot".equals(resourcePathSegments[1]);
                    if (!rf2DeltaOnly && !snapshot) {
                        continue;
                    }

                    String fileName = resourcePathSegments[resourcePathSegments.length - 1];
                    boolean rf2TextFile = fileName.startsWith("der2_") || fileName.startsWith("sct2_") || fileName.startsWith("xder2_") || fileName.startsWith("xsct2_") || fileName.startsWith("rel2_");
                    if (!rf2TextFile) {
                        continue;
                    }

                    if (fileNamePattern != null && !fileNamePattern.isBlank()) {
                        boolean fileMatch = resourcePathSegments[resourcePathSegments.length - 1].contains(fileNamePattern);
                        if (!fileMatch) {
                            continue;
                        }
                    }

                    try (InputStream inputStream = zipFile.getInputStream(entry);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(inputStream), StandardCharsets.UTF_8))) {
                        List<List<String>> chunkRows = reader.lines()
                                .parallel()
                                .filter(line -> !line.startsWith("id") && !line.startsWith("alternateIdentifier"))
                                .map(line -> Arrays.asList(line.split("\t")))
                                .map(columns -> Arrays.stream(columnIndices)
                                        .map(columns::get)
                                        .collect(Collectors.toList()))
                                .toList();

                        rows.addAll(chunkRows);
                    }
                }
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }

        return rows;
    }
}