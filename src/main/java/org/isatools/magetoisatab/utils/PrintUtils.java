package org.isatools.magetoisatab.utils;

import org.isatools.magetoisatab.io.DownloadUtils;
import org.isatools.magetoisatab.io.model.Study;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PrintUtils {

    public static final Character TAB_DELIM = '\t';
    private static final String PROTOCOL_REF = "Protocol REF";

    List columns2drop = new ArrayList();
    private boolean isPresentDT = false;
    private boolean isPresentDerivedData = false;

    public void printStudySamplesAndAssays(PrintStream ps, Study study, String accnum) {      //, Pair<Integer, Integer> positions

        try {
            for (int j = 0; j < study.getStudySampleLevelInformation().size(); j++) {
                String newSampleRecord = "";
                for (String s : study.getStudySampleLevelInformation().get(j)) {
                    newSampleRecord += s + "\t";
                }
                ps.println(newSampleRecord);
            }

        } catch (Exception e) {

            System.out.println("Caught an IO exception :-o");
            e.printStackTrace();
        }
    }


    /**
     * a method to output Assay Spreadsheet in case there is more than one assay type
     *
     * @param accnum
     */
    public void printAssay(HashMap<String, List<String[]>> assays, String accnum) {           //String assayType,


        //we print the assay file
        try {

            for (String key : assays.keySet()) {

                PrintStream assayPs = new PrintStream(new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/a_" + accnum + "_" + key + "_assay.txt"));

                System.out.println("assay type: " + key + ", Value: " + assays.get(key));

                //writing the data
                for (String[] assaySection : assays.get(key)) {

                    if (getArrayAsString(assaySection).length() > 0) {

                        //TODO: insert values in case DT column is mssing to ensure successful validation
                        if ((isPresentDT = false) && (isPresentDerivedData = true)) {

                            assayPs.println(getArrayAsString(assaySection));

                        }
                        //otherwise, output the section as normal
                        else {
                            assayPs.println(getArrayAsString(assaySection));
                        }
                    }
                }
                //closing file handle
                assayPs.flush();
                assayPs.close();
            }


        } catch (FileNotFoundException e) {

            e.printStackTrace();

        } catch (IOException ioe) {

            System.out.println("Caught an IO exception :-o");
            ioe.printStackTrace();
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

}
