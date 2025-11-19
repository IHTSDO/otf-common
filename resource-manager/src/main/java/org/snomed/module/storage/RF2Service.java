package org.snomed.module.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RF2Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(RF2Service.class);

    public static final Integer EFFECTIVE_TIME = 1;
    public static final Integer ACTIVE = 2;
    public static final Integer MODULE_ID = 3;
    public static final Integer REFERENCED_COMPONENT_ID = 5;
    public static final Integer SOURCE_EFFECTIVE_TIME = 6;
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

    public Set<RF2Row> getUniqueModulesWithLatestEffectiveTime(File file, boolean rf2DeltaOnly) {
        if (file == null) {
            return Collections.emptySet();
        }

        LOGGER.debug("Getting unique modules with latest effective time from: {}", file.getName());
        List<List<String>> rows = getRows(file, rf2DeltaOnly, null, EFFECTIVE_TIME, MODULE_ID);
        Set<RF2Row> rf2Rows = new HashSet<>();
        for (List<String> row : rows) {
            String effectiveTime = row.get(0);
            String moduleId = row.get(1);
            boolean shouldAdd = false;
            boolean isModuleFound = rf2Rows.stream().anyMatch(item -> moduleId.equals(item.getColumn(MODULE_ID)));
            if (isModuleFound) {
                boolean isModuleWithLatestEffectiveTime = rf2Rows.stream().anyMatch(item -> moduleId.equals(item.getColumn(MODULE_ID)) && Integer.parseInt(item.getColumn(EFFECTIVE_TIME)) > Integer.parseInt(effectiveTime));
                if (!isModuleWithLatestEffectiveTime) {
                    rf2Rows = rf2Rows.stream().filter(item -> !moduleId.equals(item.getColumn(MODULE_ID))).collect(Collectors.toSet());
                    shouldAdd = true;
                }
            } else {
                shouldAdd = true;
            }
            if (shouldAdd) {
                rf2Rows.add(new RF2Row().addRow(EFFECTIVE_TIME, row.get(0)).addRow(MODULE_ID, row.get(1)));
            }
        }

        return rf2Rows;
    }

    public Set<RF2Row> getMDRS(File file, boolean rf2DeltaOnly) {
        if (file == null) {
            return Collections.emptySet();
        }

        List<List<String>> rows = getRows(file, rf2DeltaOnly, "ModuleDependency", ACTIVE, MODULE_ID, REFERENCED_COMPONENT_ID, SOURCE_EFFECTIVE_TIME, TARGET_EFFECTIVE_TIME);
        Set<RF2Row> rf2Rows = new HashSet<>();
        for (List<String> row : rows) {
            String a = row.get(0);
            boolean inactive = Objects.equals(a, "0");
            if (inactive) {
                continue;
            }

            rf2Rows.add(new RF2Row().addRow(MODULE_ID, row.get(1)).addRow(REFERENCED_COMPONENT_ID, row.get(2)).addRow(SOURCE_EFFECTIVE_TIME, row.get(3)).addRow(TARGET_EFFECTIVE_TIME, row.get(4)));
        }

        return rf2Rows;
    }

    public Set<RF2Row> getMDRS(InputStream inputStream, boolean rf2DeltaOnly) {
        if (inputStream == null) {
            return Collections.emptySet();
        }

        List<List<String>> rows = getRows(inputStream, rf2DeltaOnly, "ModuleDependency", ACTIVE, MODULE_ID, REFERENCED_COMPONENT_ID, SOURCE_EFFECTIVE_TIME, TARGET_EFFECTIVE_TIME);
        Set<RF2Row> rf2Rows = new HashSet<>();
        for (List<String> row : rows) {
            String a = row.get(0);
            boolean inactive = Objects.equals(a, "0");
            if (inactive) {
                continue;
            }

            rf2Rows.add(new RF2Row().addRow(MODULE_ID, row.get(1)).addRow(REFERENCED_COMPONENT_ID, row.get(2)).addRow(SOURCE_EFFECTIVE_TIME, row.get(3)).addRow(TARGET_EFFECTIVE_TIME, row.get(4)));
        }

        return rf2Rows;
    }

    public Set<RF2Row> setTransientSourceEffectiveTimes(Set<RF2Row> mdrsRows, Set<String> transientSourceEffectiveTimes) {
        Set<RF2Row> dup = new HashSet<>();

        for (RF2Row mdrsRow : mdrsRows) {
            String sourceEffectiveTime = mdrsRow.getColumn(RF2Service.SOURCE_EFFECTIVE_TIME);
            if (sourceEffectiveTime != null && sourceEffectiveTime.isEmpty()) {
                for (String transientSourceEffectiveTime : transientSourceEffectiveTimes) {
                    RF2Row copy = new RF2Row(mdrsRow);
                    copy.addRow(RF2Service.SOURCE_EFFECTIVE_TIME, transientSourceEffectiveTime);

                    dup.add(copy);
                }
            } else {
                dup.add(mdrsRow);
            }
        }

        return dup;
    }

    @SuppressWarnings("java:S5042")
    private List<List<String>> getRows(File file, boolean rf2DeltaOnly, String fileNamePattern, Integer... columnIndices) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return getRows(fileInputStream, rf2DeltaOnly, fileNamePattern, columnIndices);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("java:S5042")
    private List<List<String>> getRows(InputStream inputStream, boolean rf2DeltaOnly, String fileNamePattern, Integer... columnIndices) {
        List<List<String>> rows = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String[] pathSegments = entry.getName().split("/");
                boolean skip = skip(rf2DeltaOnly, pathSegments, fileNamePattern);
                if (skip) {
                    continue;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                List<List<String>> chunkRows = reader.lines()
                        .parallel()
                        .filter(line -> !line.startsWith("id") && !line.startsWith("alternateIdentifier"))
                        .map(line -> Arrays.asList(line.split("\t", -1))) // Keep empty columns
                        .map(columns -> Arrays.stream(columnIndices)
                                .map(columns::get)
                                .toList())
                        .toList();

                rows.addAll(chunkRows);

                zis.closeEntry();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read from ZIP input stream", e);
            return Collections.emptyList();
        }

        return rows;
    }

    private boolean skip(boolean rf2DeltaOnly, String[] resourcePathSegments, String fileNamePattern) {
        boolean snapshot = resourcePathSegments.length >= 2 ? "Snapshot".equals(resourcePathSegments[1]) : resourcePathSegments[0].contains("Snapshot");
        if (!rf2DeltaOnly && !snapshot) {
            return true;
        }

        String fileName = resourcePathSegments[resourcePathSegments.length - 1];
        boolean rf2TextFile = fileName.startsWith("der2_") || fileName.startsWith("sct2_") || fileName.startsWith("xder2_") || fileName.startsWith("xsct2_") || fileName.startsWith("rel2_");
        if (!rf2TextFile) {
            return true;
        }

        if (fileNamePattern != null && !fileNamePattern.isBlank()) {
            boolean fileMatch = resourcePathSegments[resourcePathSegments.length - 1].contains(fileNamePattern);
            if (!fileMatch) {
                return true;
            }
        }

        return false;
    }
}