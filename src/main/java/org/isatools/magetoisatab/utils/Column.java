package org.isatools.magetoisatab.utils;

/**
 * Created by IntelliJ IDEA.
 * User: Philippe
 * Date: 16/05/2011
 * Time: 17:58
 * To change this template use File | Settings | File Templates.
 */
public class Column {

    private int index;
    private String label;

    public Column(int index, String label) {
        this.index = index;
        this.label = label;
    }

    public int getIndex() {
        return index;
    }

    public String getLabel() {
        return label;
    }
}
