package org.isatools.magetoisatab.io.fileprocessing;

import java.util.Collection;
import java.util.List;

/**
 * Created by the ISA team
 *
 * @author Eamonn Maguire (eamonnmag@gmail.com)
 *         <p/>
 *         Date: 09/07/2012
 *         Time: 14:43
 */
public abstract class CleanupUtils {

    public abstract List<String[]> processSpreadsheet(List<String[]> spreadsheet);

    protected int[] convertListOfClassesToArrayOfPrimitives(Collection<Integer> indicesToKeep) {
        int[] indices = new int[indicesToKeep.size()];
        int count = 0;
        for (Integer index : indicesToKeep) {
            indices[count] = index;
            count++;
        }
        return indices;
    }

    protected boolean isColumnNameOk(String columnName) {
        return (columnName.contains("Characteristics") || columnName.contains("Factor Value")
                || columnName.contains("Comment") || columnName.contains("Parameter Value"));
    }



}
