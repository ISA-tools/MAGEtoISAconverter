package org.isatools.magetoisatab.io.fileprocessing;

import java.util.ArrayList;
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
        List<WrongLocations> wrongLocationsList = processColumnHeaders(columnHeaders);

        System.out.println(wrongLocationsList.size());

        for (WrongLocations wrongLocations : wrongLocationsList) {
            String inferredType = inferProtocolType(wrongLocations, columnHeaders);
            if(inferredType != null) {
                System.out.println(columnHeaders[wrongLocations.firstNode] + " to " + columnHeaders[wrongLocations.lastNode] + " : type should be " + inferredType);
            }
        }

        return null;
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
                if (lastProtocolREFIndex == -1) {
                    // we have a rogue column
                    currentWrongLocation = new WrongLocations(currentIndex, lastNodeIndex);
                }
            } else {
                lastProtocolREFIndex = -1;
            }


            currentIndex++;
        }

        return wrongLocationsList;
    }

    private boolean isOtherProtocolRelatedColumn(String columnName) {
        return columnName.equals("Performer") || columnName.equals("Date") || columnName.contains("Parameter Value");
    }
    
    private String inferProtocolType(WrongLocations wrongLocations, String[] columnHeaders) {
        
        String firstNode = columnHeaders[wrongLocations.firstNode];
        String lastNode = columnHeaders[wrongLocations.lastNode];
        
        InferredProtocolTypes inferredType = InferredProtocolTypes.selectTypeGivenNodes(firstNode, lastNode);
        if(inferredType != null) {
            return inferredType.getType();    
        }
        
        return null;
    }


    class WrongLocations {

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
    }

    public static void main(String[] args) {
        List<String[]> spreadsheet = new ArrayList<String[]>();

        spreadsheet.add(new String[]{"Sample Name", "Extract Name", "Parameter Value[sequencing instrument]", "Parameter Value[library selection]", "Parameter Value[library_source]",
                "Parameter Value[library_strategy]", "Parameter Value[library layout]", "Comment [Platform_title]", "Labeled Extract Name", "Comment [ENA_EXPERIMENT]", "Protocol REF", "Protocol REF", "Assay Name",
                "Parameter Value[run identifier]", "Raw Data File", "Derived Data File", "Comment [Derived ArrayExpress FTP file]", "Factor Value[barcode (first 3 nt for fastq files of chip-seq libraries, first 4 nt for fastq files of small rna libraries)]",
                "Term Source REF", "Term Accession Number", "Term Source REF", "Term Accession Number", "Factor Value[generation]", "Factor Value[rnai target: chri]",
                "Factor Value[strain]"});

        CleanupUtils cleanup = new ProtocolInsertionUtil();
        cleanup.processSpreadsheet(spreadsheet);
    }
}
