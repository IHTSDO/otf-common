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
import java.io.IOException;
import java.util.Arrays;

import org.ihtsdo.otf.utils.StringUtils;

public class CSVToJSONDataTransformer implements DataTransformer {

    public static final String FILE_EXTENSION = ".json";

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
        JsonGenerator jsonGenerator = null;
        try (BufferedWriter outputStream  = new BufferedWriter(new FileWriter(output, false))) {
            // create the Json mapper
            ObjectMapper mapper = JsonMapper.builder()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .build();
            jsonGenerator = mapper.createGenerator(outputStream);

            // start of the report array
            jsonGenerator.writeStartArray();

            // process all rows
            MappingIterator<String[]> mappingIterator = csvMapper.readerFor(String[].class).readValues(input);
            int rows = 1;
            String[] headings = null;
            while (mappingIterator.hasNextValue()) {
                String[] rowOLD = mappingIterator.nextValue();
                String[] row = cleanRow(rowOLD); // clean row

                // simply ignore empty rows
                if (row == null) {
                    continue;
                }
                // The headings (simply store them for use later)
                if (rows == 1) {
                    headings = row;
                    rows++;
                    continue;
                }

                boolean excludeRow = !mappingIterator.hasNextValue() && excludeLastRow;
                if (!excludeRow) {
                    jsonGenerator.writeStartObject();
                    for (int index = 0; index < row.length; index++) {
                        jsonGenerator.writeStringProperty(headings[index], row[index]);
                    }
                    jsonGenerator.writeEndObject();
                }
            }
            // end of the report array
            jsonGenerator.writeEndArray();
            jsonGenerator.close();
        } catch (IOException e) {
            throw e;
        } finally {
            // close the generator if we need to
            if ( jsonGenerator != null && !jsonGenerator.isClosed()) {
                jsonGenerator.close();
            }
        }
    }

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION;
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
