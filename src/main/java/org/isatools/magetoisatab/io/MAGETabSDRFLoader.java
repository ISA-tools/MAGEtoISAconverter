package org.isatools.magetoisatab.io;

//TODO: prppagate  factor values and cleanup

import java.io.*;

import au.com.bytecode.opencsv.CSVReader;
import com.sun.tools.corba.se.idl.toJavaPortable.Factories;
import org.isatools.io.FileType;
import org.isatools.io.Loader;
import org.isatools.magetoisatab.utils.Column;
import org.isatools.magetoisatab.utils.ProtocolREFUtil;

import javax.xml.bind.SchemaOutputResolver;
import java.util.List;
import java.util.*;
import java.lang.*;

import org.isatools.magetoisatab.utils.Utils;
import org.isatools.manipulator.SpreadsheetManipulation;


public class MAGETabSDRFLoader {

    public static final Character TAB_DELIM = '\t';

    public String[] columnNames;

   private int tt=-1;
   private int ptf=-1;

    private int firstNodePosition=-1;
    private int nodeDepth;

    private int studySamplePosition=0;
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
    ArrayList columns2drop = new ArrayList();
    ArrayList columns2dropFromStudy = new ArrayList();





    public MAGETabSDRFLoader() {

        samples = new HashMap<Integer, String[]>();
        assays = new HashSet<String[]>();
    }







    /**
     * A method to read in an SDRF file and process it.
     *
     * @param url
     * @param accnum
     * @throws IOException
     */

    public void loadsdrfTab(String url, String accnum) throws IOException {

        try {

            File file = new File(url);

            if (file.exists()) {

                Loader fileReader = new Loader();
                List<String[]> sheetData = fileReader.loadSheet(url, FileType.TAB);

                // clean up the input file, removing lines with no data.
                sheetData = Utils.cleanInput(sheetData);

                SpreadsheetManipulation manipulation = new SpreadsheetManipulation();
                String[] columnNames = manipulation.getColumnHeaders(sheetData);

                // initialization of the ArrayList which will receive all fields to be kept which are not factor value fields
                List<Integer> positions2keep = new ArrayList<Integer>();


                // initialization of the ArrayList which will receive all factor value fields to be kept
                // this will be used to propagate existing factor value to study sample file
                List<Integer> factorPositions2Keep = new ArrayList<Integer>();

                // now checking which fields need dropping and adding them to the ArrayList
                for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {

                   // System.out.println("currentfield is " + columnNames[columnIndex]);

                    if (!columnNames[columnIndex].trim().equals("")) {

                        // we don't this
                        if (!((columnNames[columnIndex].equalsIgnoreCase("term source ref"))
                                && (columnNames[columnIndex - 1].equalsIgnoreCase("protocol ref")))) {

                            positions2keep.add(columnIndex);
                            System.out.println("currentfield is " + columnNames[columnIndex]);
                        }
                    }
                    if  (columnNames[columnIndex].equalsIgnoreCase("technology type"))   { tt++; }

                    if  (columnNames[columnIndex].equalsIgnoreCase("comment [platform_title]"))   { ptf++; }

                    //we are now storing the indices where factor value fields can be found
                    //TODO: append those at the end of output matrices
                    if (columnNames[columnIndex].startsWith("Factor Value[")) {

                        factorPositions2Keep.add(columnIndex);

                    }
                }



//                    if (columnNames[value].equalsIgnoreCase("label")) {
//                        labelPosition = value;
//                    }
//

                //calling spreadsheet manipulator to produce transient, slimmer input


                sheetData = manipulation.getColumnSubset(sheetData, true, convertIntegers(positions2keep));

                //getting the associated header row in order to perform identification of fields position prior to reordering
                columnNames = manipulation.getColumnHeaders(sheetData);

                LinkedList<Column> columnOrders = Utils.createColumnOrderList(columnNames);

                int assayNameIndex = Utils.getIndexForValue("Assay Name", columnOrders);

                //fetching and moving the technology type field  if present
                 if (tt>=0) {
                Column technology = columnOrders.remove(Utils.getIndexForValue("technology type", columnOrders));
                columnOrders.add(assayNameIndex, technology);
                 }

                //fetching and moving the platform title field if present
                if (ptf>=0){
                Column platformTitle = columnOrders.remove(Utils.getIndexForValue("comment [platform_title]", columnOrders));
                columnOrders.add(assayNameIndex + 1, platformTitle);
                }
                System.out.println("Reordered columns");

                for (Column column : columnOrders) {
                    System.out.println("\t" + column.getIndex() + " - " + column.getLabel());
                }


                // calling the getColumnSubset method and create a object containing the SDRF data bar all fields such as Term Source REF following a Protocol REF
                List<String[]> sheetDataSubset = manipulation.getColumnSubset(sheetData, true, Utils.createIndexArray(columnOrders));


                // now preparing to process the cleaned SDRF subset and remove all aberrant Protocol REF fields where applicable
                //we initialize
                ProtocolREFUtil util = new ProtocolREFUtil();

                //we perform the transformation using the processSpreadsheet method
                sheetDataSubset = util.processSpreadsheet(sheetDataSubset);
                // you can read each line separately!

                System.out.println("After processing, sheetDataSubset is of size " + sheetDataSubset.size());

                for (String[] columnValues : sheetDataSubset) {
                    for (String columnValue : columnValues) {
                        System.out.print(columnValue + "\t");
                    }
                    System.out.print("\n");
                }

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

                                            if  ( (nextLine[i].equals("Hybridization Name")) || (nextLine[i].equals("Assay Name")) ) {
                                                nodeDepth = 4;
                                                System.out.println("Hyb: First Node Found is: " + nextLine[i]);
                                                firstNodePosition = i;
                                                break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                            }
                                        }
                                    }
                                } else {
                                    for (int i = 0; i < nextLine.length; i++) {

                                        if (nextLine[i].equals("Labeled Extract Name")) {
                                            nodeDepth = 3;
                                            System.out.println("LE: First Node Found is: " + nextLine[i]);
                                            firstNodePosition = i;
                                            break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                        }
                                    }

                                }


                            } else {
                                for (int i = 0; i < nextLine.length; i++) {

                                    if (nextLine[i].equals("Extract Name")) {
                                        nodeDepth = 2;
                                        System.out.println("Extract: First Node Found is: " + nextLine[i]);
                                        firstNodePosition = i;
                                        break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                    }
                                }

                            }
                        } else {
                            for (int i = 0; i < nextLine.length; i++) {

                                if (nextLine[i].equals("Sample Name")) {
                                    nodeDepth = 1;
                                    System.out.println("Sample: First Node Found is: " + nextLine[i]);
                                    firstNodePosition = i;
                                    break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                }
                            }


                        }


                        if (getArrayAsString(columnNames).contains("Factor Name")) {

                            for (int i = 0; i < nextLine.length; i++) {

                                System.out.println(nextLine[i]);
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
                        System.out.println("SDRF lacks SAMPLE NAME header");
                    }
                    counter++;
                }
                printFiles(accnum);
            } else {
                System.out.println("ERROR: file not found!");
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * A method to deal with the study sample and remove duplicates row (i.e. replicated rows).
     *
     * @param sampleBlock
     */
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

    /**
     * A method to ..
     *
     * @param columnName
     * @return a set of indexes
     */
    private Set<Integer> getColumnIndexForName(String columnName) {
        Set<Integer> indexes = new HashSet<Integer>();

        for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {
            if (columnNames[columnIndex].equalsIgnoreCase(columnName)) {
                indexes.add(columnIndex);
            }
        }

        return indexes;
    }


    /**
     * @param columnIndexes
     * @return
     */
    private Map<Integer, List<String>> getValuesForColumns(Set<Integer> columnIndexes) {

        Map<Integer, List<String>> values = new HashMap<Integer, List<String>>();

        for (int rowIndex : rowValues.keySet()) {
            for (int columnIndex : columnIndexes) {
                if (!values.containsKey(columnIndex)) {
                    values.put(columnIndex, new ArrayList<String>());
                }
                values.get(columnIndex).add(rowValues.get(rowIndex)[columnIndex]);
            }
        }

        return values;
    }


    /**
     * A method to print Study sample table and assay table
     */
    public void printFiles(String accnum) {


        try {

            //todo finish outputting the column headers
            String studySampleHeaders = "";
            String studyAssayHeaders = "";

            //This test catches malformed MAGE-TAB files!
            if (firstNodePosition > 0) {

                System.out.println("study_sample position is: " + firstNodePosition);


                for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {


                    //now dealing with descriptors that will go on the ISA study sample sheet, i.e. everything before the first Material Node after Source Node
                    if (columnIndex < firstNodePosition) {

                        // we take care of the MAGE-TAB Description field which sometimes shows up in AE output
                        if (columnNames[columnIndex].equalsIgnoreCase("description")) {
                            columnNames[columnIndex] = "Comment[description]";
                        }


                        if (columnNames[columnIndex].equalsIgnoreCase("characteristics [organism]")) {
                            columnNames[columnIndex] = "Characteristics [organism]";
                        }
                        studySampleHeaders += columnNames[columnIndex] + "\t";
                    }

                    if (columnIndex == firstNodePosition)  {

                        if ( !(columnNames[firstNodePosition].equalsIgnoreCase("sample name")) ) {

                                studySampleHeaders += "Sample Name" + "\t";
                                studyAssayHeaders = "Sample Name" + "\t";
                        }

                    //TODO: warning -> need to know how to form header depending on technology i.e. hybridization or assay!
                    //TODO: as it might not require Labeled Name
    //                else if ( !(columnNames[firstNodePosition].equalsIgnoreCase("sample name")) && nodeDepth == 3 ) {
    //                    studyAssayHeaders = "Sample Name" + "\t" + "Extract Name" + "\t" + "Labeled Name" + "\t";
    //                }
                        else {
                            studyAssayHeaders = columnNames[firstNodePosition] + "\t";
                            studySampleHeaders += columnNames[columnIndex] + "\t";
                        }
                    }


                    //now dealing with descriptions going to the ISA assay spreadsheet
                    else if (columnIndex > firstNodePosition) {

                        if (columnNames[columnIndex].equalsIgnoreCase("Hybridization Name")) {
                            columnNames[columnIndex] = "Hybridization Assay Name";
                        } else if (columnNames[columnIndex].equalsIgnoreCase("Comment [FASTQ_URI]")) {
                            columnNames[columnIndex] = "Raw Data File";
                        } else if (columnNames[columnIndex].equalsIgnoreCase("Comment [Platform_title]")) {
                            columnNames[columnIndex] = "Parameter Value[sequencing instrument]";

                        } else if (columnNames[columnIndex].startsWith("FactorValue ")) {
                            columnNames[columnIndex]=columnNames[columnIndex].toLowerCase();
                            columnNames[columnIndex]=columnNames[columnIndex].replaceAll("factorvalue ","Factor Value");
                        }
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    }
                }

            }


            //TODO: this bit is confusing -> rewrite
            //this code is only executed when sequencing dataset are fed in


            else {

                System.out.println("REALLY?? Study Sample position is: " + firstNodePosition);

//                if (columnNames != null) {
//
//
//                    for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {
//
//
//                        if (columnIndex == 0) {
//                            studySampleHeaders += columnNames[columnIndex] + "\t";
//                        }
//
//
//                        else if ((columnIndex <= firstNodePosition - 1) && (columnIndex > 0)) {
//
//                            System.out.println("number is:" + columnIndex);
//
//                            if (columnNames[columnIndex].equalsIgnoreCase("description")) {
//                            columnNames[columnIndex] = "Comment[description]";
//                        }
//
//                            studySampleHeaders += columnNames[columnIndex] + "\t";
//                        }
//                        else if (columnIndex >= firstNodePosition) {
//
//                            //here, we align MAGE-TAB to ISA-Tab Assay Fields
//                            if (columnNames[columnIndex].equalsIgnoreCase("Hybridization Name")) {
//                                columnNames[columnIndex] = "Hybridization Assay Name";
//                                studyAssayHeaders += columnNames[columnIndex] + "\t";
//
//                            }
//                            else if (columnNames[columnIndex].equalsIgnoreCase("Assay Name")) {
//                                columnNames[columnIndex] = "Assay Name";
//                                studyAssayHeaders += columnNames[columnIndex] + "\t";
//                            }
//
//                            //here, we cast MAGE Comment field into ISA-Tab Raw Data File
//                            else if (columnNames[columnIndex].equalsIgnoreCase("Comment [FASTQ_URI]")) {
//                                columnNames[columnIndex] = "Raw Data File";
//                                studyAssayHeaders += columnNames[columnIndex] + "\t";
//                            }
//                            //here we deal with an ad-hoc field from MAGE-TAB only present when HTS is used
//                            else if ((columnNames[columnIndex].equalsIgnoreCase("technology type"))) {
//                                columnNames[columnIndex] = "Parameter Value[library layout]";
//                                studyAssayHeaders += columnNames[columnIndex] + "\t";
//                            }
//
//                            //here we recast a MAGE Comment into a more specific ISA-Tab entity
//                            else if (columnNames[columnIndex].equalsIgnoreCase("Comment [Platform_title]")) {
//                                columnNames[columnIndex] = "Parameter Value[sequencing instrument]";
//                                studyAssayHeaders += columnNames[columnIndex] + "\t";
//                            }
//
//
//                            //Here we deal with unnecessary Material Node by MAGE-TAB
//                            else if ( (columnNames[columnIndex].equalsIgnoreCase("Labeled Extract Name")) && (tt >=0)) {
//                                System.out.println("unnecessary Term Source REF found at " + columnIndex);
//                                columnNames[columnIndex] = "Protocol REF";
//                                studyAssayHeaders += columnNames[columnIndex] + "\t";
//                            }
//
//                            else if ((columnNames[columnIndex].equalsIgnoreCase("Label"))  && (tt >=0)) {
//
//                                columnNames[columnIndex] = "Parameter Value[immunoprecipitation antibody]";
//                                studyAssayHeaders += columnNames[columnIndex] + "\t";
//                            }
//
//
//
//
//                            else {
//                                studyAssayHeaders += columnNames[columnIndex] + "\t";
//                            }
//                        }
//                    }

                    studySampleHeaders = studySampleHeaders + "Sample Name";
                //}
            }


            //we print study_sample file

            PrintStream studyPs = new PrintStream(new File("data/" + accnum + "/s_" + accnum + "_studysample.txt"));
            System.out.println("data/" + accnum + "/s_" + accnum + "_studysample.txt");

            studyPs.println(studySampleHeaders);


            for (int hash : samples.keySet()) {
                if (getArrayAsString4Study(samples.get(hash)).length() > 0) {
                    studyPs.println(getArrayAsString4Study(samples.get(hash)));
                }
            }


            studyPs.flush();
            studyPs.close();


            //we print the assay file
            PrintStream assayPs = new PrintStream(new File("data/" + accnum + "/a_" + accnum + "_assay.txt"));

            assayPs.println(studyAssayHeaders);

            for (String[] assaySection : assays) {
                if (getArrayAsString(assaySection).length() > 0) {
                    assayPs.println(getArrayAsString(assaySection));
                }
            }


            assayPs.flush();
            assayPs.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
                System.out.println("found:" + val);
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

        // System.out.println("record:" + blockAsString);

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
                System.out.println("found in study:" + val);
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

        System.out.println("record:" + blockAsString);

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

