package org.isatools.magetoisatab.io.fileprocessing;

import au.com.bytecode.opencsv.CSVReader;
import org.junit.Ignore;
import org.junit.Test;

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
 *         Date: 10/07/2012
 *         Time: 01:08
 */
public class CleanupTest {

    @Ignore
    public void testCleanup() {
        try {
            CSVReader reader = new CSVReader(new FileReader(new File("Data/E-GEOD-16013/a_E-GEOD-16013_ChIP-Seq_assay.txt")));
            List<String[]> sheet = reader.readAll();
            sheet = CleanupRunner.cleanupSpreadsheet(sheet, false);

            System.out.println(sheet.size());
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
