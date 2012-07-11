package org.isatools.magetoisatab.io.fileprocessing.technologylocation;

/**
 * Created by IntelliJ IDEA.
 * User: prs
 * Date: 11/07/2012
 * Time: 16:57
 * To change this template use File | Settings | File Templates.
 */
public enum Technologies {

    CHIP_SEQ("chip-seq", "");


    private String name;
    private String[] associatedWith;

    private Technologies(String name, String... associatedWith) {
        this.name = name;
        this.associatedWith = associatedWith;
    }
}
