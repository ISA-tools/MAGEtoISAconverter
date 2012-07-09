package org.isatools.magetoisatab.io.model;

import java.util.ArrayList;
import java.util.List;


public class Study {


    public ArrayList<String[]> studySampleLevelInformation;

    public List<Assay> assays;

    public Study(ArrayList<String[]> studySampleLevelInformation, List<Assay> assays) {
        this.studySampleLevelInformation = studySampleLevelInformation;
        this.assays = assays;

    }

    public void setStudySampleLevelInformation(ArrayList<String[]> studySampleLevelInformation) {
        this.studySampleLevelInformation = studySampleLevelInformation;
    }

    public ArrayList<String[]> getStudySampleLevelInformation() {
            return studySampleLevelInformation;
    }

    public void setAssays(List<Assay> assays) {
        this.assays = assays;
    }

    public void addAssay(Assay assay) {
        this.assays.add(assay);
    }

    public List<Assay> getAssays() {
        return assays;
    }
}
