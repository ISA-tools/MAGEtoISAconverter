package org.isatools.magetoisatab.io;


import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;


public class MAGETabObtainTest {

    @Test
    public void testMAGETabConvert() {
        System.out.println("___Testing MAGE-Tab Obtain");
        MAGETabObtain converter = new MAGETabObtain();
        try {
            File isatabDir = converter.doConversion("E-GEOD-16013");

//            String baseDir = System.getProperty("basedir");
//            ISAConfigurationSet.setConfigPath(baseDir + "/target/test-classes/default-configuration/isaconfig-default_v2011-02-18");
//            GUIISATABValidator isatabValidator = new GUIISATABValidator();
//            GUIInvokerResult result = isatabValidator.validate(isatabDir.getAbsolutePath());

//            assertEquals("Validation was not successful, it should have been.", GUIInvokerResult.SUCCESS, result);

            assertTrue("ISA-Tab directory not created", isatabDir.exists());
            assertTrue("ISA-Tab directory should not be empty", isatabDir.list().length > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
