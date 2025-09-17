package org.snomed.module.storage;


import java.util.HashMap;
import java.util.Map;

public class RF2Row {
    private final Map<Integer, String> columns = new HashMap<>();
    private boolean found;
    private String metadataResourcePath;

    public RF2Row() {

    }

    public RF2Row(RF2Row rf2Row) {
        this.columns.putAll(rf2Row.getColumns());
    }

    public Map<Integer, String> getColumns() {
        return columns;
    }

    public RF2Row addRow(Integer columnIndex, String columnData) {
        columns.put(columnIndex, columnData);
        return this;
    }

    public String getColumn(Integer index) {
        return columns.get(index);
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public String getMetadataResourcePath() {
        return metadataResourcePath;
    }

    public void setMetadataResourcePath(String metadataResourcePath) {
        this.metadataResourcePath = metadataResourcePath;
    }
}
