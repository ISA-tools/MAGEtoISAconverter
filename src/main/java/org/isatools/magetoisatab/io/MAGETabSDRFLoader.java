package org.isatools.magetoisatab.io;

//TODO: propagate  factor values and cleanup

import com.sun.tools.javac.util.Pair;
import org.isatools.io.FileType;
import org.isatools.io.Loader;
import org.isatools.magetoisatab.io.fileprocessing.CleanupRunner;
import org.isatools.magetoisatab.io.fileprocessing.ColumnMoveUtil;
import org.isatools.magetoisatab.io.fileprocessing.ProtocolInsertionUtil;
import org.isatools.magetoisatab.io.model.Assay;
import org.isatools.magetoisatab.io.model.AssayType;
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

    private boolean isPresentSample = false;
    private boolean isPresentExtract = false;
    private boolean isPresentLE = false;
    private boolean isPresentHyb = false;
    private boolean isPresentAssay = false;
    private boolean isPresentNorm = false;
    private boolean isPresentRawData = false;
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

    public Study loadsdrfTab(String url, String accnum, List<AssayType> assayTTMT) throws IOException {

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

                List<String[]> factorSheetData;
                List<Integer> factorPositions2Keep = new ArrayList<Integer>();
                factorPositions2Keep.add(0);

                // now checking which fields need dropping and adding them to the ArrayList
                // This takes care of incorrect MAGE-TAB files where Protocol REF and Array Design REF are followed by Term Source REF
                for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {

                    if (!columnNames[columnIndex].trim().equals("")) {

                        if (!(columnNames[columnIndex].equalsIgnoreCase("term source ref") && columnNames[columnIndex - 1].equalsIgnoreCase("protocol ref"))
                                &&
                                !(columnNames[columnIndex].equalsIgnoreCase("term source ref") && columnNames[columnIndex - 1].equalsIgnoreCase("array design ref"))
                                ) {
                            positions2keep.add(columnIndex);
                        }
                    }

                    if (columnNames[columnIndex].equalsIgnoreCase("technology type")) {
                        tt = 1;
                        System.out.println("SDRF has 'technology type' header");
                    }

                    if (columnNames[columnIndex].equalsIgnoreCase("comment [platform_title]")) {
                        ptf++;
                    }

                    if (columnNames[columnIndex].startsWith("Factor Value")) {
                        columnNames[columnIndex] = columnNames[columnIndex].toLowerCase();
                        columnNames[columnIndex] = columnNames[columnIndex].replaceAll("factor value ", "Factor Value");
                        columnNames[columnIndex] = columnNames[columnIndex].replace(". #", "#").replace("#", " number");
                        factorPositions2Keep.add(columnIndex);
                    }
                }

                factorSheetData = SpreadsheetManipulation.getColumnSubset(sheetData, true, convertIntegers(factorPositions2Keep));
                sheetData = SpreadsheetManipulation.getColumnSubset(sheetData, true, convertIntegers(positions2keep));

                //getting the associated header row in order to perform identification of field positions prior to reordering
                columnNames = SpreadsheetManipulation.getColumnHeaders(sheetData);

                List<Column> columnOrders = Utils.createColumnOrderList(columnNames);

                //where does Assay Name field appear?
                int assayNameIndex = Utils.getIndexForValue("Assay Name", columnOrders);
                System.out.println("Assay Name field found at: " + assayNameIndex);

                //where does Data Transformation Name field appear?
                int dtNameIndex = -1;
                dtNameIndex = Utils.getIndexForValue("Data Transformation Name", columnOrders);

                //where does Derived Array Data File field appear?
                int derivedArrayDataFileIndex = -1;
                derivedArrayDataFileIndex = Utils.getIndexForValue("Derived Array Data Matrix File", columnOrders);

                //scanning the header checking if it contains "Scan Name field"
                int scanNameIndex = Utils.getIndexForValue("Scan Name", columnOrders);

                // if present, fetching and moving the technology type field  next to Assay Name
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

                //add a column header Data Transformation Name when absent from MAGE-TAB input but Derived Data Files are found
                if ((derivedArrayDataFileIndex > 0) && (dtNameIndex <= 0)) {
                    //System.out.println("Derived Data files found but DTNAME field missing");
                    Column derivedDataFile = columnOrders.remove(Utils.getIndexForValue("Derived Array Data Matrix File", columnOrders));
                    columnOrders.add(derivedArrayDataFileIndex - 1, derivedDataFile);
                }

                //fetching and moving the platform title field if present
                if (ptf >= 0) {
                    Column platformTitle = columnOrders.remove(Utils.getIndexForValue("comment [platform_title]", columnOrders));
                    columnOrders.add(assayNameIndex + 1, platformTitle);
                }

                // calling the getColumnSubset method and create a object containing the SDRF data minus all fields such as Term Source REF following a Protocol REF
                List<String[]> sheetDataSubset = SpreadsheetManipulation.getColumnSubset(sheetData, true, Utils.createIndexArray(columnOrders));

                //we perform the transformation using the processSpreadsheet method
                sheetDataSubset = CleanupRunner.runAll(sheetDataSubset);
                String[] sdrfHeaderRow = sheetDataSubset.get(0);
                Pair<Integer, Integer> sdrfKeyPositions;
                sdrfKeyPositions = processSdrfHeaderRow(sdrfHeaderRow);

                System.out.println("POSITIONS ARE: " + sdrfKeyPositions.fst + " AND " + sdrfKeyPositions.snd);

                Pair<List<String[]>, List<String[]>> studySplitTables = splitSdrfTable(sdrfKeyPositions, sheetDataSubset, factorSheetData);
                study.setStudySampleLevelInformation(studySplitTables.fst);
                assaysFromThisSDRF = inspectSdrfAssay(studySplitTables.snd, assayTTMT);
                study.setAssays(assaysFromThisSDRF);

                //We are now iterating through the different assays and printing them
                System.out.println("We have " + assaysFromThisSDRF.size() + " assays.");
                for (Assay anAssaysFromThisSDRF : assaysFromThisSDRF) {
                    for (String key : anAssaysFromThisSDRF.getAssayLevelInformation().keySet()) {
                        List<String[]> assaySpreadsheet = anAssaysFromThisSDRF.getAssayLevelInformation().get(key);
                        assaySpreadsheet = CleanupRunner.runSelected(assaySpreadsheet, new ColumnMoveUtil(),new ProtocolInsertionUtil());  // ,
                        PrintStream assayPs = new PrintStream(new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/a_" + accnum + "_" + key + "_assay.txt"));
                        for (String[] records : assaySpreadsheet) {
                            String newAssayRecord = "";
                            int count = 0;
                            for (String s : records) {
                                newAssayRecord += s;
                                if (count != records.length - 1) {   //avoids adding a tab right before the line break and after last value
                                    newAssayRecord += TAB_DELIM;
                                }
                                count++;
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
        }
        return study;
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
                    blockAsString.append(TAB_DELIM);
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
     *  @return a pair of integers indicating the position of the first Material Node and its depth
     */
    public Pair<Integer, Integer> processSdrfHeaderRow(String[] headerRow) {

        int firstNodePosition = -1;
        int nodeDepth = -1;

        this.columnNames = headerRow;

        //System.out.println("header : " + Arrays.toString(this.columnNames));

        String columnNamesAsString = getArrayAsString(columnNames);

        if (columnNamesAsString.contains("Sample Name")) {
            isPresentSample = true;
        }
        if (columnNamesAsString.contains("Extract Name")) {
            isPresentExtract = true;
        }
        if (columnNamesAsString.contains("Labeled Extract Name")) {
            isPresentLE = true;
        }
        if (columnNamesAsString.contains("Hybridization Name")) {
            isPresentHyb = true;
        }
        if (columnNamesAsString.contains("Assay Name")) {
            isPresentHyb = true;
        }
        if (columnNamesAsString.contains("Normalization Name")) {
            isPresentNorm = true;
        }
        if (columnNamesAsString.contains("Data Transformation Name")) {
            isPresentDT = true;
        }
        if (columnNamesAsString.contains("Raw Data File")) {
            isPresentRawData = true;
        }
        if ((columnNamesAsString.contains("Derived Array Data File") || (columnNamesAsString.contains("Derived Array Data Matrix File")))) {
            isPresentDerivedData = true;
        }

        if (!columnNamesAsString.contains("Sample Name")) {

            if (!columnNamesAsString.contains("Extract Name")) {

                if (!columnNamesAsString.contains("Labeled Extract Name")) {

                    if (!columnNamesAsString.contains("Hybridization Name")) {
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

        String studySampleHeaders = "";
        String studyAssayHeaders = "";

        Integer firstIndexNodePosition = indices.fst;
        System.out.println("FIRST NODE POSITION : " + firstIndexNodePosition);
        System.out.println("FIRST NODE  : " + columnNames[firstIndexNodePosition]);

        Integer secondIndexNodeDepth = indices.snd;

        int dataTransNameIndex;
        int instrumentIndex = -1;
        int assayNamePosition = -1;

        //This test catches malformed MAGE-TAB files (i.e MAGE-TAB files starting with things other than Source Name)
        if (firstIndexNodePosition > 0) {

            for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {

                //THIS CREATES THE ISA STUDY SAMPLE SHEET:
                // now dealing with descriptors that will go on the ISA study sample sheet, i.e. everything before the first Material Node after Source Node
                if (columnIndex < firstIndexNodePosition) {
                    columnNames[columnIndex] = columnNames[columnIndex].replace(". #", "#").replace("#", " number");
                    studySampleHeaders += columnNames[columnIndex] + TAB_DELIM;
                }

                //dealing with the hinge position
                else if (columnIndex == firstIndexNodePosition) {
                    //this case deals with situation where the first node after Source Name is *not* Sample Name, we need therefore to create it
                    if (!columnNames[firstIndexNodePosition].equalsIgnoreCase("sample name")) {
                        studySampleHeaders += "Sample Name";
                        studyAssayHeaders = "Sample Name" + TAB_DELIM + columnNames[firstIndexNodePosition] + TAB_DELIM;
                    }
                    // otherwise, this is easy, we just concatenate
                    else {
                        studySampleHeaders += columnNames[columnIndex] + TAB_DELIM;
                        studyAssayHeaders = columnNames[columnIndex] + TAB_DELIM;//+columnNames[firstIndexNodePosition] + TAB_DELIM;
                    }
                }

                //THIS CREATES THE ISA ASSAY SAMPLE SHEETS:
                //now dealing with descriptions going to the ISA assay spreadsheet
                // we stop before the last fields as some MAGE-TAB files have overhanging/trailing tab characters at the end of the header
                else if ((columnIndex > firstIndexNodePosition) && (columnIndex < columnNames.length)) {

                    if (columnNames[columnIndex].equalsIgnoreCase("Hybridization Name")) {
                        columnNames[columnIndex] = "Hybridization Assay Name";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    } else if (columnNames[columnIndex].equalsIgnoreCase("Assay Name") && tt >= 0) {

                        assayNamePosition = columnIndex;
                        System.out.println("TECHTYPE: " + tt + assayNamePosition);
                        //columnNames[columnIndex] = "Labeled Extract Name";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }

                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Labeled Extract Name field with Protocol REF corresponding to a library creation
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Labeled Extract Name")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Comment[library name]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                        System.out.println("replacing LEN with PROTOCOL_REF");
                    }

                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Label field with Parameter Value[mid] , corresponding to multiplex barcodes
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Material Type")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Comment[material]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                        System.out.println("replacing MT with C[m]");
                    }

                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Label field with Parameter Value[mid] , corresponding to multiplex barcodes
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Label")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Parameter Value[library mid]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Technology Type field with Parameter Value[library layout]
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Technology Type")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Comment[technology type]";
                        assayNamePosition = columnIndex - 1;
                        System.out.println("Assay Name found at: " + assayNamePosition);
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Technology Type field with Parameter Value[sequencing instrument]
                    else if ((columnNames[columnIndex].equalsIgnoreCase("comment [instrument model]") || (columnNames[columnIndex].equalsIgnoreCase("comment [instrument_model]")) && (tt >= 0))) {
                        columnNames[columnIndex] = "Parameter Value[sequencing instrument]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                        instrumentIndex = columnIndex;
                        System.out.println("INSTRUMENT MODEL: " + instrumentIndex);
                    } else if (!(instrumentIndex > 0) && ((columnNames[columnIndex].equalsIgnoreCase("comment [platform_title]") || (columnNames[columnIndex].equalsIgnoreCase("comment [platform title]"))))) { //&& (instrumentIndex>0)

                        System.out.println("PLATFORM title INDEX before: " + instrumentIndex);
                        columnNames[columnIndex] = "Parameter Value[sequencing instrument]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                        instrumentIndex = columnIndex;
                        System.out.println("PLATFORM title INDEX: " + instrumentIndex);
                    } else if ((columnNames[columnIndex].equalsIgnoreCase("comment [platform_title]") || (columnNames[columnIndex].equalsIgnoreCase("comment [platform title]")) && (instrumentIndex < 0))) {
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }

                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Technology Type field with Parameter Value[library_source]
                    else if ((columnNames[columnIndex].equalsIgnoreCase("comment [library_source]")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Parameter Value[library_source]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Technology Type field with Parameter Value[library_selection]
                    else if ((columnNames[columnIndex].equalsIgnoreCase("comment [library_selection]")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Parameter Value[library selection]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Technology Type field with Parameter Value[library_strategy]
                    else if ((columnNames[columnIndex].equalsIgnoreCase("comment [library_strategy]")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Parameter Value[library_strategy]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Technology Type field with Parameter Value[library_strategy]
                    else if ((columnNames[columnIndex].equalsIgnoreCase("comment [library_layout]")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Parameter Value[library_layout]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }

                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Scan Name field with Assay Name (mapped to SRA Run,which are technical replicates, and SRAExperiment matches a library
                    else if (columnNames[columnIndex].equalsIgnoreCase("Scan Name") && (tt >= 0) && assayNamePosition < 0) {
                        System.out.println("Scan Name Found, assay index is: " + assayNamePosition);
                        columnNames[columnIndex] = "Assay Name";
                        assayNamePosition = columnIndex;
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;

                    }
                    //we have already found an Assay and technology is sequencing, therefore we rename Scan Name Field to a Comment
                    else if (columnNames[columnIndex].equalsIgnoreCase("Scan Name") && (tt >= 0) && assayNamePosition > 0) {
                        System.out.println("Scan Name Found, assay index is: " + assayNamePosition);
                        columnNames[columnIndex] = "Comment[Assay Name]";
                        assayNamePosition = columnIndex;
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;

                    }
                    //we have not yet found an Assay Name field in Mage-Tab and technology is sequencing, we therefore fall back on ENA_EXPERIMENT comment to create an Assay
                    else if ((columnNames[columnIndex].equalsIgnoreCase("comment [ENA_EXPERIMENT]")) && (tt >= 0) && assayNamePosition < 0) {
                        System.out.println("ENA_Experiment Found assay index is: " + assayNamePosition);
                        columnNames[columnIndex] = "Assay Name";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    } else if ((columnNames[columnIndex].equalsIgnoreCase("comment [ENA_RUN]")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Parameter Value[run identifier]";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;

                    }
                    //if we have not found any Raw Data File before and come across a FASTQ_URI comment field, we cast it as a Raw Data File
                    else if (columnNames[columnIndex].equalsIgnoreCase("comment [FASTQ_URI]")) {
                        columnNames[columnIndex] = "Raw Data File";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }
                    //Here in case Sequencing is used (tt>=0) we replace MAGE-TAB Derived Array Data File with Derived Data file
                    else if ((columnNames[columnIndex].equalsIgnoreCase("Derived Array Data File")) && (tt >= 0)) {
                        columnNames[columnIndex] = "Derived Data File";
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }
                    //
                    else if ((isPresentDT = false) && ((columnNames[columnIndex].equals("Derived Array Data Matrix File")))) {
                        columnNames[columnIndex] = "Data Transformation Name\tDerived Data File";
                        dataTransNameIndex = columnIndex;
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    } else if (columnNames[columnIndex].startsWith("Factor Value ")) {
                        columnNames[columnIndex] = columnNames[columnIndex].toLowerCase();
                        columnNames[columnIndex] = columnNames[columnIndex].replaceAll("factor\\s*value ", "Factor Value");
                        columnNames[columnIndex] = columnNames[columnIndex].replace(". #", "#").replace("#", " number");
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    } else if (columnNames[columnIndex].contains("#")) {
                        columnNames[columnIndex] = columnNames[columnIndex].replace(". #", "#").replace("#", " number");
                    } else {
                        studyAssayHeaders += columnNames[columnIndex] + TAB_DELIM;
                    }
                }

                // we are now dealing with the last field provided it is not a tab!
                else if (!(columnNames[columnIndex].contains("" + TAB_DELIM)) && (columnIndex == columnNames.length - 1)) {
                    studyAssayHeaders += columnNames[columnIndex];
                }
            }

            sdrfStudySampleTable.add(studySampleHeaders.split("\\t"));
            sdrfAssayTable.add(studyAssayHeaders.split("\\t"));
            System.out.println("Assay Header:" + Arrays.toString(sdrfAssayTable.get(0)));
        } else {
            studySampleHeaders = studySampleHeaders + "Sample Name";
            sdrfStudySampleTable.add(studySampleHeaders.split("\\t"));
        }

        //we now drop the header row in order to only process data records
        sheetDataSubset.remove(0);

        for (String[] sdrfRecord : sheetDataSubset) {

            if (firstIndexNodePosition > 0 && (secondIndexNodeDepth == 0)) {
                insertMissingNode(sdrfStudySampleTable, sdrfAssayTable, firstIndexNodePosition, sdrfRecord, 1);
            } else if (firstIndexNodePosition > 0 && (secondIndexNodeDepth == 1)) {   //&& counter > 0
                //  Sample Name is present  and Extract Name is the first Material Node
                insertMissingNode(sdrfStudySampleTable, sdrfAssayTable, firstIndexNodePosition, sdrfRecord, secondIndexNodeDepth);
            } else if (firstIndexNodePosition > 0 && secondIndexNodeDepth == 2) {
                insertMissingNode(sdrfStudySampleTable, sdrfAssayTable, firstIndexNodePosition, sdrfRecord, secondIndexNodeDepth);
            } else if (firstIndexNodePosition > 0 && secondIndexNodeDepth == 3) {
                insertMissingNode(sdrfStudySampleTable, sdrfAssayTable, firstIndexNodePosition, sdrfRecord, secondIndexNodeDepth);
            }
        }

        // THIS CODE SECTION IS MEANT TO RETROFIT ANY FACTOR VALUES TO THE STUDY SAMPLE SPREADSHEET
        // The underlying assumption is that the factors are the same, even in the case of multiple SDRF

        List<String[]> sdrfStudySampleTableCumFactors = new ArrayList<String[]>();

        for (int k = 0; k < sdrfStudySampleTable.size(); k++) {
            //  we are now splicing the 2 records sections, that corresponding to the sample descriptions and that detailing the factor set
            if (sdrfStudySampleTable.get(k)[0].equalsIgnoreCase(factorSheetData.get(k)[0])) {

                String[] tempRecord = sdrfStudySampleTable.get(k);

                String[] tempFactorRecord = factorSheetData.get(k);

                String[] newRecord = new String[tempRecord.length + tempFactorRecord.length - 1];

                System.arraycopy(tempRecord, 0, newRecord, 0, tempRecord.length);

                System.arraycopy(tempFactorRecord, 1, newRecord, tempRecord.length, tempFactorRecord.length - 1);

                sdrfStudySampleTableCumFactors.add(newRecord);
            }
        }
        return new Pair<List<String[]>, List<String[]>>(sdrfStudySampleTableCumFactors, sdrfAssayTable);
    }

    private void insertMissingNode(List<String[]> sdrfStudySampleTable, List<String[]> sdrfAssayTable, Integer firstAssayNodeIndex, String[] sdrfRecord, int secondIndexNodeDepth) {
        List<String> sampleRecord = new ArrayList<String>();

        int offset = 0;
        if (sdrfRecord.length - firstAssayNodeIndex > 0) {
            List<String> assayRecord = new ArrayList<String>();
            for (int sdrfColumnIndex = 0; sdrfColumnIndex < sdrfRecord.length; sdrfColumnIndex++) {
                if (sdrfColumnIndex < firstAssayNodeIndex) {
                    sampleRecord.add(sdrfColumnIndex, sdrfRecord[sdrfColumnIndex]);
                } else if (sdrfColumnIndex == firstAssayNodeIndex) {
                    sampleRecord.add(sdrfColumnIndex, sdrfRecord[sdrfColumnIndex]);
                    offset = secondIndexNodeDepth -1;
                    for (int insertionCount = 0; insertionCount < secondIndexNodeDepth; insertionCount++) {
                        assayRecord.add((sdrfColumnIndex - firstAssayNodeIndex) + insertionCount, sdrfRecord[sdrfColumnIndex]);
                    }
                } else {
                    assayRecord.add((sdrfColumnIndex - firstAssayNodeIndex) + offset, sdrfRecord[sdrfColumnIndex]);
                }
            }

            sdrfAssayTable.add(assayRecord.toArray(new String[assayRecord.size()]));
        }
        sdrfStudySampleTable.add(sampleRecord.toArray(new String[sampleRecord.size()]));
    }

    /*
   A Method inspecting the SDRF assay table resulting from splitting an SDRF and identifying the potential to split further into several asssay
   in case SDRF contains more than one assay type.
   The method returns a HashMap where the key as assay type and the values are ArrayList of assay records.
    */
    private List<Assay> inspectSdrfAssay(List<String[]> sdrfAssayTableAsInput, List<AssayType> assayTTMT) {

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

        String[] columnHeaders = sdrfAssayTableAsInput.get(0);

        boolean isHybridizationAssay = false;
        for (String columnHeader : columnHeaders) {
            if (columnHeader.contains("Hybridization")) {
                isHybridizationAssay = true;
            }
        }

        
        List<AssayType> assayTypes = assayTTMT;

        for (int i =0 ; i<assayTTMT.size(); i++) {
            System.out.println("ZIS IS ZE CONTENT: " +  assayTTMT.get(i).getMeasurement() + " - " +  assayTTMT.get(i).getTechnology());
        }

        for (int rowIndex = 1; rowIndex < sdrfAssayTableAsInput.size(); rowIndex++) {

            String[] thisAssayRecord = sdrfAssayTableAsInput.get(rowIndex);
            String arrayAsString = getArrayAsString(thisAssayRecord);


            
            
                //TODO: implement an method to iterate over all
            if  (
                    (assayTTMT.get(0).getMeasurement().equals("protein-DNA binding site identification") &&  assayTTMT.get(0).getTechnology().contains("sequencing"))
                    ||
                            (assayTTMT.get(1).getMeasurement().equals("protein-DNA binding site identification") &&  assayTTMT.get(1).getTechnology().contains("sequencing"))
                    ||
                            (assayTTMT.get(2).getMeasurement().equals("protein-DNA binding site identification") &&  assayTTMT.get(2).getTechnology().contains("sequencing"))
                            ||
                            (assayTTMT.get(3).getMeasurement().equals("protein-DNA binding site identification") &&  assayTTMT.get(3).getTechnology().contains("sequencing"))
                    ) {
                System.out.println("REALLY? "+ arrayAsString );
                if ( arrayAsString.contains("ChIP-Seq") || arrayAsString.contains("ChIPSeq") ) {
                aTypeUnique.add("ChIP-Seq");
                chipSeqRecords.add(thisAssayRecord);
                }
                if (arrayAsString.contains("Bisulfite-Seq") || arrayAsString.contains("MRE-Seq") ||
                        arrayAsString.contains("MBD-Seq") || arrayAsString.contains("MeDIP-Seq ")) {
                    aTypeUnique.add("ME-Seq");
                    meSeqRecords.add(thisAssayRecord);
                }
               if (arrayAsString.contains("DNase-Hypersensitivity") ||  arrayAsString.contains("MNase-Seq")) {
                    aTypeUnique.add("Chromatin-Seq");
                    tfSeqRecords.add(thisAssayRecord);
                }
            }
            //ConversionProperties.isValueInDesignTypes("ChIP-Seq") &&
            //NOTE: this is potential problematic: solves issues with AE ChipSeq data but what happens with non chip seq application uisng genomic dna
            if ( (arrayAsString.contains("genomic DNA") || arrayAsString.contains("genomic_DNA")) && !(arrayAsString.contains("MNase-Seq"))) {
                aTypeUnique.add("ChIP-Seq");
                chipSeqRecords.add(thisAssayRecord);
            }
            if ( arrayAsString.contains("RNA-Seq") || arrayAsString.contains("total RNA") ) {
                aTypeUnique.add("RNA-Seq");
                rnaSeqRecords.add(thisAssayRecord);
            }
            if (ConversionProperties.isValueInDesignTypes("dye_swap_design")) {
                aTypeUnique.add("transcription profiling by array");
                genechipRecords.add(thisAssayRecord);
            }
//            else if (arrayAsString.contains("Bisulfite-Seq") || arrayAsString.contains("MRE-Seq") ||
//                    arrayAsString.contains("MBD-Seq") || arrayAsString.contains("MeDIP-Seq ")) {
//                aTypeUnique.add("ME-Seq");
//                meSeqRecords.add(thisAssayRecord);
//            }
//             if (arrayAsString.contains("DNase-Hypersensitivity") ||
//                    arrayAsString.contains("MNase-Seq")) {
//                aTypeUnique.add("Chromatin-Seq");
//                tfSeqRecords.add(thisAssayRecord);
//            }
            if (isHybridizationAssay || arrayAsString.contains("Hybridization") || arrayAsString.contains("biotin")) {
                aTypeUnique.add("Hybridization");
                genechipRecords.add(thisAssayRecord);
            }
        }


        for (String assaytype : aTypeUnique) {
            if ( assaytype.contains("transcription profiling by array")) {
                addToAssays("genechip", assaysFromGivenSDRF, geneChipAssay, genechipRecords);
            }
            if ( assaytype.contains("ChIP-Chip") || assaytype.contains("ChIP-chip")) {
                System.out.println("YAYYYA!!!!!!");
                addToAssays("ChIP-Chip", assaysFromGivenSDRF, geneChipAssay, genechipRecords);
            }
            if (assaytype.contains("ChIP-Seq")|| assaytype.contains("ChIPSeq")) {
                addToAssays("ChIP-Seq", assaysFromGivenSDRF, chipSeqAssay, chipSeqRecords);
            }
            if (assaytype.contains("RNA-Seq")) {
                addToAssays("RNA-Seq", assaysFromGivenSDRF, rnaSeqAssay, rnaSeqRecords);
            }
            if (assaytype.contains("ME-Seq")) {
                addToAssays("ChIP-Seq", assaysFromGivenSDRF, meSeqAssay, meSeqRecords);
            }
            if (assaytype.contains("Chromatin-Seq")) {
                addToAssays("Chromatin-Seq", assaysFromGivenSDRF, TFSeqAssay, tfSeqRecords);
            }
        }

        return new ArrayList<Assay>(assaysFromGivenSDRF);
    }


    private void addToAssays(String type, List<Assay> assaysFromGivenSDRF, Assay assay, List<String[]> records) {
        assay.setAssayLevelInformation(Collections.singletonMap(type, records));
        assaysFromGivenSDRF.add(assay);
    }

}