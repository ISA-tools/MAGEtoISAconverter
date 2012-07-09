package org.isatools.magetoisatab.io.model;

/**
 * Created by IntelliJ IDEA.
 * User: prs
 * Date: 28/03/2012
 * Time: 14:12
 * To change this template use File | Settings | File Templates.
 */
public class AssayType {
    
    private String measurement;
    private String technology;
    private String shortcut;
    private String platform;
    private String file;

    public AssayType(String measurement, String technology, String shortcut, String platform, String file) {
        this.measurement = measurement;
        this.technology = technology;
        this.shortcut = shortcut;
        this.platform = platform;
        this.file = file;
    }

    public AssayType(String measurement, String technology, String shortcut) {
        this(measurement, technology, shortcut, "", "");
    }

    public String getMeasurement() {
        return measurement;
    }

    public String getShortcut() {
        return shortcut;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
