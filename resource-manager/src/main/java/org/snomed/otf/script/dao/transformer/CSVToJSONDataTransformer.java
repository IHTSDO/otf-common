package org.snomed.otf.script.dao.transformer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

import org.ihtsdo.otf.utils.StringUtils;

public class CSVToJSONDataTransformer implements DataTransformer {

    public static final String FILE_EXTENSION = ".json";

    // Jackson 3 mappers are immutable and thread safe, so this is built once and shared.
    private static final ObjectMapper JSON_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    protected CsvMapper csvMapper;

    protected boolean excludeLastRow = false;

    public CSVToJSONDataTransformer() {
        csvMapper = CsvMapper.builder()
                .enable(CsvReadFeature.WRAP_AS_ARRAY)
                .build();
    }

    public CSVToJSONDataTransformer(boolean excludeLastRow) {
        this();
        this.excludeLastRow = excludeLastRow;
    }

    @Override
    public void transform(File input, File output) throws Exception {
        // The resources are declared in this order so that the generator flushes and closes before
        // the writer beneath it, and the CSV reader is always closed. The previous version closed
        // the writer first and then closed the generator from a finally block, where it failed with
        // "Stream closed" and discarded whatever exception had actually caused the failure.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output, false));
             JsonGenerator jsonGenerator = JSON_MAPPER.createGenerator(writer);
             MappingIterator<String[]> rows = csvMapper.readerFor(String[].class).readValues(input)) {

            jsonGenerator.writeStartArray();

            String[] headings = null;
            // Each row is held back until the next one is read, so excludeLastRow can drop the
            // final row of data without peeking at the iterator - a peek that reported the wrong
            // row whenever the file ended with blank lines.
            String[] heldRow = null;
            int heldRowLine = 0;
            int line = 0;

            while (rows.hasNextValue()) {
                line++;
                String[] row = cleanRow(rows.nextValue());

                // Simply ignore empty rows.
                if (isBlank(row)) {
                    continue;
                }

                if (headings == null) {
                    // The headings (simply store them for use later)
                    headings = row;
                } else {
                    if (heldRow != null) {
                        writeRow(jsonGenerator, headings, heldRow, heldRowLine);
                    }
                    heldRow = row;
                    heldRowLine = line;
                }
            }

            if (heldRow != null && !excludeLastRow) {
                writeRow(jsonGenerator, headings, heldRow, heldRowLine);
            }

            jsonGenerator.writeEndArray();
        }
    }

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION;
    }

    private void writeRow(JsonGenerator jsonGenerator, String[] headings, String[] row, int line) {
        // Guarded explicitly: indexing headings past its length threw an ArrayIndexOutOfBounds that
        // said nothing about which line of which file was malformed.
        if (row.length > headings.length) {
            throw new IllegalStateException("CSV line " + line + " has " + row.length
                    + " values but the header row only names " + headings.length
                    + ", so the surplus values cannot be mapped to a property.");
        }
        jsonGenerator.writeStartObject();
        for (int index = 0; index < row.length; index++) {
            jsonGenerator.writeStringProperty(headings[index], row[index]);
        }
        jsonGenerator.writeEndObject();
    }

    /**
     * A row holding nothing but blank values carries no data. The previous {@code row == null}
     * check never fired, because {@link #cleanRow(String[])} returns an array for every non-null
     * input, so a blank line was emitted as an object with one empty property.
     */
    private static boolean isBlank(String[] row) {
        return StringUtils.isEmpty(row) || Arrays.stream(row).allMatch(StringUtils::isEmpty);
    }

    private String[] cleanRow(String[] row) {
        if (row != null) {
            return Arrays.stream(row).map(aRow -> {
                if (!StringUtils.isEmpty(aRow)) {
                    aRow = aRow.trim();
                }
                return aRow;
            }).toArray(String[]::new);
        }
        return null;
    }
}
