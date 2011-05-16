package org.isatools.magetoisatab.io;

import au.com.bytecode.opencsv.CSVReader;
import org.isatools.io.FileType;
import org.isatools.io.Loader;

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

        try {

            if (mageTaDir.exists()) {
                if (mageTaDir.isDirectory()) {

                    File[] mageTabFiles = mageTaDir.listFiles();

                    for (File file : mageTabFiles) {
                        Loader fileReader = new Loader();

                        List<String[]> myEntries = fileReader.loadSheet(file.getAbsolutePath(), FileType.TAB);
                    }


                } else {
                    // throw an Exception declaring that the given path is not a directory
                }
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
