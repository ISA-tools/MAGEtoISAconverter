package org.isatools.magetoisatab.io.fileprocessing;

/**
 * Created by IntelliJ IDEA.
 * User: prs
 * Date: 25/07/2012
 * Time: 18:45
 * To change this template use File | Settings | File Templates.
 */
public enum InferredProtocolTypes {

   SAMPLE_PREP("Source Name", "Sample Name", "material collection"),
   EXTRACTION("Sample Name", "Extract Name", "material separation"),
   LABELING("Extract Name", "Labeled Extract Name", "labeling"),
   ASSAY("Labeled Extract Name", "Assay Name", "library sequencing"),
   DATA_ACQUISITION("Scan Name", "Raw Data File", "data acquisition"),
   DATA_NORMALIZATION("Assay Name", "Normalization Name", "normalization"),
   DATA_TRANSFORMATION("Normalization Name", "Data Transformation Name", "data transformation"),
   IDENTIFICATION_M("Raw Data File","Metabolite Identification File","metabolite identification"),
   IDENTIFICATION_P("Raw Data File","Protein Identification File","metabolite identification");



    private String firstNode;
    private String lastNode;
    private String type;

    private InferredProtocolTypes(String firstNode, String lastNode, String type) {
        this.firstNode = firstNode;
        this.lastNode = lastNode;
        this.type = type;
    }

    public String getFirstNode() {
        return firstNode;
    }

    public String getLastNode() {
        return lastNode;
    }

    public String getType() {
        return type;
    }
    
    public static InferredProtocolTypes selectTypeGivenNodes(String firstNode, String lastNode) {
        for(InferredProtocolTypes inferredProtocolType : values()) {
            if(inferredProtocolType.firstNode.equals(firstNode) && inferredProtocolType.lastNode.equals(lastNode)) {
                return inferredProtocolType;
            }
        }
        
        return null;
    }
}
