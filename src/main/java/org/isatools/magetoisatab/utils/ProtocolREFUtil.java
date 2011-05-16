package org.isatools.magetoisatab.utils;

import org.isatools.manipulator.SpreadsheetManipulation;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Philippe
 * Date: 13/05/2011
 * Time: 15:15
 * To change this template use File | Settings | File Templates.
 */
public class ProtocolREFUtil {

    private Map<Integer, Integer> selectCandidates(String[] columnNames) {

        Map<Integer, Integer> candidates = new HashMap<Integer, Integer>();

        int startIndex = -1;
        int length = 0;

        for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {

            if (columnNames[columnIndex].equals("Protocol REF")) {

                if (startIndex == -1) {
                    startIndex = columnIndex;
                }
                length++;
            } else {
                // store the current result;
                if (startIndex > -1 && length > 1) {
                    candidates.put(startIndex, length);
                }

                // reset the counter
                startIndex = -1;
                length = 0;
            }
        }

        // final cleanup
        if (startIndex > -1 && length > 1) {
            candidates.put(startIndex, length);
        }

        return candidates;
    }

    public List<String[]> processSpreadsheet(List<String[]> spreadsheet) {
        String[] columnNames = spreadsheet.get(0);

        Map<Integer, Integer> candidates = selectCandidates(columnNames);

        Map<Integer, Map<Integer, Set<String>>> rowToProtocolValues
                = new HashMap<Integer, Map<Integer, Set<String>>>();

        // at this point we now have the cases where there are multiple chained Protocol REFs
        for (int rowNumber = 1; rowNumber < spreadsheet.size(); rowNumber++) {

            String[] column = spreadsheet.get(rowNumber);

            rowToProtocolValues.put(rowNumber, new HashMap<Integer, Set<String>>());

            for (int startIndex : candidates.keySet()) {
                rowToProtocolValues.get(rowNumber).put(startIndex, new HashSet<String>());

                for (int columnIndex = startIndex;
                     columnIndex < startIndex + candidates.get(startIndex); columnIndex++) {

                    if (columnIndex < column.length) {
                        if (!column[columnIndex].equals("")) {
                            rowToProtocolValues.get(rowNumber).get(startIndex).add(column[columnIndex]);
                        }
                    }
                }
            }
        }

        for (int rowNumber : rowToProtocolValues.keySet()) {
            System.out.println("Row # " + rowNumber);
            for (int startIndex : rowToProtocolValues.get(rowNumber).keySet()) {
                System.out.println("Was expecting " + candidates.get(startIndex) +
                        ", instead I found " + rowToProtocolValues.get(rowNumber).get(startIndex).size());

            }
        }

        Map<Integer, List<String[]>> protocolBlocks = createNewProtocolBlock(rowToProtocolValues);

        // at this point, we should now have the protocol blocks for reinsertion back into the List<String[]> object
        for (int startIndex : protocolBlocks.keySet()) {
            System.out.println("For start index " + startIndex);
            for (String[] columnValues : protocolBlocks.get(startIndex)) {
                for (String columnValue : columnValues) {
                    System.out.print(columnValue);
                }
                System.out.print("\n");
            }
        }


        return reconstructSpreadsheetAfterProcessing(protocolBlocks, candidates, spreadsheet);


    }

    private Map<Integer, List<String[]>> createNewProtocolBlock(Map<Integer, Map<Integer, Set<String>>> rowToProtocolValues) {

        Map<Integer, List<String[]>> builtProtocolBlocks = new HashMap<Integer, List<String[]>>();

        for (int rowNumber : rowToProtocolValues.keySet()) {

            for (int startIndex : rowToProtocolValues.get(rowNumber).keySet()) {

                int numberOfProtocols = calculateNumberOfProtocols(rowToProtocolValues, startIndex);

                String[] protocols = new String[numberOfProtocols];

                String[] values = rowToProtocolValues.get(rowNumber).get(startIndex)
                        .toArray(new String[rowToProtocolValues.get(rowNumber).get(startIndex).size()]);

                for (int protocolIndex = 0; protocolIndex < protocols.length; protocolIndex++) {
                    if (protocolIndex < values.length) {
                        protocols[protocolIndex] = values[protocolIndex];
                    } else {
                        protocols[protocolIndex] = "";
                    }
                }

                if (!builtProtocolBlocks.containsKey(startIndex)) {
                    builtProtocolBlocks.put(startIndex, new ArrayList<String[]>());
                    builtProtocolBlocks.get(startIndex).add(createHeaderBlock(numberOfProtocols));
                }

                builtProtocolBlocks.get(startIndex).add(protocols);
            }
        }

        return builtProtocolBlocks;
    }

    private List<String[]> reconstructSpreadsheetAfterProcessing(Map<Integer, List<String[]>> protocolBlocks, Map<Integer, Integer> candidates, List<String[]> spreadsheet) {

        List<String[]> newSpreadsheet = new ArrayList<String[]>();

        List<Integer> values = new ArrayList<Integer>();
        values.addAll(protocolBlocks.keySet());
        Collections.sort(values);


        for (int startIndex = values.size() - 1; startIndex >= 0; startIndex--) {
            int rowCount = 0;
            System.out.println("Performing replacement from startindex " + values.get(startIndex));
            if (protocolBlocks.get(values.get(startIndex)) != null) {

                for (String[] columnValues : spreadsheet) {
                    if (values.get(startIndex) < columnValues.length) {

                        List<String> newColumnValues = new ArrayList<String>();

                        newColumnValues.addAll(Arrays.asList(columnValues).subList(0, values.get(startIndex)));

                        Collections.addAll(newColumnValues, protocolBlocks.get(values.get(startIndex)).get(rowCount));

                        // by now we've added the new protocol block. We now need to add the remaining columns, not part of the merge

                        int previousProtocolSize = candidates.get(values.get(startIndex));

                        newColumnValues.addAll(Arrays.asList(columnValues).subList(values.get(startIndex) + previousProtocolSize, columnValues.length));

                        newSpreadsheet.add(newColumnValues.toArray(new String[newColumnValues.size()]));

                        rowCount++;
                    }

                }

                protocolBlocks.remove(values.get(startIndex));

                System.out.println("Processed spreadsheet after replacement of " + values.get(startIndex));

                for (String[] columnValues : newSpreadsheet) {
                    for (String columnValue : columnValues) {
                        System.out.print(columnValue + "\t");
                    }
                    System.out.print("\n");
                }

                reconstructSpreadsheetAfterProcessing(protocolBlocks, candidates, newSpreadsheet);
            }

        }

        System.out.println("Returning spreadsheet of size: " + spreadsheet.size() + " from reconstructSpreadsheetAfterProcessing()");

        return spreadsheet;
    }

    private String[] createHeaderBlock(int numberOfProtocols) {
        String[] header = new String[numberOfProtocols];

        for (int protocolIndex = 0; protocolIndex < numberOfProtocols; protocolIndex++) {
            header[protocolIndex] = "Protocol REF";
        }

        return header;
    }

    private int calculateNumberOfProtocols(Map<Integer, Map<Integer, Set<String>>> rowToProtocolValues, int startIndex) {

        int maxProtocols = -1;
        for (int rowNumber : rowToProtocolValues.keySet()) {


            int numberProtocols = rowToProtocolValues.get(rowNumber).get(startIndex).size();
            if (numberProtocols > maxProtocols) {
                maxProtocols = numberProtocols;
            }
        }

        return maxProtocols;
    }

    public static void main(String[] args) {

        List<String[]> testList = new ArrayList<String[]>();

        testList.add(new String[]{"Sample Name", "Label", "Protocol REF", "Protocol REF", "Protocol REF", "Extract Name", "Protocol REF", "Protocol REF", "Array Data File"});
        testList.add(new String[]{"sample1", "cy3", "prot1", "", "", "extract1", "", "prot2", "blah.cel"});
        testList.add(new String[]{"sample2", "cy5", "", "prot1", "prot1", "extract1", "prot1", "", "blah.cel"});
        testList.add(new String[]{"sample3", "cy5", "", "", "prot1", "extract1", "prot1", "", "blah.cel"});

        ProtocolREFUtil util = new ProtocolREFUtil();

        util.processSpreadsheet(testList);

    }
}
