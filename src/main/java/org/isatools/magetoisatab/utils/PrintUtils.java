package org.isatools.magetoisatab.utils;

import org.isatools.magetoisatab.io.model.Study;
import java.io.PrintStream;

public class PrintUtils {

    public static final Character TAB_DELIM = '\t';

    public void printStudySamples(PrintStream ps, Study study) {

        try {
            for (int j = 0; j < study.getStudySampleLevelInformation().size(); j++) {
                String newSampleRecord = "";
                for (String s : study.getStudySampleLevelInformation().get(j)) {
                    newSampleRecord += s + TAB_DELIM;
                }
                ps.println(newSampleRecord);
            }

        } catch (Exception e) {

            System.out.println("Caught an IO exception :-o");
            e.printStackTrace();
        }
    }

}
