package org.isatools.magetoisatab.io;

import java.io.*;

import au.com.bytecode.opencsv.CSVReader;
import java.util.List;
import java.util.*;
import java.lang.*;

/**
 * Created by the ISA team
 *
 * @author Eamonn Maguire (eamonnmag@gmail.com)
 * @author Philippe Rocca-Serra (proccaserra@gmail.com)
 *         <p/>
 *         Date: 02/03/2011
 *         Time: 18:02
 */
public class MAGETabSDRFLoader {

    public static final Character TAB_DELIM = '\t';

    public String[] columnNames;

    private int studySamplePosition;
    private int firstNodeIfNoSamplePosition;
    private int firstNodePosition;
    private int firstFactorPosition;
    private int nodeDepth;

    private boolean isPresentSample=false;
    private boolean isPresentExtract=false;
    private boolean isPresentLE=false;
    private boolean isPresentHyb=false;
    private boolean isPresentAsssay=false;

    public Map<Integer, String[]> rowValues;

    public Map<Integer, String[]> samples;

    public Set<String[]> assays;

    public MAGETabSDRFLoader() {

        samples = new HashMap<Integer, String[]>();
         assays = new HashSet<String[]>();
    }


    /**
     * A method to read in an SDRF file and process it.
     * @param url
     * @param accnum
     * @throws IOException
     */

    public void loadsdrfTab(String url, String accnum) throws IOException {

        try {

            File file = new File(url);

            if (file.exists()) {

            CSVReader fileReader = new CSVReader(new FileReader(url), TAB_DELIM);

            // you can read each line separately!
            String[] nextLine;
            String line;
            String[] sampleRecord;
            String[] assayRecord;
            String[] vectorFactor;


            rowValues = new HashMap<Integer, String[]>();

            int counter = 0;

            while ( ((nextLine = fileReader.readNext()) != null) && ( getArrayAsString(nextLine).length() !=0) ) {

                //we are dealing with the header row and location the field 'Sample Name'

                if (counter == 0) {

                    columnNames = nextLine;

                    if (getArrayAsString(columnNames).contains("Sample Name")) {  isPresentSample=true;  }
                    if (getArrayAsString(columnNames).contains("Extract Name")) {  isPresentExtract=true;  }
                    if (getArrayAsString(columnNames).contains("Labeled Extract Name")) {  isPresentLE=true;  }
                    if (getArrayAsString(columnNames).contains("Hybridization Name")) {  isPresentHyb=true;  }
                    if (getArrayAsString(columnNames).contains("Assay Name")) {  isPresentHyb=true;  }


//                    if (getArrayAsString(columnNames).contains("Sample Name") ){
//
//                        for (int i = 0; i < nextLine.length; i++) {
//
//                            System.out.println(nextLine[i]);
//                            if (nextLine[i].equals("Sample Name")) {
//
//                                studySamplePosition = i;
//
//                                break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
//                            }
//                        }
//
//                    }



                    if (!getArrayAsString(columnNames).contains("Sample Name")) {
                        if (!getArrayAsString(columnNames).contains("Extract Name")) {
                           if (!getArrayAsString(columnNames).contains("Labeled Extract Name")) {
                             if   (!getArrayAsString(columnNames).contains("Hybridization Name")) {
                                    firstNodePosition = 0;
                                    break;
                                }
                              else {
                                 for (int i = 0; i < nextLine.length; i++) {

                                    if (nextLine[i].equals("Hybridization Name")) {
                                       nodeDepth=4;
                                       System.out.println("Hyb: First Node Found is: "+nextLine[i]);
                                       firstNodePosition = i;
                                       break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                    }
                                 }
                             }
                            } else {
                               for (int i = 0; i < nextLine.length; i++) {

                                    if (nextLine[i].equals("Labeled Extract Name")) {
                                        nodeDepth=3;
                                        System.out.println("LE: First Node Found is: "+nextLine[i]);
                                       firstNodePosition = i;
                                       break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                    }
                                 }

                           }


                        } else {
                            for (int i = 0; i < nextLine.length; i++) {

                                    if (nextLine[i].equals("Extract Name")) {
                                       nodeDepth=2;
                                        System.out.println("Extract: First Node Found is: "+nextLine[i]);
                                       firstNodePosition = i;
                                       break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                    }
                                 }

                        }
                    } else {
                        for (int i = 0; i < nextLine.length; i++) {

                                    if (nextLine[i].equals("Sample Name")) {
                                       nodeDepth=1;
                                        System.out.println("Sample: First Node Found is: "+nextLine[i]);
                                       firstNodePosition = i;
                                       break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
                                    }
                                 }


                    }





                    if (getArrayAsString(columnNames).contains("Factor Name")){

                         for (int i = 0; i < nextLine.length; i++) {

                            System.out.println(nextLine[i]);
                            if (nextLine[i].equals("Factor Name")) {

                                firstFactorPosition = i;


                                break;   // we have found the First Factor field, we can leave, from this point onwards
                            }
                        }


                    }

//                    else if (getArrayAsString(columnNames).contains("Extract Name")) {
//
//                        System.out.println("found Extract Name");
//
//                            for (int i = 0; i < nextLine.length; i++) {
//
//                            System.out.println(nextLine[i]);
//                            if (nextLine[i].equals("Extract Name")) {
//
//                                firstNodeIfNoSamplePosition = i;
//
//                                //break;   // we have found a Sample Name field, we can leave (but we need to handle the case where no such header is present...
//                            }
//                        }
//
//                    }
                }
                else if (studySamplePosition>0 && counter>0) {

                    //now that we know where the 'Sample Name' field is, we are splitting the table

                    rowValues.put(counter, nextLine);

                    sampleRecord = new String[studySamplePosition + 1];

                    if (nextLine.length - studySamplePosition>0) {

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
                    }

                    else { break; }

                    addStudySample(sampleRecord);


                    // do whatever you want with the line
                }
              else if (firstNodePosition>0 && counter>0  && nodeDepth == 1) {

                    rowValues.put(counter, nextLine);

                    sampleRecord = new String[firstNodePosition + 1];


                    if (nextLine.length - firstNodePosition>0) {

                    assayRecord = new String[nextLine.length - firstNodePosition+2];


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
                    }

                    else { break; }

                    addStudySample(sampleRecord);

                }

                else if (firstNodePosition>0 && counter>0  && nodeDepth == 2) {

                    rowValues.put(counter, nextLine);

                    sampleRecord = new String[firstNodePosition + 1];

                    rowValues.put(counter, nextLine);

                    sampleRecord = new String[firstNodePosition + 1];


                    if (nextLine.length - firstNodePosition>0) {

                    assayRecord = new String[nextLine.length - firstNodePosition+2];


                        for (int j = 0; j < nextLine.length; j++) {

                            if (j < firstNodePosition) {
                                sampleRecord[j] = nextLine[j];
                            } else if (j == firstNodePosition) {
                                sampleRecord[j] = nextLine[j];
                                assayRecord[j - firstNodePosition] = nextLine[j]+"\t"+nextLine[j];
                            } else {
                                assayRecord[j - firstNodePosition] = nextLine[j];
                            }
                        }
                        assays.add(assayRecord);
                    }

                    else { break; }

                    addStudySample(sampleRecord);

                }

                else if (firstNodePosition>0 && counter>0  && nodeDepth == 3) {

                    rowValues.put(counter, nextLine);

                    sampleRecord = new String[firstNodePosition + 1];

                    rowValues.put(counter, nextLine);

                    sampleRecord = new String[firstNodePosition + 1];


                    if (nextLine.length - firstNodePosition>0) {

                    assayRecord = new String[nextLine.length - firstNodePosition+3];

                        for (int j = 0; j < nextLine.length; j++) {

                            if (j < firstNodePosition) {
                                sampleRecord[j] = nextLine[j];
                            } else if (j == firstNodePosition) {
                                sampleRecord[j] = nextLine[j];
                                assayRecord[j - firstNodePosition] = nextLine[j]+"\t"+nextLine[j]+"\t"+nextLine[j];
                            } else {
                                assayRecord[j - firstNodePosition] = nextLine[j];
                            }
                        }
                        assays.add(assayRecord);
                    }

                    else { break; }

                    addStudySample(sampleRecord);

                }



                 else { System.out.println("SDRF lacks SAMPLE NAME header"); }
                counter++;
            }
                printFiles(accnum);
           }
           else { System.out.println("ERROR: file not found!");}


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
     * @param columnName
     * @return   a set of indexes
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
     *
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
            PrintStream studyPs = new PrintStream(new File("data/"+accnum+"/s_"+accnum+"_studysample.txt"));
            System.out.println("data/s_"+accnum+"_studysample.txt");
            //todo finish outputting the column headers
            String studySampleHeaders = "";
            String studyAssayHeaders = "";

            if (studySamplePosition>0) {

                 studyAssayHeaders = columnNames[studySamplePosition] + "\t";

                 for (int c=0; c<columnNames.length ; c++) {

                    if (c<=studySamplePosition) {

                    studySampleHeaders +=columnNames[c] + "\t";

                    }

                    else if (c >= studySamplePosition) {

                        if (columnNames[c].equalsIgnoreCase("Hybridization Name")) {
                            columnNames[c]="Hybridization Assay Name";
                        }

                       studyAssayHeaders += columnNames[c] +  "\t";

                    }
                }
            }
            else {


                studyAssayHeaders = "Sample Name"+"\t";

                for (int c=0; c<columnNames.length ; c++) {

                    if (c<=firstNodePosition-1) {

                    studySampleHeaders += columnNames[c] + "\t";
                    }

                    else if (c >= firstNodePosition) {

                        if (columnNames[c].equalsIgnoreCase("Hybridization Name")) {
                            columnNames[c]="Hybridization Assay Name";
                        }

                    studyAssayHeaders += columnNames[c] +  "\t";

                    }
                }

                studySampleHeaders = studySampleHeaders + "Sample Name";
           }



            studyPs.println(studySampleHeaders);


            for (int hash : samples.keySet()) {
                 if (getArrayAsString(samples.get(hash)).length()>0) {
                      studyPs.println(getArrayAsString(samples.get(hash)));
                 }
            }


            studyPs.flush();
            studyPs.close();


            PrintStream assayPs = new PrintStream(new File("data/"+accnum+"/a_"+accnum+"_assay.txt"));

            assayPs.println(studyAssayHeaders);

            for (String[] assaySection : assays) {
                if (getArrayAsString(assaySection).length()>0) {
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
     * @param array
     * @return    a record as a string
     */
    private String getArrayAsString(String[] array) {

        StringBuffer blockAsString = new StringBuffer();

        int val = 0;

        for (String value : array) {
            blockAsString.append(value);
            if (val != array.length - 1) {
                blockAsString.append("\t");
            }
            val++;
        }

        return blockAsString.toString();
    }


}


