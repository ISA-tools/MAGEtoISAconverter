package org.isatools.magetoisatab.io.fileprocessing;

import org.isatools.manipulator.SpreadsheetManipulation;

import java.util.ArrayList;
import java.util.List;

public class ColumnMoveUtil extends CleanupUtils {

    @Override
    public List<String[]> processSpreadsheet(List<String[]> spreadsheet) {
        // look for certain columns, namely Parameter Value[run identifier]
        for (ColumnMovementParameters movementParameters : ColumnMovementParameters.values()) {
            int toMoveIndex = -1;
            int indexToMoveTo = -1;
            String[] columnHeaders = SpreadsheetManipulation.getColumnHeaders(spreadsheet);
            int columnIndex = 0;
            for (String columnHeader : columnHeaders) {
                if (columnHeader.equals(movementParameters.getColumnName())) {
                    toMoveIndex = columnIndex;
                }
                if (columnHeader.equals(movementParameters.getColumnNameToMoveBeside())) {
                    indexToMoveTo = columnIndex;
                    break;
                }
                columnIndex++;
            }

            if (toMoveIndex != -1 && indexToMoveTo != -1) {
                spreadsheet = SpreadsheetManipulation.moveColumn(spreadsheet, toMoveIndex, movementParameters.isInsertBefore()
                        ? indexToMoveTo == 0
                        ? 0
                        : indexToMoveTo - 1
                        : indexToMoveTo);
            }

        }

        return spreadsheet;
    }

    public static void main(String[] args) {
        List<String[]> spreadsheet = new ArrayList<String[]>();

        spreadsheet.add(new String[]{"Sample Name", "Extract Name", "Parameter Value[sequencing instrument]", "Parameter Value[library selection]", "Parameter Value[library_source]",
                "Parameter Value[library_strategy]", "Parameter Value[library layout]", "Comment [Platform_title]", "Labeled Extract Name", "Comment [ENA_EXPERIMENT]", "Protocol REF", "Protocol REF", "Assay Name",
                "Parameter Value[run identifier]", "Raw Data File", "Derived Data File", "Comment [Derived ArrayExpress FTP file]", "Factor Value[barcode (first 3 nt for fastq files of chip-seq libraries, first 4 nt for fastq files of small rna libraries)]",
                "Term Source REF", "Term Accession Number", "Term Source REF", "Term Accession Number", "Factor Value[generation]", "Factor Value[rnai target: chri]",
                "Factor Value[strain]"});

        CleanupUtils protocol = new ProtocolInsertionUtil();
        spreadsheet = protocol.processSpreadsheet(spreadsheet);

        CleanupUtils move = new ColumnMoveUtil();
        spreadsheet = move.processSpreadsheet(spreadsheet);

        for (String[] row : spreadsheet) {
            for (String columnName : row) {
                System.out.print(columnName + "\t");
            }
            System.out.println();
        }

    }
}
