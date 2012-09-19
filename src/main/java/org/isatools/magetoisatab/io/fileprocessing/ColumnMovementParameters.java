package org.isatools.magetoisatab.io.fileprocessing;

public enum ColumnMovementParameters {
    
    ASSAY_NAME("Assay Name", "Raw Data File", true),
    SEQUENCING_RUN("Parameter Value[run identifier]", "Parameter Value[sequencing instrument]", true) ,
    SEQUENCING_INSTRUMENT("Parameter Value[sequencing instrument]", "Assay Name", true)

    ;

    private String columnName;
    private String columnNameToMoveBeside;
    private boolean insertBefore;

    private ColumnMovementParameters(String columnName, String columnNameToMoveBeside, boolean insertBefore) {
        this.columnName = columnName;
        this.columnNameToMoveBeside = columnNameToMoveBeside;
        this.insertBefore = insertBefore;
    }


    public String getColumnName() {
        return columnName;
    }

    public String getColumnNameToMoveBeside() {
        return columnNameToMoveBeside;
    }

    public boolean isInsertBefore() {
        return insertBefore;
    }
}
