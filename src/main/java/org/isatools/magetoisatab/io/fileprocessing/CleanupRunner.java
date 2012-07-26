package org.isatools.magetoisatab.io.fileprocessing;

import java.util.List;

/**
 * Created by the ISA team
 *
 * @author Eamonn Maguire (eamonnmag@gmail.com)
 *         <p/>
 *         Date: 09/07/2012
 *         Time: 23:25
 */
public class CleanupRunner {

    public static List<String[]> cleanupSpreadsheet(List<String[]> spreadsheet, boolean skipCollapse) {

        CleanupUtils collapseColumnUtil = new CollapseColumnUtil();
        CleanupUtils removeDuplicateColumnUtil = new RemoveDuplicateColumnUtil();
        CleanupUtils removeRogueColumnUtil = new RogueColumnRemovalUtil();
        CleanupUtils protocolInsertionUtil = new ProtocolInsertionUtil();

        //we perform the transformation using the processSpreadsheet method

        spreadsheet = removeDuplicateColumnUtil.processSpreadsheet(spreadsheet);
        System.out.println("Ran remove duplicate util.");

        if (!skipCollapse) {
            spreadsheet = collapseColumnUtil.processSpreadsheet(spreadsheet);
            System.out.println("Ran collapse column util.");
        }

        spreadsheet = removeRogueColumnUtil.processSpreadsheet(spreadsheet);
        System.out.println("Ran remove rogue column util.");

        spreadsheet = protocolInsertionUtil.processSpreadsheet(spreadsheet);
        System.out.println("Ran protocol insertion util.");

        return spreadsheet;
    }
}
