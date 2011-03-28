package org.isatools.magetoisatab.io;

import au.com.bytecode.opencsv.CSVReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by the ISA team
 *
 * @author Eamonn Maguire (eamonnmag@gmail.com)
 *         <p/>
 *         Date: 02/03/2011
 *         Time: 18:02
 */
public class MAGETabLoader {

    public static final Character TAB_DELIM = '\t';

    private void loadMAGETab(String MAGETabDirectory) throws IOException {

        File mageTaDir = new File(MAGETabDirectory);

        if (mageTaDir.exists()) {
            if (mageTaDir.isDirectory()) {

                File[] mageTabFiles = mageTaDir.listFiles();

                for (File file : mageTabFiles) {
                    CSVReader fileReader = new CSVReader(new FileReader(file), TAB_DELIM);

                    // you can read each line separately!
                    String[] nextLine;

                    while ((nextLine = fileReader.readNext()) != null) {

                        // do whatever you want with the line

                    }


                    // or you can get the whole file as a list of String arrays. Each list item is a row.
                    List<String[]> myEntries = fileReader.readAll();
                }



            } else {
                // throw an Exception declaring that the given path is not a directory
            }
        }

    }

    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
