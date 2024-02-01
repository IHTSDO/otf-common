package org.snomed.module.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RF2Service {
    public static final Integer MODULE_ID = 3;
    public static final Integer REFERENCED_COMPONENT_ID = 5;
    public static final Integer TARGET_EFFECTIVE_TIME = 7;

    public Set<String> getUniqueModuleIds(File file) {
        if (file == null) {
            return Collections.emptySet();
        }

        List<List<String>> rows = getRows(file, null, MODULE_ID);
        Set<String> result = new HashSet<>();
        for (List<String> row : rows) {
            result.addAll(row);
        }

        return result;
    }

    public Set<RF2Row> getMDRS(File file) {
        if (file == null) {
            return Collections.emptySet();
        }

        List<List<String>> rows = getRows(file, "der2_ssRefset_ModuleDependency", REFERENCED_COMPONENT_ID, TARGET_EFFECTIVE_TIME);
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

    private List<List<String>> getRows(File file, String fileNamePattern, Integer... columnIndices) {
        List<List<String>> rows = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String[] resourcePathSegments = entry.getName().split("/");

                if (resourcePathSegments.length >= 2) {
                    boolean snapshot = "Snapshot".equals(resourcePathSegments[1]);
                    if (!snapshot) {
                        continue;
                    }

                    String fileName = resourcePathSegments[resourcePathSegments.length - 1];
                    boolean rf2TextFile = fileName.startsWith("der2_") || fileName.startsWith("sct2_") || fileName.startsWith("xder2_") || fileName.startsWith("xsct2_");
                    if (!rf2TextFile) {
                        continue;
                    }

                    if (fileNamePattern != null && !fileNamePattern.isBlank()) {
                        boolean fileMatch = resourcePathSegments[resourcePathSegments.length - 1].contains(fileNamePattern);
                        if (!fileMatch) {
                            continue;
                        }
                    }

                    InputStream inputStream = zipFile.getInputStream(entry);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] columns = line.split("\t");
                        boolean header = Objects.equals(columns[0], "id") || Objects.equals(columns[0], "alternateIdentifier");
                        if (!header) {
                            List<String> row = new ArrayList<>();
                            for (Integer columnIndex : columnIndices) {
                                row.add(columns[columnIndex]);
                            }

                            rows.add(row);
                        }
                    }
                }
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }

        return rows;
    }
}