package org.isatools.magetoisatab.io;

//TODO: propagate  factor values and cleanup

import org.isatools.io.FileType;
import org.isatools.io.Loader;
import org.isatools.magetoisatab.utils.Column;
import org.isatools.magetoisatab.utils.ProtocolREFUtil;
import org.isatools.magetoisatab.utils.Utils;
import org.isatools.manipulator.SpreadsheetManipulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Character;
import java.lang.String;
import java.lang.StringBuffer;
import java.util.*;


public class MAGETabSDRFLoader {

    public static final Character TAB_DELIM = '\t';

    public String[] columnNames;

    private int tt = -1;
    private int ptf = -1;

    private int firstNodePosition = -1;
    private int nodeDepth;

    private int studySamplePosition = 0;
    private int firstFactorPosition;
    private int firstNodeIfNoSamplePosition;


    private boolean isPresentSample = false;
    private boolean isPresentExtract = false;
    private boolean isPresentLE = false;
    private boolean isPresentHyb = false;
    private boolean isPresentAssay = false;

    public Map<Integer, String[]> rowValues;

    public Map<Integer, String[]> samples;

    public Set<String[]> assays;

    //public ArrayList<int[]> columns2drop;
    List columns2drop = new ArrayList();
    List columns2dropFromStudy = new ArrayList();


    public MAGETabSDRFLoader() {

        samples = new HashMap<Integer, String[]>();

         assays = new HashSet<String[]>();
    }

    public void loadsdrfTab(String url, String accnum) throws IOException {

        try {

            File file = new File(url);

            if (file.exists()) {

                Loader fileReader = new Loader();
                List<String[]> sheetData = fileReader.loadSheet(url, FileType.TAB);

                // clean up the input file, removing lines with no data.
                sheetData = Utils.cleanInput(sheetData, accnum);

                String[] columnNames = SpreadsheetManipulation.getColumnHeaders(sheetData);

                columnNames = Utils.correctColumnHeaders(columnNames);

                // initialization of the ArrayList which will receive all fields to be kept which are not factor value fields
                List<Integer> positions2keep = new ArrayList<Integer>();

                // initialization of the ArrayList which will receive all factor value fields to be kept
                // this will be used to propagate existing factor value to study sample file
                List<Integer> factorPositions2Keep = new ArrayList<Integer>();

                // now checking which fields need dropping and adding them to the ArrayList
                for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {

                    if (!columnNames[columnIndex].trim().equals("")) {

                        // we don't need TERM SOURCE REF if they are found right after Protocol REF
                        if (!((columnNames[columnIndex].equalsIgnoreCase("term source ref"))
                                && (columnNames[columnIndex - 1].equalsIgnoreCase("protocol ref")))) {

                            positions2keep.add(columnIndex);
                        }
                    }
                    if (columnNames[columnIndex].equalsIgnoreCase("technology type")) {
                        tt++;
                    }

                    if (columnNames[columnIndex].equalsIgnoreCase("comment [platform_title]")) {
                        ptf++;
                    }

                    if (columnNames[columnIndex].startsWith("Factor Value[")) {
                        factorPositions2Keep.add(columnIndex);
                    }
                }

                sheetData = SpreadsheetManipulation.getColumnSubset(sheetData, true, convertIntegers(positions2keep));

                //getting the associated header row in order to perform identification of fields position prior to reordering
                columnNames = SpreadsheetManipulation.getColumnHeaders(sheetData);

                List<Column> columnOrders = Utils.createColumnOrderList(columnNames);

                //where does Assay Name field appear?
                int assayNameIndex = Utils.getIndexForValue("Assay Name", columnOrders);

                //where does Derived Array Data File field appear?
                int derivedArrayDataFileIndex = Utils.getIndexForValue("Derived Array Data File", columnOrders);

                //scanning the header checking if it contains "Scan Name field"
                int   scanNameIndex=Utils.getIndexForValue("Scan Name", columnOrders);


                //System.out.println("SDRF Processing: assayName field found at location Index:"  + assayNameIndex);


                // if present, fetching and moving the technology type field
                if (tt >= 0) {

                    Column technology = columnOrders.remove(Utils.getIndexForValue("technology type", columnOrders));
                    columnOrders.add(assayNameIndex, technology);



                    System.out.println("SDRF Processing: SCAN: " + scanNameIndex);



//                    if ( (scanNameIndex<0)  || (derivedArrayDataFileIndex<0 ) ) {
//                        System.out.println("SDRF Processing: NO SCAN FOUND in this file");
//                    }

                    if (derivedArrayDataFileIndex>0 && scanNameIndex>0){
                        Column scanName = columnOrders.remove(Utils.getIndexForValue("Scan Name", columnOrders));
                        columnOrders.add(derivedArrayDataFileIndex - 1, scanName);
                    }
                    else if (derivedArrayDataFileIndex==0 && scanNameIndex>0) {
                        Column scanName = columnOrders.remove(Utils.getIndexForValue("Scan Name", columnOrders));
                        columnOrders.add(assayNameIndex + 1, scanName);

                    }


                   //System.out.println("SDRF Processing: SCAN" + scanNameIndex);

                }



                //fetching and moving the platform title field if present
                if (ptf >= 0) {
                    Column platformTitle = columnOrders.remove(Utils.getIndexForValue("comment [platform_title]", columnOrders));
                    columnOrders.add(assayNameIndex + 1, platformTitle);
                }

                // calling the getColumnSubset method and create a object containing the SDRF data bar all fields such as Term Source REF following a Protocol REF
                List<String[]> sheetDataSubset = SpreadsheetManipulation.getColumnSubset(sheetData, true, Utils.createIndexArray(columnOrders));


                // now preparing to process the cleaned SDRF subset and remove all aberrant Protocol REF fields where applicable
                //we initialize
                ProtocolREFUtil util = new ProtocolREFUtil();

                //we perform the transformation using the processSpreadsheet method
                sheetDataSubset = util.processSpreadsheet(sheetDataSubset);
                // you can read each line separately!

                String[] sampleRecord;
                String[] assayRecord;

                rowValues = new HashMap<Integer, String[]>();

                //a counter to count lines in input file
                int counter = 0;

                for (String[] nextLine : sheetDataSubset) {

                    //we are dealing with the header row and location the field 'Sample Name'
                    if (counter == 0) {

                        this.columnNames = nextLine;

                        if (getArrayAsString(columnNames).contains("Sample Name")) {
                            isPresentSample = true;
                        }
                        if (getArrayAsString(columnNames).contains("Extract Name")) {
                            isPresentExtract = true;
                        }
                        if (getArrayAsString(columnNames).contains("Labeled Extract Name")) {
                            isPresentLE = true;
                        }
                        if (getArrayAsString(columnNames).contains("Hybridization Name")) {
                            isPresentHyb = true;
                        }
                        if (getArrayAsString(columnNames).contains("Assay Name")) {
                            isPresentHyb = true;
                        }

                        if (!getArrayAsString(columnNames).contains("Sample Name")) {
                            if (!getArrayAsString(columnNames).contains("Extract Name")) {
                                if (!getArrayAsString(columnNames).contains("Labeled Extract Name")) {
                                    if (!getArrayAsString(columnNames).contains("Hybridization Name")) {
                                        firstNodePosition = 0;
                                        break;
                                    } else {
                                        for (int i = 0; i < nextLine.length; i++) {

                                            if ((nextLine[i].equals("Hybridization Name")) || (nextLine[i].equals("Assay Name"))) {
                                                nodeDepth = 4;
                                                System.out.println("SDRF Processing: Hyb: First Node Found is: " + nextLine[i]);
                                                firstNodePosition = i;
                                                break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                            }
                                        }
                                    }
                                } else {
                                    for (int i = 0; i < nextLine.length; i++) {

                                        if (nextLine[i].equals("Labeled Extract Name")) {
                                            nodeDepth = 3;
                                            System.out.println("SDRF Processing: LE: First Node Found is: " + nextLine[i]);
                                            firstNodePosition = i;
                                            break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                        }
                                    }

                                }


                            } else {
                                for (int i = 0; i < nextLine.length; i++) {

                                    if (nextLine[i].equals("Extract Name")) {
                                        nodeDepth = 2;
                                        System.out.println("SDRF Processing:Extract: First Node Found is: " + nextLine[i]);
                                        firstNodePosition = i;
                                        break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                    }
                                }

                            }
                        } else {
                            for (int i = 0; i < nextLine.length; i++) {

                                if (nextLine[i].equals("Sample Name")) {
                                    nodeDepth = 1;
                                    System.out.println("SDRF Processing: Sample: First Node Found is: " + nextLine[i]);
                                    firstNodePosition = i;
                                    break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                }
                            }


                        }


                        if (getArrayAsString(columnNames).contains("Factor Name")) {

                            for (int i = 0; i < nextLine.length; i++) {

                                System.out.println("SDRF Processing: "+ nextLine[i]);
                                if (nextLine[i].equals("Factor Name")) {

                                    firstFactorPosition = i;

                                    break;   // we have found the First Factor field, we can leave, from this point onwards
                                }
                            }


                        }

                    }

                    //we have located the first Material Node by parsing the Header, we now handle lines of data
                    else if (firstNodePosition > 0 && counter > 0) {

                        //now that we know where the 'Sample Name' field is, we are splitting the table

                        rowValues.put(counter, nextLine);

                        sampleRecord = new String[firstNodePosition + 1];

                        if (nextLine.length - firstNodePosition > 0) {

                            assayRecord = new String[nextLine.length - firstNodePosition];


                            for (int j = 0; j < nextLine.length; j++) {

                                if (j < firstNodePosition) {
                                    sampleRecord[j] = nextLine[j];
                                } else if (j == firstNodePosition) {
                                    sampleRecord[j] = nextLine[j];
                                    assayRecord[j - firstNodePosition] = nextLine[j];
                                } else {
                                    assayRecord[j - firstNodePosition] = nextLine[j];
                                }
                            }
                            assays.add(assayRecord);
                        } else {
                            break;
                        }

                        addStudySample(sampleRecord);

                    } else if (firstNodePosition > 0 && counter > 0 && nodeDepth == 1) {

                        rowValues.put(counter, nextLine);

                        sampleRecord = new String[firstNodePosition + 1];


                        if (nextLine.length - firstNodePosition > 0) {

                            assayRecord = new String[nextLine.length - firstNodePosition + 1];


                            for (int j = 0; j < nextLine.length; j++) {

                                if (j < firstNodePosition) {
                                    sampleRecord[j] = nextLine[j];
                                } else if (j == firstNodePosition) {
                                    sampleRecord[j] = nextLine[j];
                                    assayRecord[j - firstNodePosition] = nextLine[j];
                                } else {
                                    assayRecord[j - firstNodePosition] = nextLine[j];
                                }
                            }
                            assays.add(assayRecord);
                        } else {
                            break;
                        }

                        addStudySample(sampleRecord);

                    } else if (firstNodePosition > 0 && counter > 0 && nodeDepth == 2) {

                        rowValues.put(counter, nextLine);

                        sampleRecord = new String[firstNodePosition + 1];


                        if (nextLine.length - firstNodePosition > 0) {

                            assayRecord = new String[nextLine.length - firstNodePosition + 2];


                            for (int j = 0; j < nextLine.length; j++) {

                                if (j < firstNodePosition) {
                                    sampleRecord[j] = nextLine[j];
                                } else if (j == firstNodePosition) {
                                    sampleRecord[j] = nextLine[j];
                                    assayRecord[j - firstNodePosition] = nextLine[j] + "\t" + nextLine[j];
                                } else {
                                    assayRecord[j - firstNodePosition] = nextLine[j];
                                }
                            }
                            assays.add(assayRecord);
                        } else {
                            break;
                        }

                        addStudySample(sampleRecord);

                    } else if (firstNodePosition > 0 && counter > 0 && nodeDepth == 3) {

                        rowValues.put(counter, nextLine);

                        sampleRecord = new String[firstNodePosition + 1];


                        if (nextLine.length - firstNodePosition > 0) {

                            assayRecord = new String[nextLine.length - firstNodePosition + 3];

                            for (int j = 0; j < nextLine.length; j++) {

                                if (j < firstNodePosition) {
                                    sampleRecord[j] = nextLine[j];
                                } else if (j == firstNodePosition) {
                                    sampleRecord[j] = nextLine[j];
                                    assayRecord[j - firstNodePosition] = nextLine[j] + "\t" + nextLine[j] + "\t" + nextLine[j];
                                } else {
                                    assayRecord[j - firstNodePosition] = nextLine[j];
                                }
                            }
                            assays.add(assayRecord);
                        } else {
                            break;
                        }

                        addStudySample(sampleRecord);

                    } else {
                        System.out.println("SDRF Processing: SDRF lacks SAMPLE NAME header");
                    }
                    counter++;
                }
                printFiles(accnum);
            } else {
                System.out.println("SDRF Processing: ERROR: file not found!");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void addStudySample(String[] sampleBlock) {

        StringBuffer blockAsString = new StringBuffer();

        for (String value : sampleBlock) {
            blockAsString.append(value);
        }

        int hashValue = blockAsString.toString().hashCode();

        if (!samples.containsKey(hashValue)) {
            samples.put(hashValue, sampleBlock);
        }
    }

//    private Set<Integer> getColumnIndexForName(String columnName) {
//        Set<Integer> indexes = new HashSet<Integer>();
//
//        for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {
//            if (columnNames[columnIndex].equalsIgnoreCase(columnName)) {
//                indexes.add(columnIndex);
//            }
//        }
//
//        return indexes;
//    }
//
//
//    private Map<Integer, List<String>> getValuesForColumns(Set<Integer> columnIndexes) {
//
//        Map<Integer, List<String>> values = new HashMap<Integer, List<String>>();
//
//        for (int rowIndex : rowValues.keySet()) {
//            for (int columnIndex : columnIndexes) {
//                if (!values.containsKey(columnIndex)) {
//                    values.put(columnIndex, new ArrayList<String>());
//                }
//                values.get(columnIndex).add(rowValues.get(rowIndex)[columnIndex]);
//            }
//        }
//
//        return values;
//    }


    public void printFiles(String accnum) {

        try {
            String studySampleHeaders = "";
            String studyAssayHeaders = "";

            //This test catches malformed MAGE-TAB files!
            if (firstNodePosition > 0) {

                for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {


                    //now dealing with descriptors that will go on the ISA study sample sheet, i.e. everything before the first Material Node after Source Node
                    if (columnIndex < firstNodePosition) {

                        studySampleHeaders += columnNames[columnIndex] + "\t";
                    }

                    if (columnIndex == firstNodePosition) {

                        if (!(columnNames[firstNodePosition].equalsIgnoreCase("sample name"))) {

                            studySampleHeaders += "Sample Name" + "\t";
                            studyAssayHeaders = "Sample Name" + "\t";
                        } else {
                            studyAssayHeaders = columnNames[firstNodePosition] + "\t";
                            studySampleHeaders += columnNames[columnIndex] + "\t";
                        }
                    }


                    //now dealing with descriptions going to the ISA assay spreadsheet
                    else if (columnIndex > firstNodePosition) {

                        if (columnNames[columnIndex].equalsIgnoreCase("Hybridization Name")) {
                            columnNames[columnIndex] = "Hybridization Assay Name";
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        }

                        else if (columnNames[columnIndex].equalsIgnoreCase("Assay Name")) {
                            columnNames[columnIndex] = "Assay Name";
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        }

                        //Here in case Sequencing is used (tt>=) we replace MAGE-TAB Labeled Extract Name field with Protocol REF
                        else if ((columnNames[columnIndex].equalsIgnoreCase("Labeled Extract Name")) && (tt >= 0)) {
                            columnNames[columnIndex] = "Protocol REF";
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        }

                        //Here in case Sequencing is used (tt>=) we replace MAGE-TAB Label field with Parameter Value[library layout]
                        else if ((columnNames[columnIndex].equalsIgnoreCase("Label")) && (tt >= 0)) {
                            columnNames[columnIndex] = "Parameter Value[library layout]";
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        }
                        //Here in case Sequencing is used (tt>=) we replace MAGE-TAB Technology Type field with Parameter Value[library layout]
                        else if ((columnNames[columnIndex].equalsIgnoreCase("Technology Type")) && (tt >= 0)) {
                            columnNames[columnIndex] = "Parameter Value[mid]";
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        }
                        //Here in case Sequencing is used (tt>=) we replace MAGE-TAB Technology Type field with Parameter Value[library layout]
                        else if ((columnNames[columnIndex].equalsIgnoreCase("comment [platform_title]")) && (tt >= 0)) {
                            columnNames[columnIndex] = "Parameter Value[sequencing instrument]";
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        }

                        //Here in case Sequencing is used (tt>=) we replace MAGE-TAB Labeled Extract Name field with Protocol REF
                        else if ((columnNames[columnIndex].equalsIgnoreCase("Scan Name")) && (tt >= 0)) {
                            columnNames[columnIndex] = "Data Transformation Name";
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        } else if (columnNames[columnIndex].equalsIgnoreCase("comment [FASTQ_URI]")) {
                            columnNames[columnIndex] = "Raw Data File";
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        } else if ((columnNames[columnIndex].equalsIgnoreCase("Derived Array Data File")) && (tt >= 0)) {
                            columnNames[columnIndex] = "Derived Data File";
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        } else if (columnNames[columnIndex].startsWith("FactorValue ")) {
                            columnNames[columnIndex] = columnNames[columnIndex].toLowerCase();
                            columnNames[columnIndex] = columnNames[columnIndex].replaceAll("factorvalue ", "Factor Value");
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        } else {
                            studyAssayHeaders += columnNames[columnIndex] + "\t";
                        }
                    }
                }

            } else {

                System.out.println("SDRF Processing: REALLY?? Study Sample position is: " + firstNodePosition);

                studySampleHeaders = studySampleHeaders + "Sample Name";
            }

            PrintStream studyPs = new PrintStream(new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/s_" + accnum + "_studysample.txt"));

            studyPs.println(studySampleHeaders);


            for (int hash : samples.keySet()) {
                if (getArrayAsString4Study(samples.get(hash)).length() > 0) {
                    studyPs.println(getArrayAsString4Study(samples.get(hash)));
                }
            }


            studyPs.flush();
            studyPs.close();


            //we print the assay file
            PrintStream assayPs = new PrintStream(new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/a_" + accnum + "_assay.txt"));

            assayPs.println(studyAssayHeaders);

            for (String[] assaySection : assays) {
                if (getArrayAsString(assaySection).length() > 0) {
                    assayPs.println(getArrayAsString(assaySection));
                }
            }


            assayPs.flush();
            assayPs.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    /**
     * A method to create a new record.
     *
     * @param array columns2drop
     * @return a record as a string
     */
    private String getArrayAsString(String[] array) {

        StringBuffer blockAsString = new StringBuffer();
        int val = 0;

        for (String anArray : array) {

            // here we check which fields to drop
            if ((columns2drop.contains(val))) {
                val++;
            }
            //otherwise we print
            else if (anArray != null) {

                //provided the value is not equal to null, we append to the record
                blockAsString.append(anArray);

                //join record with a tab
                if ((val != array.length - 1)) {
                    blockAsString.append("\t");
                    val++;
                }
            }
        }

        return blockAsString.toString();
    }


    /**
     * A method to create a new record.
     *
     * @param array columns2drop
     * @return a record as a string
     */
    private String getArrayAsString4Study(String[] array) {

        StringBuffer blockAsString = new StringBuffer();
        int val = 0;

        for (String anArray : array) {

            // here we check which fields to drop from output Study_Sample file
            if (columns2dropFromStudy.contains(val)) {
                val++;
            }
            //otherwise we print
            else if (anArray != null) {

                //provided the value is not equal to null, we append to the record
                blockAsString.append(anArray);

                //join record with a tab
                if ((val != array.length - 1)) {
                    blockAsString.append("\t");
                    val++;
                }
            }
        }

        return blockAsString.toString();
    }


    public static int[] convertIntegers(List<Integer> integers) {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int index = 0; index < ret.length; index++) {
            ret[index] = iterator.next();
        }
        return ret;
    }


}

