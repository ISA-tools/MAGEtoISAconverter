package org.isatools.magetoisatab.io.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Assay {

    public Map<String, List<String[]>> assayLevelInformation;

   // public String assaytype;

    public Assay(Map<String, List<String[]>> assayLevelInformation) {
        this.assayLevelInformation = assayLevelInformation;
    }

    public Assay() {
        //To change body of created methods use File | Settings | File Templates.
    }


    public Map<String, List<String[]>> getAssayLevelInformation() {
        return assayLevelInformation;
    }

    public void setAssayLevelInformation(Map<String, List<String[]>> assayLevelInformation) {
        this.assayLevelInformation = assayLevelInformation;
       // assayLevelInformation.put(assaytype,assayLevelInformation);
    }
}
