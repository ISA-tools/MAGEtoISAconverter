package org.isatools.magetoisatab.io;

//TODO: propagate  factor values and cleanup

import com.sun.tools.javac.util.Pair;
import org.isatools.io.FileType;
import org.isatools.io.Loader;
import org.isatools.magetoisatab.io.fileprocessing.CollapseColumnUtil;
import org.isatools.magetoisatab.io.fileprocessing.RemoveDuplicateColumnUtil;
import org.isatools.magetoisatab.io.model.Assay;
import org.isatools.magetoisatab.io.model.Study;
import org.isatools.magetoisatab.utils.Column;
import org.isatools.magetoisatab.utils.ConversionProperties;
import org.isatools.magetoisatab.utils.Utils;
import org.isatools.manipulator.SpreadsheetManipulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;


public class MAGETabSDRFLoader {

    // private static final Logger log = Logger.getLogger(MAGETabSDRFLoader);

    public static final Character TAB_DELIM = '\t';

    private static final String PROTOCOL_REF = "Protocol REF";
    private static final String CHARACTERISTICS = "Characteristics[";

    public String[] columnNames;

    private int tt = -1;    //a flag to detect the presence of Technology Type Field in MAGE-TAB
    private int ptf = -1;   //a flag to detect the presence of Comment[platform] Field in MAGE-TAB

//    private int firstNodePosition = -1;
//    private int nodeDepth;

    private int studySamplePosition = 0;
    private int firstFactorPosition;
    private int firstNodeIfNoSamplePosition;
    private boolean isPresentSample = false;
    private boolean isPresentExtract = false;
    private boolean isPresentLE = false;
    private boolean isPresentHyb = false;
    private boolean isPresentAssay = false;
    private boolean isPresentNorm = false;
    private boolean isPresentDT = false;
    private boolean isPresentDerivedData = false;

    public Map<Integer, String[]> rowValues;

    public Map<Integer, String[]> samples;

    public Set<String[]> assaysFromGivenSDRF;

    //public ArrayList<int[]> ;
    List columns2drop = new ArrayList();
    List columns2dropFromStudy = new ArrayList();
    private HashMap<String, String[]> theOne;


    public MAGETabSDRFLoader() {

        samples = new HashMap<Integer, String[]>();

        assaysFromGivenSDRF = new HashSet<String[]>();
    }

    public Study loadsdrfTab(String url, String accnum) throws IOException {

        List<String[]> studySamplesFromThisSDRF = new ArrayList<String[]>();
        List<Assay> assaysFromThisSDRF = new ArrayList<Assay>();

        Study study = new Study(studySamplesFromThisSDRF, assaysFromThisSDRF);


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

                List<String[]> factorSheetData = new ArrayList<String[]>();
                List<Integer> factorPositions2Keep = new ArrayList<Integer>();
                factorPositions2Keep.add(0);

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
                        System.out.println("SDRF has 'technology type header");
                    }

                    if (columnNames[columnIndex].equalsIgnoreCase("comment [platform_title]")) {
                        ptf++;
                    }

                    if (columnNames[columnIndex].startsWith("Factor Value")) {

                        columnNames[columnIndex] = columnNames[columnIndex].toLowerCase();
                        columnNames[columnIndex] = columnNames[columnIndex].replaceAll("factor value ", "Factor Value");

                        System.out.println("There is a factor value at position: " + columnIndex);
                        factorPositions2Keep.add(columnIndex);
                    }
                }

                factorSheetData = SpreadsheetManipulation.getColumnSubset(sheetData, true, convertIntegers(factorPositions2Keep));


                sheetData = SpreadsheetManipulation.getColumnSubset(sheetData, true, convertIntegers(positions2keep));

                //getting the associated header row in order to perform identification of fields position prior to reordering
                columnNames = SpreadsheetManipulation.getColumnHeaders(sheetData);

                List<Column> columnOrders = Utils.createColumnOrderList(columnNames);

                //where does Assay Name field appear?
                int assayNameIndex = Utils.getIndexForValue("Assay Name", columnOrders);

                //where does Data Transformation Name field appear?
                int dtNameIndex = -1;
                dtNameIndex = Utils.getIndexForValue("Data Transformation Name", columnOrders);

                //where does Derived Array Data File field appear?
                int derivedArrayDataFileIndex = -1;
                derivedArrayDataFileIndex = Utils.getIndexForValue("Derived Array Data Matrix File", columnOrders);
                //System.out.println("dreDF: " + derivedArrayDataFileIndex);

                //scanning the header checking if it contains "Scan Name field"
                int scanNameIndex = Utils.getIndexForValue("Scan Name", columnOrders);
                //System.out.println("Scan found at: " + scanNameIndex);

                // if present, fetching and moving the technology type field
                if (tt >= 0) {
                    Column technology = columnOrders.remove(Utils.getIndexForValue("technology type", columnOrders));
                    columnOrders.add(assayNameIndex, technology);

                    if (derivedArrayDataFileIndex > 0 && scanNameIndex > 0) {
                        Column scanName = columnOrders.remove(Utils.getIndexForValue("Scan Name", columnOrders));
                        columnOrders.add(derivedArrayDataFileIndex - 1, scanName);
                    } else if (derivedArrayDataFileIndex == 0 && scanNameIndex > 0) {
                        Column scanName = columnOrders.remove(Utils.getIndexForValue("Scan Name", columnOrders));
                        columnOrders.add(assayNameIndex + 1, scanName);
                    }
                }

                //add a column header Data Transformation Name when initially absent from MAGE-TAB input but Derived Data Files are used
                if ((derivedArrayDataFileIndex > 0) && (dtNameIndex <= 0)) {
                    System.out.println("derived Data files found but DTNAME field missing");
                    Column derivedDataFile = columnOrders.remove(Utils.getIndexForValue("Derived Array Data Matrix File", columnOrders));
                    columnOrders.add(derivedArrayDataFileIndex - 1, derivedDataFile);
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
                CollapseColumnUtil util = new CollapseColumnUtil();

                //we perform the transformation using the processSpreadsheet method
                sheetDataSubset = util.processSpreadsheet(sheetDataSubset, PROTOCOL_REF);

                RemoveDuplicateColumnUtil removeDuplicateColumnUtil = new RemoveDuplicateColumnUtil();
                sheetDataSubset = removeDuplicateColumnUtil.processSpreadsheet(sheetDataSubset);

                // you can read each line separately!

                String[] sdrfHeaderRow = sheetDataSubset.get(0);

                Pair<Integer, Integer> sdrfKeyPositions;

                sdrfKeyPositions = processSdrfHeaderRow(sdrfHeaderRow);

                System.out.println("POSITIONS ARE: " + sdrfKeyPositions.fst + " AND " + sdrfKeyPositions.snd);

                Pair<List<String[]>, List<String[]>> studySplitTables = splitSdrfTable(sdrfKeyPositions, sheetDataSubset, factorSheetData);

                study.setStudySampleLevelInformation(studySplitTables.fst);

                study.setAssays(inspectSdrfAssay(studySplitTables.snd));

                assaysFromThisSDRF = inspectSdrfAssay(studySplitTables.snd);

                // we are now iterating through the different assays and printing them
                for (Assay anAssaysFromThisSDRF : assaysFromThisSDRF) {

                    for (String key : anAssaysFromThisSDRF.getAssayLevelInformation().keySet()) {

                        PrintStream assayPs = new PrintStream(new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/a_" + accnum + "_" + key + "_assay.txt"));

                        for (String[] records : anAssaysFromThisSDRF.getAssayLevelInformation().get(key)) {

                            String newAssayRecord = "";

                            for (String s : records) {
                                newAssayRecord += s + "\t";
                            }
                            assayPs.println(newAssayRecord);

                        }
                    }
                }
            } else {

                System.out.println("SDRF Processing: ERROR: file not found!");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return study;
    }

    private void addStudySample(String[] sampleBlock) {

        StringBuilder blockAsString = new StringBuilder();

        for (String value : sampleBlock) {
            blockAsString.append(value);
        }

        int hashValue = blockAsString.toString().hashCode();

        if (!samples.containsKey(hashValue)) {
            samples.put(hashValue, sampleBlock);
        }
    }

    /**
     * A method to create a new record.
     *
     * @param array columns2drop
     * @return a record as a string
     */
    private String getArrayAsString(String[] array) {

        StringBuilder blockAsString = new StringBuilder();
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


    public static int[] convertIntegers(List<Integer> integers) {
        int[] ret = new int[integers.size()];
        Iterator<Integer> iterator = integers.iterator();
        for (int index = 0; index < ret.length; index++) {
            ret[index] = iterator.next();
        }
        return ret;
    }


    /**
     * A Method of to process SRDF header
     * input is a SRDF file name
     * output is an list of fields, and an index where to split the SDRF
     */
    public Pair<Integer, Integer> processSdrfHeaderRow(String[] headerRow) {

        int firstNodePosition = -1;
        int nodeDepth = -1;

        this.columnNames = headerRow;

        System.out.println("header : " + Arrays.toString(this.columnNames));

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
        if (getArrayAsString(columnNames).contains("Normalization Name")) {
            isPresentNorm = true;
        }
        if (getArrayAsString(columnNames).contains("Data Transformation Name")) {
            isPresentDT = true;
        }
        if (getArrayAsString(columnNames).contains("Raw Data File")) {
            boolean presentRawData = true;
        }
        if ((getArrayAsString(columnNames).contains("Derived Array Data File") || (getArrayAsString(columnNames).contains("Derived Array Data Matrix File")))) {
            isPresentDerivedData = true;
        }

        if (!getArrayAsString(columnNames).contains("Sample Name")) {

            if (!getArrayAsString(columnNames).contains("Extract Name")) {

                if (!getArrayAsString(columnNames).contains("Labeled Extract Name")) {

                    if (!getArrayAsString(columnNames).contains("Hybridization Name")) {
                        firstNodePosition = 0;
                        // todo set variables
                        //break;
                    } else {
                        for (int i = 0; i < headerRow.length; i++) {

                            if ((headerRow[i].equals("Hybridization Name")) || (headerRow[i].equals("Assay Name"))) {
                                nodeDepth = 4;
                                System.out.println("SDRF Processing: Hyb: First Node Found is: " + headerRow[i]);
                                firstNodePosition = i;
                                break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                            }
                        }
                    }
                } else {

                    for (int i = 0; i < headerRow.length; i++) {

                        if (headerRow[i].equals("Labeled Extract Name")) {
                            nodeDepth = 3;
                            System.out.println("SDRF Processing: LE: First Node Found is: " + headerRow[i]);
                            firstNodePosition = i;
                            break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                        }
                    }

                }


            } else {

                for (int i = 0; i < headerRow.length; i++) {

                    if (headerRow[i].equals("Extract Name")) {
                        nodeDepth = 2;
                        System.out.println("SDRF Processing:Extract: First Node Found is: " + headerRow[i]);
                        firstNodePosition = i;
                        break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                    }
                }

            }
        } else {

            for (int i = 0; i < headerRow.length; i++) {

                if (headerRow[i].equals("Sample Name")) {
                    nodeDepth = 1;
                    System.out.println("SDRF Processing: Sample: First Node Found is: " + headerRow[i]);
                    firstNodePosition = i;
                    break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                }
            }
        }
        return new Pair<Integer, Integer>(firstNodePosition, nodeDepth);
    }


    /**
     * A Method of to process SRDF file and split input SDRF file into StudySamples table and List of Assay tables
     * input is a SRDF file name
     * output is a study object containing a StudySample hashmap and an ArrayList of Assays
     */
    public Pair<List<String[]>, List<String[]>> splitSdrfTable(Pair<Integer, Integer> indices, List<String[]> sheetDataSubset, List<String[]> factorSheetData) {

        List<String[]> sdrfStudySampleTable = new ArrayList<String[]>();
        List<String[]> sdrfAssayTable = new ArrayList<String[]>();

        //HashMap<String, ArrayList<String[]>> assaysFromThisSDRF = new HashMap<String, ArrayList<String[]>>();
        //Pair<ArrayList<String[]>, ArrayList<String[]>> srdfSplitTables = new Pair<ArrayList<String[]>, ArrayList<String[]>>(sdrfStudySampleTable, sdrfAssayTable);

        String studySampleHeaders = "";
        String studyAssayHeaders = "";

        Integer firstIndexNodePosition = indices.fst;
        System.out.println("FIRST NODE POSITION : " + firstIndexNodePosition);
        System.out.println("FIRST NODE  : " + columnNames[firstIndexNodePosition]);

        Integer secondIndexNodeDepth = indices.snd;

        int dataTransNameIndex;

        //This test catches malformed MAGE-TAB files (i.e MAGE-TAB files starting with things other than Source Name)
        if (firstIndexNodePosition > 0) {

            for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {

                //THIS CREATES THE ISA STUDY SAMPLE SHEET: now dealing with descriptors that will go on the ISA study sample sheet, i.e. everything before the first Material Node after Source Node
                if (columnIndex < firstIndexNodePosition) {
                    studySampleHeaders += columnNames[columnIndex] + "\t";
                }

                //dealing with the hinge position
                else if (columnIndex == firstIndexNodePosition) {

                    //this case deals with situation where the first node after Source Name is *not* Sample Name, we need therefore to create it
                    if (!columnNames[firstIndexNodePosition].equalsIgnoreCase("sample name")) {

                        studySampleHeaders += "Sample Name";
                        studyAssayHeaders = "Sample Name" + "\t" + columnNames[firstIndexNodePosition] + "\t";

                    }
                    // otherwise, this is easy, we just concatenate
                    else {

                        studySampleHeaders += columnNames[columnIndex] + "\t";
                        studyAssayHeaders = columnNames[columnIndex] + "\t";//+columnNames[firstIndexNodePosition] + "\t";

                    }
                }

                //THIS CREATES THE ISA ASSAY SAMPLE SHEETS:
                //now dealing with descriptions going to the ISA assay spreadsheet
                // we stop before the last fields as some MAGE-TAB files have overhanging/trailing tab characters at the end of the header
                else if ((columnIndex > firstIndexNodePosition) && (columnIndex < columnNames.length - 1)) {

                    if (columnNames[columnIndex].equalsIgnoreCase("Hybridization Name")) {
                        columnNames[columnIndex] = "Hybridization Assay Name";
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    } else if (columnNames[columnIndex].equalsIgnoreCase("Assay Name")) {
                        columnNames[columnIndex] = "Assay Name";
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    }

                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Labeled Extract Name field with Protocol REF corresponding to a library creation
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Labeled Extract Name")) && (tt >= 0)) {
                        columnNames[columnIndex] = PROTOCOL_REF;
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    }

                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Label field with Parameter Value[mid] , corresponding to multiplex barcodes
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Label")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Parameter Value[mid]";
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Technology Type field with Parameter Value[library layout]
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Technology Type")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Parameter Value[library layout]";
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Technology Type field with Parameter Value[platform title]
                    else if ((columnNames[columnIndex].equalsIgnoreCase("comment [instrument model]")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Parameter Value[sequencing instrument]";
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Labeled Extract Name field with Protocol REF
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Scan Name")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Data Transformation Name";
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    } else if (columnNames[columnIndex].equalsIgnoreCase("comment [FASTQ_URI]")) {
                        columnNames[columnIndex] = "Raw Data File";
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Derived Array Data File with Derived Data file
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Derived Array Data File")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Derived Data File";
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    }
                    //
                    else if ((isPresentDT = false) && ((columnNames[columnIndex].equals("Derived Array Data Matrix File")))) {

                        columnNames[columnIndex] = "Data Transformation Name\tDerived Data File";
                        dataTransNameIndex = columnIndex;
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    } else if (columnNames[columnIndex].startsWith("FactorValue ")) {
                        columnNames[columnIndex] = columnNames[columnIndex].toLowerCase();
                        columnNames[columnIndex] = columnNames[columnIndex].replaceAll("factorvalue ", "Factor Value");
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    } else if (columnNames[columnIndex].startsWith("Factor Value ")) {
                        columnNames[columnIndex] = columnNames[columnIndex].toLowerCase();
                        columnNames[columnIndex] = columnNames[columnIndex].replaceAll("factor value ", "Factor Value");
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    } else {
                        studyAssayHeaders += columnNames[columnIndex] + "\t";
                    }
                }

                // we are now dealing with the last field provided it is not a tab!
                else if (!(columnNames[columnIndex].contains("\t")) && (columnIndex == columnNames.length - 1)) {
                    studyAssayHeaders += columnNames[columnIndex];
                }
            }

            sdrfStudySampleTable.add(studySampleHeaders.split("\\t"));
            sdrfAssayTable.add(studyAssayHeaders.split("\\t"));
            System.out.println("Assay Header:" + Arrays.toString(sdrfAssayTable.get(0)));
        } else {

            System.out.println("SDRF Processing: REALLY?? Study Sample position is: " + firstIndexNodePosition);

            studySampleHeaders = studySampleHeaders + "Sample Name";

            sdrfStudySampleTable.add(studySampleHeaders.split("\\t"));
        }

        //we now drop the header row in order to only process data records
        sheetDataSubset.remove(0);

        for (String[] sdrfRecord : sheetDataSubset) {

            if (firstIndexNodePosition > 0 && (secondIndexNodeDepth == 0)) {

                String[] sampleRecord = new String[firstIndexNodePosition + 1];

                if (sdrfRecord.length - firstIndexNodePosition > 0) {

                    String[] assayRecord = new String[sdrfRecord.length - (firstIndexNodePosition + 1)];

                    for (int j = 0; j < sdrfRecord.length; j++) {

                        //we are pushing the data to the study sample table
                        if (j < firstIndexNodePosition) {
                            sampleRecord[j] = sdrfRecord[j];
                        }
                        //we are dealing with the hinge point, same information is pushed to study sample and assay table
                        else if (j == firstIndexNodePosition) {
                            sampleRecord[j] = sdrfRecord[j];
                            assayRecord[j - firstIndexNodePosition] = sdrfRecord[j];
                        }
                        //we are pushing the rest of the data to the assay table
                        else {
                            assayRecord[j - firstIndexNodePosition] = sdrfRecord[j];
                        }
                    }

                    sdrfAssayTable.add(assayRecord);

                }

                sdrfStudySampleTable.add(sampleRecord);

            }
            //  Sample Name is present  and Extract Name is the first Material Node
            else if (firstIndexNodePosition > 0 && (secondIndexNodeDepth == 1)) {   //&& counter > 0

                String[] sampleRecord = new String[firstIndexNodePosition + 1];

                if (sdrfRecord.length - firstIndexNodePosition > 0) {

                    String[] assayRecord = new String[sdrfRecord.length - (firstIndexNodePosition + 1) + 1];

                    for (int j = 0; j < sdrfRecord.length; j++) {

                        if (j < firstIndexNodePosition) {
                            sampleRecord[j] = sdrfRecord[j];
                        } else if (j == firstIndexNodePosition) {
                            sampleRecord[j] = sdrfRecord[j];
                            assayRecord[j - firstIndexNodePosition] = sdrfRecord[j];
                        } else {
                            assayRecord[j - firstIndexNodePosition] = sdrfRecord[j];
                        }
                    }

                    sdrfAssayTable.add(assayRecord);

                }

                sdrfStudySampleTable.add(sampleRecord);

            } else if (firstIndexNodePosition > 0 && secondIndexNodeDepth == 2) {

                String[] sampleRecord = new String[firstIndexNodePosition + 1];

                if (sdrfRecord.length - firstIndexNodePosition > 0) {

                    String[] assayRecord = new String[sdrfRecord.length - (firstIndexNodePosition + 1) + 1];

                    for (int j = 0; j < sdrfRecord.length; j++) {

                        if (j < firstIndexNodePosition) {
                            sampleRecord[j] = sdrfRecord[j];
                        } else if (j == firstIndexNodePosition) {
                            sampleRecord[j] = sdrfRecord[j];
                            assayRecord[j - firstIndexNodePosition] = sdrfRecord[j] + "\t" + sdrfRecord[j];
                        } else {
                            assayRecord[j - firstIndexNodePosition] = sdrfRecord[j];
                        }
                    }

                    sdrfAssayTable.add(assayRecord);
                }

                sdrfStudySampleTable.add(sampleRecord);

            } else if (firstIndexNodePosition > 0 && secondIndexNodeDepth == 3) {

                String[] sampleRecord = new String[firstIndexNodePosition + 1];

                if (sdrfRecord.length - firstIndexNodePosition > 0) {

                    String[] assayRecord = new String[sdrfRecord.length - (firstIndexNodePosition + 1) + 2];

                    for (int j = 0; j < sdrfRecord.length; j++) {

                        if (j < firstIndexNodePosition) {
                            sampleRecord[j] = sdrfRecord[j];
                        } else if (j == firstIndexNodePosition) {
                            sampleRecord[j] = sdrfRecord[j];
                            assayRecord[j - firstIndexNodePosition] = sdrfRecord[j] + "\t" + sdrfRecord[j] + "\t" + sdrfRecord[j];
                        } else {
                            assayRecord[j - firstIndexNodePosition] = sdrfRecord[j];
                        }
                    }

                    sdrfAssayTable.add(assayRecord);
                }

                sdrfStudySampleTable.add(sampleRecord);
            }


        }


        // THIS CODE SECTION IS MEANT TO RETROFIT ANY FACTOR VALUES TO THE STUDY SAMPLE SPREADSHEET
        // The underlying assumption is that the factors are the same, even in the case of multiple SDRF
        // TODO: be able to create a sparse matrix of experimental factor.
        List<String[]> sdrfStudySampleTableFactors = new ArrayList<String[]>();

        for (int rowIndex = 0; rowIndex < sdrfStudySampleTable.size(); rowIndex++) {
            //  we are now splicing the 2 records sections, that corresponding to the sample descriptions and that detailing the factor set
            if (sdrfStudySampleTable.get(rowIndex)[0].equalsIgnoreCase(factorSheetData.get(rowIndex)[0])) {

                String[] tempRecord = sdrfStudySampleTable.get(rowIndex);

                String[] tempFactorRecord = factorSheetData.get(rowIndex);

                String[] newRecord = new String[tempRecord.length + tempFactorRecord.length - 1];

                System.arraycopy(tempRecord, 0, newRecord, 0, tempRecord.length);

                System.arraycopy(tempFactorRecord, 1, newRecord, tempRecord.length, tempFactorRecord.length - 1);

                sdrfStudySampleTableFactors.add(newRecord);
            }
        }
        return new Pair<List<String[]>, List<String[]>>(sdrfStudySampleTableFactors, sdrfAssayTable);
    }

    /*
      A Method inspecting the SDRF assay table resulting from splitting an SDRF and identifying the potential to split
      further into several assay in case SDRF contains more than one assay type.
      The method returns a HashMap where the key as assay type and the values are ArrayList of assay records.
    */
    private List<Assay> inspectSdrfAssay(List<String[]> sdrfAssayTableAsInput) {

        //a data structure to hold the different assay types found when iterating over the sdrf assay sheet
        List<Assay> assaysFromGivenSDRF = new ArrayList<Assay>();

        Assay chipSeqAssay = new Assay();
        Assay geneChipAssay = new Assay();
        Assay meSeqAssay = new Assay();
        Assay TFSeqAssay = new Assay();
        Assay rnaSeqAssay = new Assay();

        List<String[]> chipSeqRecords = new ArrayList<String[]>();
        chipSeqRecords.add(sdrfAssayTableAsInput.get(0));

        List<String[]> rnaSeqRecords = new ArrayList<String[]>();
        rnaSeqRecords.add(sdrfAssayTableAsInput.get(0));

        List<String[]> meSeqRecords = new ArrayList<String[]>();
        meSeqRecords.add(sdrfAssayTableAsInput.get(0));

        List<String[]> tfSeqRecords = new ArrayList<String[]>();
        tfSeqRecords.add(sdrfAssayTableAsInput.get(0));

        List<String[]> genechipRecords = new ArrayList<String[]>();
        genechipRecords.add(sdrfAssayTableAsInput.get(0));

        Set<String> aTypeUnique = new HashSet<String>();

        for (int rowIndex = 1; rowIndex < sdrfAssayTableAsInput.size(); rowIndex++) {

            String[] thisAssayRecord = sdrfAssayTableAsInput.get(rowIndex);
            // This is only checking the contents of the assay file itself for the type of experiment. We should also
            // rely on the study design type in the investigation file.

            if (getArrayAsString(thisAssayRecord).toLowerCase().contains("chip-seq") || ConversionProperties.isValueInDesignTypes("chip-seq")) {
                aTypeUnique.add("ChIP-Seq");
                chipSeqRecords.add(thisAssayRecord);
            }

            if (getArrayAsString(thisAssayRecord).contains("RNA-Seq")) {
                aTypeUnique.add("RNA-Seq");
                rnaSeqRecords.add(thisAssayRecord);
            }

            if ((getArrayAsString(thisAssayRecord).contains("Bisulfite-Seq")) || (getArrayAsString(thisAssayRecord).contains("MRE-Seq"))
                    || (getArrayAsString(thisAssayRecord).contains("MBD-Seq")) || (getArrayAsString(thisAssayRecord).contains("MeDIP-Seq "))) {

                aTypeUnique.add("ME-Seq");
                meSeqRecords.add(thisAssayRecord);
            }
            if ((getArrayAsString(thisAssayRecord).contains("DNase-Hypersensitivity")) || (getArrayAsString(thisAssayRecord).contains("MNase-Seq"))) {
                aTypeUnique.add("Chromatin-Seq");
                tfSeqRecords.add(thisAssayRecord);

            }
            if ((getArrayAsString(thisAssayRecord).contains("Hybridization")) || (getArrayAsString(thisAssayRecord).contains("biotin"))) {
                aTypeUnique.add("Hybridization");
                genechipRecords.add(thisAssayRecord);
            }
        }

        System.out.println("Assay type size " + aTypeUnique.size());
        for (String assaytype : aTypeUnique) {
            if (assaytype.contains("Hybridization")) {
                Map<String, List<String[]>> theOne = new HashMap<String, List<String[]>>();
                theOne.put("GeneChip", genechipRecords);
                geneChipAssay.setAssayLevelInformation(theOne);
                assaysFromGivenSDRF.add(geneChipAssay);

            }
            if (assaytype.contains("transcription profiling by array")) {
                Map<String, List<String[]>> theOne = new HashMap<String, List<String[]>>();
                theOne.put("genechip", genechipRecords);
                geneChipAssay.setAssayLevelInformation(theOne);
                assaysFromGivenSDRF.add(geneChipAssay);
            }
            if (assaytype.contains("ChIP-Seq")) {
                Map<String, List<String[]>> theOne = new HashMap<String, List<String[]>>();
                theOne.put("ChIP-Seq", chipSeqRecords);
                chipSeqAssay.setAssayLevelInformation(theOne);
                assaysFromGivenSDRF.add(chipSeqAssay);

            }
            if (assaytype.contains("RNA-Seq")) {
                Map<String, List<String[]>> theOne = new HashMap<String, List<String[]>>();
                theOne.put("RNA-Seq", rnaSeqRecords);
                rnaSeqAssay.setAssayLevelInformation(theOne);
                assaysFromGivenSDRF.add(rnaSeqAssay);
            }
            if (assaytype.contains("ME-Seq")) {
                Map<String, List<String[]>> theOne = new HashMap<String, List<String[]>>();
                theOne.put("ME-Seq", meSeqRecords);
                meSeqAssay.setAssayLevelInformation(theOne);
                assaysFromGivenSDRF.add(meSeqAssay);
            }
            if (assaytype.contains("Chromatin-Seq")) {
                Map<String, List<String[]>> theOne = new HashMap<String, List<String[]>>();
                theOne.put("Chromatin-Seq", tfSeqRecords);
                TFSeqAssay.setAssayLevelInformation(theOne);
                assaysFromGivenSDRF.add(TFSeqAssay);
            }
        }
        //TODO: this is inefficient! assigment should occur earlier


        //We now output an assay spreadsheet for each assay type identified when scanning through the SDRF
        //TODO: if there is a mismatch between assayCountinIDF and assayCountinSDRF, need to retrofit assaysFromGivenSDRF in IDF

        System.out.println("There are " + assaysFromGivenSDRF.size() + " assays in this SDRF.");

        return new ArrayList<Assay>(assaysFromGivenSDRF);

    }

}