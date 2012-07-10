package org.isatools.magetoisatab.io.fileprocessing;

import org.isatools.manipulator.SpreadsheetManipulation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Will remove any rogue term source ref columns laying around in the sheet which would otherwise cause the
 * validator to fail.
 */
public class RogueColumnRemovalUtil extends CleanupUtils {

    static Set<String> columnsToLookFor;

    static {
        columnsToLookFor = new HashSet<String>();

        columnsToLookFor.add("Term Source REF");
        columnsToLookFor.add("Term Accession Number");
    }

    public List<String[]> processSpreadsheet(List<String[]> spreadsheet) {
        try {
            String[] columnHeaders = spreadsheet.get(0);
            int[] indicesToKeep = locateAndRemoveRogueColumns(columnHeaders);

            System.out.println("Column header size is:" + columnHeaders.length);
            System.out.println("Indices to keep is: " + indicesToKeep.length);
            if(indicesToKeep.length == columnHeaders.length) {
                return spreadsheet;
            }
            return SpreadsheetManipulation.getColumnSubset(spreadsheet, true, indicesToKeep);
        } catch (Exception e) {
            e.printStackTrace();
            return spreadsheet;
        }
    }

    public int[] locateAndRemoveRogueColumns(String[] columnHeaders) {
        List<Integer> indexesToKeep = new ArrayList<Integer>();

        for (int columnIndex = 0; columnIndex < columnHeaders.length; columnIndex++) {
            if (columnIndex != 0) {
                // check if last column is one which should have term source and accession
                if (columnsToLookFor.contains(columnHeaders[columnIndex])) {
                    // we want to check for column 1 or two back in the case of the source ref and accession columns.
                    if (isColumnNameOk(columnHeaders[columnIndex - 1])) {
                        indexesToKeep.add(columnIndex);
                    } else if (columnIndex - 2 > 0) {
                        if (isColumnNameOk(columnHeaders[columnIndex - 2])) {
                            indexesToKeep.add(columnIndex);
                        }
                    } else {
                        System.out.println(columnIndex + " should be removed!");
                    }
                } else {
                    indexesToKeep.add(columnIndex);
                }
            } else {
                indexesToKeep.add(columnIndex);
            }
        }

        return convertListOfClassesToArrayOfPrimitives(indexesToKeep);
    }
}
