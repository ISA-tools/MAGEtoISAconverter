package org.isatools.magetoisatab.io;

import java.io.*;

import au.com.bytecode.opencsv.CSVReader;
import org.isatools.io.FileType;
import org.isatools.io.Loader;
import org.isatools.magetoisatab.utils.ProtocolREFUtil;

import javax.xml.bind.SchemaOutputResolver;
import java.util.List;
import java.util.*;
import java.lang.*;

import org.isatools.manipulator.SpreadsheetManipulation;


public class MAGETabSDRFLoader {

    public static final Character TAB_DELIM = '\t';

    public String[] columnNames;

    private int studySamplePosition;
    private int firstNodeIfNoSamplePosition;
    private int firstNodePosition;
    private int firstFactorPosition;
    private int nodeDepth;

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

                SpreadsheetManipulation manipulation = new SpreadsheetManipulation();
                String[] columnNames = manipulation.getColumnHeaders(sheetData);

                // initialization of the ArrayList which will receive all fields to be removed
                ArrayList<Integer> positions2keep = new ArrayList<Integer>();

                // now checking which fields need dropping and adding them to the ArrayList
                for (int columnIndex = 0; columnIndex < columnNames.length - 1; columnIndex++) {

                    // we don't this
                    if ((columnNames[columnIndex].equalsIgnoreCase("term source ref"))
                            && (columnNames[columnIndex - 1].equalsIgnoreCase("protocol ref"))) {
                        System.out.println("dodgy term source REF found at:" + columnIndex);


                    }
                    // but we keep the rest
                    else {
                        positions2keep.add(columnIndex);
                    }
                }


                // calling the getColumnSubset method and create a object containing the SDRF data bar all fields such as Term Source REF following a Protocol REF
                List<String[]> sheetDataSubset = manipulation.getColumnSubset(sheetData, true, convertIntegers(positions2keep));

                // now preparing to process the cleaned SDRF subset and remove all aberrant Protocol REF fields where applicable
                //we initialize
                ProtocolREFUtil util = new ProtocolREFUtil();

                //we perform the transformation using the processSpreadsheet method
                sheetDataSubset = util.processSpreadsheet(sheetDataSubset);
                // you can read each line separately!

                System.out.println("After processing, sheetDataSubset is of size " + sheetDataSubset.size());

                for (String[] columnValues : sheetDataSubset){
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

                                            if (nextLine[i].equals("Hybridization Name")) {
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

                    //we have locate the first Material Node by parsing the Header, we now handle lines of data
                    else if (studySamplePosition > 0 && counter > 0) {

                        //now that we know where the 'Sample Name' field is, we are splitting the table

                        rowValues.put(counter, nextLine);

                        sampleRecord = new String[studySamplePosition + 1];

                        if (nextLine.length - studySamplePosition > 0) {

                            assayRecord = new String[nextLine.length - studySamplePosition];


                            for (int j = 0; j < nextLine.length; j++) {

                                if (j < studySamplePosition) {
                                    sampleRecord[j] = nextLine[j];
                                } else if (j == studySamplePosition) {
                                    sampleRecord[j] = nextLine[j];
                                    assayRecord[j - studySamplePosition] = nextLine[j];
                                } else {
                                    assayRecord[j - studySamplePosition] = nextLine[j];
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

                            assayRecord = new String[nextLine.length - firstNodePosition + 2];


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

            if (studySamplePosition > 0) {
                //int k = 0;
                studyAssayHeaders = columnNames[studySamplePosition] + "\t";

                for (int c = 0; c < columnNames.length; c++) {

                    if (c <= studySamplePosition) {

                        studySampleHeaders += columnNames[c] + "\t";

                    } else if (c >= studySamplePosition) {

                        if (columnNames[c].equalsIgnoreCase("Hybridization Name")) {
                            columnNames[c] = "Hybridization Assay Name";
                        } else if (columnNames[c].equalsIgnoreCase("Comment [FASTQ_URI]")) {
                            columnNames[c] = "Raw Data File";
                        } else if (columnNames[c].equalsIgnoreCase("Comment [Platform_title]")) {
                            columnNames[c] = "Parameter Value[sequencing instrument]";
                        }

                        studyAssayHeaders += columnNames[c] + "\t";
                    }
                }
            } else {

                //int k = 0;
                studyAssayHeaders = "Sample Name" + "\t";

                if (columnNames != null) {
                    for (int c = 0; c < columnNames.length; c++) {
                        if (c == 0) {
                            studySampleHeaders += columnNames[c] + "\t";
                        } else if ((c <= firstNodePosition - 1) && (c > 0)) {

                            System.out.println("number is:" + c);
                            studySampleHeaders += columnNames[c] + "\t";
                        } else if (c >= firstNodePosition) {

                            //here, we align MAGE-TAB to ISA-Tab Assay Fields
                            if (columnNames[c].equalsIgnoreCase("Hybridization Name")) {
                                columnNames[c] = "Hybridization Assay Name";
                                studyAssayHeaders += columnNames[c] + "\t";
                            }
                            //here, we cast MAGE Comment field into ISA-Tab Raw Data File
                            if (columnNames[c].equalsIgnoreCase("Comment [FASTQ_URI]")) {
                                columnNames[c] = "Raw Data File";
                                studyAssayHeaders += columnNames[c] + "\t";
                            }

                            //here we recast a MAGE Comment into a more specific ISA-Tab entity
                            if (columnNames[c].equalsIgnoreCase("Comment [Platform_title]")) {
                                columnNames[c] = "Parameter Value[sequencing instrument]";
                            }


                            //Here we deal with unnecessary Material Node by MAGE-TAB
                            if ((columnNames[c].equalsIgnoreCase("Labeled Extract Name"))) {
                                System.out.println("unnecessary Term Source REF found at " + c);
                                columns2drop.add(c - firstNodePosition);
                                //k++;
                                c = c + 1;
                            }

                            if ((columnNames[c].equalsIgnoreCase("Label"))) {
/*                            System.out.println("unnecessary Term Source REF found at " + c);
                            columns2drop.add(c-firstNodePosition);
                            k++;
                            c=c+1;*/

                                columnNames[c] = "Parameter Value[immunoprecipitation antibody]";
                            }

                            //here we deal with an ad-hoc field from MAGE-TAB only present when HTS is used
                            if ((columnNames[c].equalsIgnoreCase("Technology Type"))) {
                                columnNames[c] = "Parameter Value[library layout]";
                            }

                            //here we are trying to deal with dodgy MAGE-TAB files  where Protocol REF fields are followed by Term Source REF fields
//                            if ((columnNames[c].equalsIgnoreCase("Term Source REF") & columnNames[c - 1].equalsIgnoreCase("Protocol REF"))) {
//                                System.out.println("unnecessary Term Source REF found at " + c);
//                                columns2drop.add(c - firstNodePosition);
//                                k++;
//                                c = c++;
//                            }

                            //studyAssayHeaders += columnNames[c] +  "\t";


                            else {
                                studyAssayHeaders += columnNames[c] + "\t";
                            }
                        }
                    }

                    studySampleHeaders = studySampleHeaders + "Sample Name";
                }
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

        for (int i = 0; i < array.length; i++) {

            // here we check which fields to drop
            if ((columns2drop.contains(val))) {
                System.out.println("found" + val);
                val++;
            }
            //otherwise we print
            else if (array[i] != null) {

                //provided the value is not equal to null, we append to the record
                blockAsString.append(array[i]);

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

        for (int i = 0; i < array.length; i++) {

            // here we check which fields to drop from output Study_Sample file
            if (columns2dropFromStudy.contains(val)) {
                System.out.println("found in study:" + val);
                val++;
            }
            //otherwise we print
            else if (array[i] != null) {

                //provided the value is not equal to null, we append to the record
                blockAsString.append(array[i]);

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


    public static int[] convertIntegers(ArrayList<Integer> integers) {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int index = 0; index < ret.length; index++) {
            ret[index] = iterator.next().intValue();
        }
        return ret;
    }


}

