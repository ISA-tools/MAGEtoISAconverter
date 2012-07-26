package org.isatools.magetoisatab.io.fileprocessing;
/**
 * Created by IntelliJ IDEA.
 * User: prs
 * Date: 24/07/2012
 * Time: 00:48
 * To change this template use File | Settings | File Templates.
 */
//public class ColumnMoveUtil extends CleanupUtils {
//    static Set<String> columnsToLookFor;
//
//    static {
//        columnsToLookFor = new HashSet<String>();
//
//        columnsToLookFor.add("Parameter Value[run identifier]");
//        //columnsToLookFor.add("Parameter Value[sequencing instrument]");
//       // columnsToLookFor.add("Assay Name");
//    }

//    public List<String[]> processSpreadsheet(List<String[]> spreadsheet) {
//        try {
//            String[] columnHeaders = spreadsheet.get(0);
//          //  int[] indicesToKeep = locateAndMoveColumns(columnHeaders);
//
//            System.out.println("Column header size is:" + columnHeaders.length);
//            System.out.println("Indices to keep is: " + indicesToKeep.length);
//            if(indicesToKeep.length == columnHeaders.length) {
//                return spreadsheet;
//            }
//            return SpreadsheetManipulation.getColumnSubset(spreadsheet, true, indicesToKeep);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return spreadsheet;
//        }
//    }

//    public int[] locateAndMoveColumns(String[] columnHeaders) {
//        List<Integer> indexesToMove = new ArrayList<Integer>();
//
//        for (int columnIndex = 0; columnIndex < columnHeaders.length; columnIndex++) {
//            if (columnIndex != 0) {
//                // check if last column is one which should have term source and accession
//                if (columnsToLookFor.contains(columnHeaders[columnIndex])) {
//                    // we want to check for column 1 or two back in the case of the source ref and accession columns.
//                    if (isColumnName4PVOk(columnHeaders[columnIndex - 1])) {
//                        indexesToMove.add(columnIndex);
//                    }
//                    } else {
//                        System.out.println(columnIndex + " should be removed!");
//                    }
//                } else {
//                    indexesToKeep.add(columnIndex);
//                }
//            } else {
//                indexesToKeep.add(columnIndex);
//            }
//        }
//
//        return convertListOfClassesToArrayOfPrimitives(indexesToKeep);
//    }
//    }
//}
