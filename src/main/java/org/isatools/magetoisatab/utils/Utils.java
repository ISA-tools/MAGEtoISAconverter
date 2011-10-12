package org.isatools.magetoisatab.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Utils {

    public static List<Column> createColumnOrderList(String[] columnNames) {
        LinkedList<Column> columnOrders = new LinkedList<Column>();
        for (int index = 0; index < columnNames.length; index++) {
            if (!columnNames[index].trim().equals("")) {
                columnOrders.add(new Column(index, columnNames[index]));
            }
        }

        return columnOrders;
    }

    public static int getIndexForValue(String value, List<Column> columnOrders) {
        int count = 0;

        for (Column column : columnOrders) {
            if (column.getLabel().equalsIgnoreCase(value)) {
                return count;
            }
            count++;
        }

        return -1;
    }

    public static int[] createIndexArray(List<Column> columnOrders) {
        int[] columnOrder = new int[columnOrders.size()];

        int count = 0;
        for (Column column : columnOrders) {
            columnOrder[count] = column.getIndex();
            count++;
        }

        return columnOrder;

    }

    public static List<String[]> cleanInput(List<String[]> spreadsheet, String geoAcc) {

        geoAcc = extractGEOAccession(geoAcc);

        List<String[]> cleanedData = new ArrayList<String[]>();

        boolean doGEOCleanup = geoAcc.startsWith("GSE") || geoAcc.startsWith("GDS");

        for (String[] line : spreadsheet) {

            if (doGEOCleanup) {
                for (int columnIndex = 0; columnIndex < line.length; columnIndex++) {
                    line[columnIndex] = line[columnIndex].replace(geoAcc, "");
                }
            }

            if (line.length > 0) {
                if (!line[0].trim().equals("")) {
                    cleanedData.add(line);
                }
            }


        }

        return cleanedData;
    }

    public static String extractGEOAccession(String accessionNumber) {
        Pattern geoaccnumregex = Pattern.compile("-");
        String[] items = geoaccnumregex.split(accessionNumber);

        String geo_orig = accessionNumber;
        if (items[1].contains("GEOD")) {
            geo_orig = "GSE" + items[2];
        }

        return geo_orig;
    }


    public static String[] correctColumnHeaders(String[] columnHeaders) {

        for (int index = 0; index < columnHeaders.length; index++) {
            // we take care of the MAGE-TAB Description field which sometimes shows up in AE output
            if (columnHeaders[index].equalsIgnoreCase("description")) {
                columnHeaders[index] = "Comment[description]";
            }


            if (columnHeaders[index].equalsIgnoreCase("characteristics [organism]")) {
                columnHeaders[index] = "Characteristics[organism]";
            }

            if (columnHeaders[index].equalsIgnoreCase("LabeledExtract Name")) {
                columnHeaders[index] = "Labeled Extract Name";
            }

            if (columnHeaders[index].contains("FactorValue")) {
                columnHeaders[index] = columnHeaders[index].replace("FactorValue", "Factor Value");
            }

            if (columnHeaders[index].contains("ArrayDesign")) {

            }

        }

        return columnHeaders;
    }


}
