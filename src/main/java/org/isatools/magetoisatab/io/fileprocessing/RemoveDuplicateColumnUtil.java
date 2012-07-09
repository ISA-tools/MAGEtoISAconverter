package org.isatools.magetoisatab.io.fileprocessing;

import org.isatools.manipulator.SpreadsheetManipulation;

import java.util.*;

/**
 * Created by the ISA team
 *
 * @author Eamonn Maguire (eamonnmag@gmail.com)
 *         <p/>
 *         Date: 09/07/2012
 *         Time: 14:43
 */
public class RemoveDuplicateColumnUtil implements CleanupUtils {

    private Map<String, String[]> mergedColumnValues;

    public RemoveDuplicateColumnUtil() {
        this.mergedColumnValues = new HashMap<String, String[]>();
    }

    public List<String[]> processSpreadsheet(List<String[]> spreadsheet) {
        String[] columnNames = spreadsheet.get(0);

        Map<String, Set<Integer>> duplicateColumns = getDuplicateColumns(columnNames);
        createMergedDuplicateColumnRepresentation(spreadsheet, duplicateColumns);

        System.out.println("processSpreadsheet()..");

        // now we try remove duplicates and move their content to the same index in one column.
        int[] indicesToInclude = getIndicesToIncludeAndMergeValuesInDuplicateColumns(columnNames, spreadsheet, duplicateColumns);

        return SpreadsheetManipulation.getColumnSubset(spreadsheet, true, indicesToInclude);
    }

    private void createMergedDuplicateColumnRepresentation(List<String[]> spreadsheet, Map<String, Set<Integer>> duplicateColumns) {
        for (String columnName : duplicateColumns.keySet()) {
            // -1 because the 0th row is the column headers
            String[] values = new String[spreadsheet.size()];

            boolean overlapping = false;
            for (int rowIndex = 0; rowIndex < spreadsheet.size() && !overlapping; rowIndex++) {

                for (int columnIndex : duplicateColumns.get(columnName)) {

                    if(values[rowIndex] == null) {
                        values[rowIndex] = spreadsheet.get(rowIndex)[columnIndex];
                    }
                }
            }
            mergedColumnValues.put(columnName, values);
        }
    }

    private int[] getIndicesToIncludeAndMergeValuesInDuplicateColumns(String[] columnNames, List<String[]> spreadsheet, Map<String, Set<Integer>> columnNamesToIndexes) {

        List<Integer> indicesToKeep = new ArrayList<Integer>();

        Set<String> visitedColumns = new HashSet<String>();
        for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {
            String columnName = columnNames[columnIndex];

            if (!columnNamesToIndexes.containsKey(columnName)) {
                indicesToKeep.add(columnIndex);
            } else {
                // we should only add one of the indexes
                if (!visitedColumns.contains(columnName)) {
                    visitedColumns.add(columnName);
                    // add the first value in the set.
                    int indexToKeep = columnNamesToIndexes.get(columnName).iterator().next();

                    // merge all values for duplicate columns in to the column represented by 'indexToKeep' in to
                    spreadsheet = mergeDuplicates(spreadsheet, columnName, indexToKeep);
                    indicesToKeep.add(indexToKeep);
                }
            }
        }

        int[] indices = new int[indicesToKeep.size()];
        int count = 0;
        for (Integer index : indicesToKeep) {
            indices[count] = index;
            count++;
        }

        return indices;
    }

    private List<String[]> mergeDuplicates(List<String[]> spreadsheet, String columnName, int indexToKeep) {

        for (int rowIndex = 1; rowIndex < spreadsheet.size(); rowIndex++) {
            String newValue = mergedColumnValues.get(columnName)[rowIndex];
            spreadsheet.get(rowIndex)[indexToKeep] = newValue == null ? "" : newValue;
        }

        return spreadsheet;
    }


    private Map<String, Set<Integer>> getDuplicateColumns(String[] columnNames) {
        Map<String, Set<Integer>> columnNamesToIndexes = getColumnNameAndIndexes(columnNames);
        Set<String> toRemove = new HashSet<String>();

        for (String columnName : columnNamesToIndexes.keySet()) {
            if (columnNamesToIndexes.get(columnName).size() > 1) {
                if (!isColumnNameOk(columnName)) {
                    toRemove.add(columnName);
                }
            } else {
                toRemove.add(columnName);
            }
        }

        for (String columnToRemove : toRemove) {
            columnNamesToIndexes.remove(columnToRemove);
        }
        return columnNamesToIndexes;
    }

    private boolean isColumnNameOk(String columnName) {
        return (columnName.contains("Characteristics") || columnName.contains("Factor") || columnName.contains("Comment"));
    }

    private Map<String, Set<Integer>> getColumnNameAndIndexes(String[] columnNames) {

        Map<String, Set<Integer>> columnNamesToIndexes = new HashMap<String, Set<Integer>>();

        int count = 0;
        for (String columnName : columnNames) {
            if (!columnNamesToIndexes.containsKey(columnName)) {
                columnNamesToIndexes.put(columnName, new HashSet<Integer>());
            }
            columnNamesToIndexes.get(columnName).add(count);
            count++;
        }

        return columnNamesToIndexes;
    }
}
