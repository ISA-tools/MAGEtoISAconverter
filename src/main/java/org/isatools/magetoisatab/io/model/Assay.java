package org.isatools.magetoisatab.io.model;

import java.util.ArrayList;
import java.util.HashMap;



public class Assay {

    public HashMap<String, ArrayList<String[]>> assayLevelInformation;

   // public String assaytype;

    public Assay(HashMap<String, ArrayList<String[]>> assayLevelInformation) {
        this.assayLevelInformation = assayLevelInformation;
    }

    public Assay() {
        //To change body of created methods use File | Settings | File Templates.
    }


    public HashMap<String, ArrayList<String[]>> getAssayLevelInformation() {
        return assayLevelInformation;
    }

    public void setAssayLevelInformation(HashMap<String, ArrayList<String[]>> assayLevelInformation) {
        this.assayLevelInformation = assayLevelInformation;
       // assayLevelInformation.put(assaytype,assayLevelInformation);
    }
}
