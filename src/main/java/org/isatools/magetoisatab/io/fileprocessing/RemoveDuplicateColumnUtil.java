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
public class RemoveDuplicateColumnUtil extends CleanupUtils {

    private Map<String, String[]> mergedColumnValues;

    public RemoveDuplicateColumnUtil() {
        this.mergedColumnValues = new HashMap<String, String[]>();
    }

    public List<String[]> processSpreadsheet(List<String[]> spreadsheet) {
        String[] columnNames = spreadsheet.get(0);

        Map<String, Set<Integer>> duplicateColumns = getDuplicateColumns(columnNames);
        System.out.println("Number of duplicate columns: " + duplicateColumns.size());

        if (duplicateColumns.size() == 0) {
            return spreadsheet;

        }
        // otherwise continue on.
        createMergedDuplicateColumnRepresentation(spreadsheet, duplicateColumns);
        // now we try remove duplicates and move their content to the same index in one column.
        int[] indicesToInclude = getIndicesToIncludeAndMergeValuesInDuplicateColumns(columnNames, spreadsheet, duplicateColumns);

        return SpreadsheetManipulation.getColumnSubset(spreadsheet, true, indicesToInclude);
    }

    private void createMergedDuplicateColumnRepresentation(List<String[]> spreadsheet, Map<String, Set<Integer>> duplicateColumns) {
        for (String columnName : duplicateColumns.keySet()) {
            String[] newColumnValues = new String[spreadsheet.size()];

            boolean overlapping = false;
            for (int rowIndex = 0; rowIndex < spreadsheet.size() && !overlapping; rowIndex++) {

                for (int columnIndex : duplicateColumns.get(columnName)) {

                    if (newColumnValues[rowIndex] == null || newColumnValues[rowIndex].isEmpty()) {
                        newColumnValues[rowIndex] = spreadsheet.get(rowIndex)[columnIndex];
                    }
                }
            }

            mergedColumnValues.put(columnName, newColumnValues);
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
        return convertListOfClassesToArrayOfPrimitives(indicesToKeep);
    }

    private List<String[]> mergeDuplicates(List<String[]> spreadsheet, String columnName, int indexToKeep) {

        for (int rowIndex = 0; rowIndex < spreadsheet.size(); rowIndex++) {
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
