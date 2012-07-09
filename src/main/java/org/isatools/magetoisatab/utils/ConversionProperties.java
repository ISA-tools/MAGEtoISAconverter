package org.isatools.magetoisatab.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by the ISA team
 *
 * @author Eamonn Maguire (eamonnmag@gmail.com)
 *         <p/>
 *         Date: 09/07/2012
 *         Time: 14:00
 */
public class ConversionProperties {
    
    private static Set<String> designTypes = new HashSet<String>();

    public static Set<String> getDesignTypes() {
        return designTypes;
    }

    public static void setDesignTypes(Set<String> designTypes) {
        ConversionProperties.designTypes = designTypes;
    }
    
    public static void addDesignType(String designType) {
        ConversionProperties.designTypes.add(designType);
    }
    
    public static boolean isValueInDesignTypes(String value) {
        for(String designLine : designTypes) {
            if(designLine.toLowerCase().contains(value)) {
                return true;
            }
        }
        return false;
    }
}
