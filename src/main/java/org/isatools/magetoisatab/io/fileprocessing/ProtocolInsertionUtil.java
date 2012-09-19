package org.isatools.magetoisatab.io.fileprocessing;

import org.isatools.manipulator.SpreadsheetManipulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: prs
 * Date: 25/07/2012
 * Time: 18:13
 * To change this template use File | Settings | File Templates.
 */
public class ProtocolInsertionUtil extends CleanupUtils {

    @Override
    public List<String[]> processSpreadsheet(List<String[]> spreadsheet) {
        String[] columnHeaders = spreadsheet.get(0);
        int libraryConstruction = 0;
        List<WrongLocations> wrongLocationsList = processColumnHeaders(columnHeaders);

        Collections.sort(wrongLocationsList);
       List<WrongLocations> cleanedWrongLocationsList = new ArrayList<WrongLocations>();
        cleanedWrongLocationsList = (List<WrongLocations>) removeDuplicates(wrongLocationsList);

        for (WrongLocations wrongLocations : cleanedWrongLocationsList) {

            String inferredType = inferProtocolType(wrongLocations, columnHeaders);
            final int valueToInsertAt = wrongLocations.getParameterValueLocation();
            inferredType = (inferredType == null) ? "" : inferredType;

            System.out.println("wrong location: "+wrongLocations.getParameterValueLocation() +" @ " + columnHeaders[wrongLocations.getParameterValueLocation()]);

            if (columnHeaders[valueToInsertAt].contains("ibrary")) {  //||columnHeaders[valueToInsertAt].contains("mid-L")
                //TODO: add as missing from investigation file  by calling a addProtocolObject2InvFile method
                inferredType="library construction";
                spreadsheet = SpreadsheetManipulation.insertColumn(spreadsheet, "Protocol REF", valueToInsertAt, inferredType);
                //System.out.println("inferred protocol is: " + inferredType.toString());
                //System.out.println("insertion before: "+ columnHeaders[valueToInsertAt]);
            }

           else if (columnHeaders[valueToInsertAt].contains("instrument") && (columnHeaders[valueToInsertAt-1].contains("run")||columnHeaders[valueToInsertAt+1].contains("run") ))    {     //
                //TODO: add as missing from investigation file
                inferredType="nucleic acid sequencing";
                spreadsheet = SpreadsheetManipulation.insertColumn(spreadsheet, "Protocol REF", valueToInsertAt, inferredType);
                //System.out.println("inferred protocol is:: " + inferredType);

            }

            else if (columnHeaders[valueToInsertAt].contains("instrument") && (!columnHeaders[valueToInsertAt-1].contains("run")&& !columnHeaders[valueToInsertAt+1].contains("run") ))    {     //
                inferredType="nucleic acid sequencing";
                spreadsheet = SpreadsheetManipulation.insertColumn(spreadsheet, "Protocol REF", valueToInsertAt, inferredType);
                //System.out.println("inferred protocol is:: " + inferredType);

            }

        }

        return spreadsheet;
    }

    private List<WrongLocations> processColumnHeaders(String[] columnNames) {
        // we want to find:
        //  - 1) if a Parameter value exists on it's own, we need to insert a Protocol REF before
        // we also need to know what type given the nodes before and after. These nodes are not characteristics, etc.
        List<WrongLocations> wrongLocationsList = new ArrayList<WrongLocations>();

        WrongLocations currentWrongLocation = null;
        int lastNodeIndex = -1;
        int lastProtocolREFIndex = -1;
        int currentIndex = 0;

        for (String columnName : columnNames) {
            // detecting nodes
            if (!isColumnNameOk(columnName)) {
                if (lastNodeIndex != -1) {
                    if (currentWrongLocation != null) {
                        currentWrongLocation.setLastNode(currentIndex);
                        wrongLocationsList.add(currentWrongLocation);
                        currentWrongLocation = null;
                    }
                    lastProtocolREFIndex = -1;
                }
                lastNodeIndex = currentIndex;

            } else {
                lastProtocolREFIndex = -1;
            }

            // detecting the protocol refs
            if (columnName.equals("Protocol REF")) {
                lastProtocolREFIndex = currentIndex;
            }

            // checking if isn't a lone protocol value
            if (isOtherProtocolRelatedColumn(columnName)) {
                // check if we've seen a protocol ref before this...
                if (lastProtocolREFIndex == -1 && currentWrongLocation == null) {
                    // we have a rogue column
                    currentWrongLocation = new WrongLocations(currentIndex, lastNodeIndex);
                    if (!wrongLocationsList.contains(currentWrongLocation))
                    wrongLocationsList.add(currentWrongLocation);
                }
            }

            if  (isSequencingInstrumentRelatedColumn(columnName)) {

                // check if we've seen a protocol ref before this...
                if (lastProtocolREFIndex == -1 && currentWrongLocation == null) {
                    // we have a rogue column
                    currentWrongLocation = new WrongLocations(currentIndex, lastNodeIndex);
                    if (!wrongLocationsList.contains(currentWrongLocation))
                    wrongLocationsList.add(currentWrongLocation);
                    System.out.println("from isSequencingInstrumentRelatedColumn method call: " + columnName + "current index: "+currentIndex);
                }
                else if  (lastProtocolREFIndex == -1 && currentWrongLocation != null)  {
                    currentWrongLocation = new WrongLocations(currentIndex, lastNodeIndex);
                    if (!wrongLocationsList.contains(currentWrongLocation))
                    wrongLocationsList.add(currentWrongLocation);
                }
            }

            else {
                lastProtocolREFIndex = -1;
            }

            currentIndex++;
        }

        return wrongLocationsList;
    }

    private boolean isOtherProtocolRelatedColumn(String columnName) {
        return columnName.equals("Performer") || columnName.equals("Date") ||  columnName.contains("Parameter Value") ;
    }


    private boolean isSequencingInstrumentRelatedColumn(String columnName) {
        return columnName.contains("sequencing instrument");
    }

    private String inferProtocolType(WrongLocations wrongLocations, String[] columnHeaders) {

        String firstNode = columnHeaders[wrongLocations.firstNode];
        String lastNode = columnHeaders[wrongLocations.lastNode];

        InferredProtocolTypes inferredType = InferredProtocolTypes.selectTypeGivenNodes(firstNode, lastNode);
        if (inferredType != null) {


            return inferredType.getType();

        }

        return null;
    }

    public Collection removeDuplicates(Collection c) {
// Returns a new collection with duplicates removed from passed collection.
        Collection result = new ArrayList();

        for(Object o : c) {
            if (!result.contains(o)) {
                result.add(o);
            }
        }

        return result;
    }




    class WrongLocations implements Comparable<WrongLocations> {

        int parameterValueLocation;
        int firstNode, lastNode;

        WrongLocations(int parameterValueLocation, int firstNode) {
            this.parameterValueLocation = parameterValueLocation;
            this.firstNode = firstNode;
        }

        public void setLastNode(int lastNode) {
            this.lastNode = lastNode;
        }

        public int getParameterValueLocation() {
            return parameterValueLocation;
        }

        public int getFirstNode() {
            return firstNode;
        }

        public int getLastNode() {
            return lastNode;
        }

        public int compareTo(WrongLocations wrongLocations) {
            return wrongLocations.getParameterValueLocation() < parameterValueLocation ?
                    -1 : wrongLocations.getParameterValueLocation() == parameterValueLocation
                    ? 0 : 1;
        }
    }
}
