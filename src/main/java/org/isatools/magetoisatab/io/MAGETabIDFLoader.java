package org.isatools.magetoisatab.io;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.isatools.magetoisatab.io.model.AssayType;
import org.isatools.magetoisatab.io.model.Study;
import org.isatools.magetoisatab.utils.ConversionProperties;
import org.isatools.magetoisatab.utils.PrintUtils;

import java.io.*;
import java.util.*;

/**
 * Created by the ISA team
 *
 * @author Philippe Rocca-Serra (proccaserra@gmail.com)
 *         <p/>
 *         Date: 02/03/2011
 *         Time: 18:02
 */

/**
 * A class to convert MAGE-TAB idf file to an ISA-TAB investigation file.
 * The input can be either an ArrayExpress Accession number or the full address of a file on a local file system.
 * The output is an ISA-TAB investigation file.
 */

//TODO: in a number of cases MAGE-TAB encoded data needs relocation and reassignment (typically Chip Antibodies)
//TODO: perform the collapsing the gapped characteristiscs


public class MAGETabIDFLoader {

    private static final Logger log = Logger.getLogger(MAGETabIDFLoader.class.getName());

    public String[] sdrfFileNames;


    public List<String> investigationLines;

    //HashMap initialization to define canonical Study Publication block structure
    public Map<Integer, String> IsaPublicationSection = new HashMap<Integer, String>() {
        {
            put(0, "Study PubMed ID");
            put(1, "Study Publication DOI");
            put(2, "Study Publication Author List");
            put(3, "Study Publication Title");
            put(4, "Study Publication Status");
            put(5, "Study Publication Status Term Accession Number");
            put(6, "Study Publication Status Term Source REF");
        }
    };

    public List<String> publicationLines;

    public List<String> studyDesc;
    public List<String> assaylines;
    public List<String> dateLines;


    public Map<Integer, String> isaOntoSection = new HashMap<Integer, String>() {
        {
            put(0, "Term Source Name");
            put(1, "Term Source File");
            put(2, "Term Source Version");
            put(3, "Term Source Description");
        }
    };


    public List<String> factorLines = new ArrayList<String>() {
        {
            add("Study Factor Name");
            add("Study Factor Type");
            add("Study Factor Type Term Accession Number");
            add("Study Factor Type Term Source REF");
        }
    };

    public List<String> designLines = new ArrayList<String>() {
        {
            add("Study Design Type");
            add("Study Design Type Term Accession Number");
            add("Study Design Type Term Source REF");
        }
    };

    //HashMap initialization to define canonical block structure
    public Map<Integer, String> isaProtocolSection = new HashMap<Integer, String>() {
        {
            put(0, "Study Protocol Name");
            put(1, "Study Protocol Type");
            put(2, "Study Protocol Type Term Accession Number");
            put(3, "Study Protocol Type Term Source REF");
            put(4, "Study Protocol Description");
            put(5, "Study Protocol URI");
            put(6, "Study Protocol Version");
            put(7, "Study Protocol Parameters Name");
            put(8, "Study Protocol Parameters Name Term Accession Number");
            put(9, "Study Protocol Parameters Name Term Source REF");
            put(10, "Study Protocol Components Name");
            put(11, "Study Protocol Components Type");
            put(12, "Study Protocol Components Type Term Accession Number");
            put(13, "Study Protocol Components Type Term Source REF");
        }
    };


    //HashMap initialization to define canonical block structure
    public Map<Integer, String> isaContactSection = new HashMap<Integer, String>() {
        {
            put(0, "Study Person Last Name");
            put(1, "Study Person First Name");
            put(2, "Study Person Mid Initials");
            put(3, "Study Person Email");
            put(4, "Study Person Phone");
            put(5, "Study Person Fax");
            put(6, "Study Person Address");
            put(7, "Study Person Affiliation");
            put(8, "Study Person Roles");
            put(9, "Study Person Roles Term Accession Number");
            put(10, "Study Person Roles Term Source REF");
        }
    };

    Map<InvestigationSections, List<String>> investigationSections;


    public MAGETabIDFLoader() {
        investigationSections = new HashMap<InvestigationSections, List<String>>();
    }

    private void populateIDF() {
        investigationSections.put(InvestigationSections.STUDY_PROTOCOL_SECTION, new ArrayList<String>());
        investigationSections.put(InvestigationSections.STUDY_CONTACT_SECTION, new ArrayList<String>());
        investigationSections.put(InvestigationSections.STUDY_PUBLICATION_SECTION, new ArrayList<String>());
        investigationLines = new ArrayList<String>();
        dateLines = new ArrayList<String>();
    }


    public void loadidfTab(String url, String accnum) throws IOException {

        try {
            populateIDF();
            List<String> sdrfDownloadLocation = new ArrayList<String>();
            File file = new File(url);

            boolean success = (new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum)).mkdirs();
            if (success) {
                System.out.println("Directory: " + accnum + " created");
            }

            if (file.exists()) {

                System.out.println("Starting processing of IDF file");

                processIncomingIDFFile(accnum, sdrfDownloadLocation, file);
                System.out.println("Ending processing of IDF file");

                PrintStream invPs = new PrintStream(new File(
                        DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/i_" + accnum + "_investigation.txt"));

                printOntologySourceRefSection(invPs);
                printInvestigationSection(accnum, invPs);

                invPs.println("Study Submission Date");

                for (String dateLine : dateLines) {
                    invPs.println(dateLine);
                }

                for (String aStudyDesc : studyDesc) {
                    invPs.println(aStudyDesc);
                }

                invPs.println("Study File Name" + "\t" + "s_" + accnum + "_study_samples.txt");

                invPs.println("STUDY DESIGN DESCRIPTORS");

                for (String designLine : designLines) {
                    invPs.println(designLine);
                }


                invPs.println("STUDY PUBLICATIONS");
                if (investigationSections.get(InvestigationSections.STUDY_PUBLICATION_SECTION).size() > 0) {

                    for (String publicationLine : investigationSections.get(InvestigationSections.STUDY_PUBLICATION_SECTION)) {

//
//                //if (publicationLines.size() > 0) {
//                    for (String publicationLine : publicationLines) {

                        if (publicationLine.contains("PubMed")) {
                            IsaPublicationSection.put(0, publicationLine);
                        }
                        if (publicationLine.contains("DOI")) {
                            IsaPublicationSection.put(1, publicationLine);
                        }
                        if (publicationLine.contains("List")) {
                            IsaPublicationSection.put(2, publicationLine);
                        }
                        if (publicationLine.contains("Title")) {
                            IsaPublicationSection.put(3, publicationLine);
                        }
                        if ((publicationLine.contains("Status")) && !(publicationLine.contains("Status Term"))) {
                            IsaPublicationSection.put(4, publicationLine);
                        }
                        if (publicationLine.contains("Status Term Accession")) {
                            IsaPublicationSection.put(5, publicationLine);
                        }
                        if (publicationLine.contains("Status Term Source")) {
                            IsaPublicationSection.put(6, publicationLine);
                        }
                    }
                }

                //we now output the Publication Section of an ISA Study
                for (Map.Entry<Integer, String> e : IsaPublicationSection.entrySet())
                    invPs.println(e.getValue());


                // Now Creating the Factor Section
                invPs.println("STUDY FACTORS");

                for (String factorLine : factorLines) {
                    //this is to take care of unsupported commonly used characters and match the replacements performed
                    //in the MAGETabSDRFloader to match declared and used factors.
                    factorLine = factorLine.replace(". #", " number");
                    invPs.println(factorLine);
                }

                //Now creating the Assay Section:
                invPs.println("STUDY ASSAYS");


                // We are now trying to get the Measurement and Technology Type from MAGE annotation Experimental Design Type

                List<AssayType> assayTTMT = getMeasurementAndTech(designLines.get(0));

                //trying to recover information about assay type in the absence of information in  Comment[AE..] and StudyDesign fields
                for (String investigationLine : investigationLines) {

                    if (investigationLine.contains("Study Title\tTranscription prof") || investigationLine.contains("Study Title\tGene expression prof")
                            || investigationLine.contains("Study Title\ttranscription prof") || investigationLine.contains("Study Title\tgene expression prof")) {

                        System.out.println("BYE investigationLine = " + investigationLine);
                        //NOTE: we take a risk here by assuming that in the absence of information  in  Comment[AE..] and StudyDesign fields, finding 'Gene expression profiling or Transcription profiling in the title
                        // means it always uses microarrays.
                        assayTTMT.add(new AssayType("transcription profiling", "DNA microarray", "GeneChip"));
                    }
                }

                // If this fails, we are falling back on checking MAGE-TAB Comment[AEExperimentType] line
                String measurementTypes = "Study Assay Measurement Type";
                String technologyTypes = "Study Assay Technology Type";


                for (AssayType anAssayTTMT1 : assayTTMT) {
                    //testing if it contains something meaningful!
                    if (!anAssayTTMT1.getMeasurement().equals("") || anAssayTTMT1.getMeasurement() != null
                            || anAssayTTMT1.getTechnology().equals("") || anAssayTTMT1.getTechnology() != null) {
                        measurementTypes = measurementTypes + "\t" + anAssayTTMT1.getMeasurement();
                        technologyTypes = technologyTypes + "\t" + anAssayTTMT1.getTechnology();
                    }

                }

                invPs.println(measurementTypes);
                invPs.println("Study Assay Measurement Type Term Accession Number\n" +
                        "Study Assay Measurement Type Term Source REF");


                invPs.println(technologyTypes);
                invPs.println("Study Assay Technology Type Term Accession Number\n" +
                        "Study Assay Technology Type Term Source REF\n" +
                        "Study Assay Technology Platform");

                String assayfilenames = "Study Assay File Name";


                //we now create as many assay spreadsheet as needed:

                //case1: there is only SDRF and we rely on the information found under Comment[AEexperimentTypes]
                //NOTE: caveat: AE is inconsistent and encode various measurement types under the same spreadsheet for sequencing applications
                if (assayTTMT.size() > 0) {

                    for (AssayType anAssayTTMT : assayTTMT) {       //we start at 1 as the first element of the array is the header "
                        assayfilenames = assayfilenames + "\ta_" + accnum + "_" + anAssayTTMT.getShortcut() + "_assay.txt";
                        System.out.println("CASE1: " + anAssayTTMT.getShortcut());
                    }

                    System.out.println("CASE1: " + assayfilenames);
                }

                //case2: there are more than 1 SDRF and we rely on the information found under Comment[AEexperimentTypes]
                else if (sdrfFileNames != null && sdrfFileNames.length > 0 && (sdrfFileNames.length == ConversionProperties.getDesignTypes().size())) {

                    for (String cmtDesignType : ConversionProperties.getDesignTypes()) { //we start at 1 as the first element of the array is the header "

                        if (ConversionProperties.isValueInDesignTypes("chip-seq") || ConversionProperties.isValueInDesignTypes("ChIP-seq")) {

                            for (AssayType anAssayTTMT : assayTTMT) {
                                if ((anAssayTTMT.getMeasurement().equalsIgnoreCase("protein-DNA binding site identification")) &&
                                        (anAssayTTMT.getTechnology().equalsIgnoreCase("nucleotide sequencing"))) {
                                    anAssayTTMT.setFile(cmtDesignType.replaceAll("\\?iChIP-seq", "ChIP-Seq"));
                                }
                            }
                        }

                        if (ConversionProperties.isValueInDesignTypes("transcription profiling by array")) {
                            for (AssayType anAssayTTMT : assayTTMT) {

                                if ((anAssayTTMT.getMeasurement().equalsIgnoreCase("transcription profiling")) &&
                                        (anAssayTTMT.getTechnology().equalsIgnoreCase("DNA microarray"))) {
                                    anAssayTTMT.setFile(cmtDesignType.replaceAll("transcription profiling by array", "GeneChip"));
                                }
                            }
                        }
                    }


                    for (AssayType anAssayTTMT : assayTTMT) {
                        assayfilenames = assayfilenames + "\ta_" + accnum + "_" + anAssayTTMT.getFile().replaceAll("\\s", "_") + "_assay.txt";
                    }
                    System.out.println("CASE2: " + assayfilenames);
                }

                //now we can output that IDF row containing all

                invPs.println(assayfilenames);

                //Now creating the Protocol section
                invPs.println("STUDY PROTOCOLS");


                if (investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).size() > 0) {

                    for (String protocolLine : investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION)) {

                        if (protocolLine.contains("Name") && technologyTypes.contains("sequencing")) {
                            isaProtocolSection.put(0, investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).get(0).concat("\tlibrary construction\tnucleic acid sequencing"));

                        } else if (protocolLine.contains("Name")) {
                            isaProtocolSection.put(0, protocolLine);

                        }

                        if (protocolLine.contains("Type") && technologyTypes.contains("sequencing")) {
                            System.out.println("protocol line: " + protocolLine);
                            String tempProtocolType = protocolLine.concat("\tlibrary construction\tnucleic acid sequencing");
                            isaProtocolSection.put(1, tempProtocolType);

                            System.out.println("modified protocol line: " + tempProtocolType);
                        } else if (protocolLine.contains("Type")) {
                            isaProtocolSection.put(1, protocolLine);
                        }

                        if (protocolLine.contains("Accession")) {
                            String tempAcc = protocolLine.replaceAll("Term Accession", "Type Term Accession");
                            isaProtocolSection.put(2, tempAcc);
                        }

                        if (protocolLine.contains("Term Source")) {
                            String tempSource = protocolLine.replaceAll("Term Source", "Type Term Source");
                            isaProtocolSection.put(3, tempSource);
                        }

                        if (protocolLine.contains("Description")) {
                            isaProtocolSection.put(4, protocolLine);
                        }

                        if (protocolLine.endsWith("Parameters")) {

                            String tempParam = protocolLine.replaceAll("Parameters", "Parameters Name");
                            isaProtocolSection.put(5, tempParam);
                        }

                        if ((protocolLine.contains("Software")) || (protocolLine.contains("Hardware"))) {

                            String tempComponent = protocolLine.replaceAll("Software", "Components Name");
                            tempComponent = tempComponent.replaceAll("Hardware", "Components Name");
                            isaProtocolSection.put(10, tempComponent);
                        }
                    }
                }

                //we now output the Protocol Section of an ISA Study
                for (Map.Entry<Integer, String> e : isaProtocolSection.entrySet())
                    invPs.println(e.getValue());


                // Let's now deal with the Contact Information Section
                invPs.println("STUDY CONTACTS");

                if (investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION).size() > 0) {

                    for (String contactLine : investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION)) {

                        if (contactLine.contains("Last")) {
                            isaContactSection.put(0, contactLine);
                        }
                        if (contactLine.contains("First")) {
                            isaContactSection.put(1, contactLine);
                        }
                        if (contactLine.contains("Mid")) {
                            isaContactSection.put(2, contactLine);
                        }
                        if (contactLine.contains("Email")) {
                            isaContactSection.put(3, contactLine);
                        }
                        if (contactLine.contains("Phone")) {
                            isaContactSection.put(4, contactLine);
                        }
                        if (contactLine.contains("Fax")) {
                            isaContactSection.put(5, contactLine);
                        }
                        if (contactLine.contains("Address")) {
                            isaContactSection.put(6, contactLine);
                        }
                        if (contactLine.contains("Affiliation")) {
                            isaContactSection.put(7, contactLine);
                        }
                        if ((contactLine.contains("Roles") && !(contactLine.contains("Roles Term")))) {

                            isaContactSection.put(8, contactLine);
                        }
                    }

                    //we now output the Contact Section of an ISA Study
                    for (Map.Entry<Integer, String> e : isaContactSection.entrySet())
                        invPs.println(e.getValue());

                    for (String sdrfFile : sdrfFileNames) {
                        System.out.println("Processing " + sdrfFile);
                        PrintUtils pu = new PrintUtils();

                        PrintStream ps = new PrintStream(new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accnum + "/s_" + accnum + "_" + "study_samples.txt"));

                        List<Map<String, List<String>>> studies = new ArrayList<Map<String, List<String>>>();

                        //we start at 1 as the first element of the sdrfFilenames array corresponds to the ISA Study File Name tag
                        for (int counter = 0; counter < sdrfFileNames.length; counter++) {

                            System.out.println("SDRF number " + counter + " is:" + sdrfDownloadLocation.get(counter));

                            MAGETabSDRFLoader sdrfloader = new MAGETabSDRFLoader();

                            Study study = sdrfloader.loadsdrfTab(sdrfDownloadLocation.get(counter), accnum, assayTTMT);

                            Map<String, List<String>> table = new LinkedHashMap<String, List<String>>();

                            if (study.getStudySampleLevelInformation().size() > 0) {
                                for (int i = 0; i < study.getStudySampleLevelInformation().get(0).length; i++) {

                                    String key = study.getStudySampleLevelInformation().get(0)[i];

                                    List<String> values = new ArrayList<String>();

                                    for (int k = 1; k < study.getStudySampleLevelInformation().size(); k++) {
                                        String value = study.getStudySampleLevelInformation().get(k)[i];
                                        if (value != null) {
                                            values.add(value);
                                        }
                                    }

                                    table.put(key, values);

                                }
                            }

                            studies.add(table);

                            pu.printStudySamples(ps, study);

                            ps.flush();
                            ps.close();
                        }

                        mergeTables(studies);

                        //this set's keys are the final header of the merged study sample file
                        Set<String> tableKeyset = mergeTables(studies).keySet();
                        String finalStudyTableHeader = "";

                        //we now splice the header together by concatenating the key
                        for (String aTableKeyset : tableKeyset) {
                            finalStudyTableHeader = finalStudyTableHeader + aTableKeyset + "\t";
                        }
                        //we print the header
                        ps.println(finalStudyTableHeader);

                        //we now need to get the total number of records. This corresponds to the number of elements in the arrays associated to the key "Sample Name"

                        int numberOfSampleRecords;

                        List<String> guestList = mergeTables(studies).get("Sample Name");

                        numberOfSampleRecords = guestList.size();

                        Set<String> finalStudyTable = new HashSet<String>();

                        for (int sampleRecordIndex = 0; sampleRecordIndex < numberOfSampleRecords; sampleRecordIndex++) {

                            String studyRecord = "";

                            for (String key : mergeTables(studies).keySet()) {

                                //obtain the list associated to that given key
                                List<String> correspondingList = mergeTables(studies).get(key);

                                // now obtain the ith element of that associated list
                                if (sampleRecordIndex < correspondingList.size()) {
                                    studyRecord += correspondingList.get(sampleRecordIndex) + "\t";
                                }
                            }

                            finalStudyTable.add(studyRecord);
                        }

                        //Here we print the new records to the final study sample file
                        for (String aFinalStudyTable : finalStudyTable) {
                            ps.println(aFinalStudyTable);
                        }

                        //closing file handle
                        ps.flush();
                        ps.close();
                    }
                }
            } else {
                System.out.println("ERROR: File not found");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void processIncomingIDFFile(String accnum, List<String> sdrfDownloadLocation, File file) throws IOException {
        CSVReader reader = new CSVReader(new FileReader(file), '\t');
        String[] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            String rowName = nextLine[0];

            if (rowName.startsWith("Protocol")) {
                String line = arrayToString(nextLine).replaceFirst("Protocol", "Study Protocol");
                investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).add(line);
            } else if (rowName.startsWith("Experiment Desc")) {

                String line = arrayToString(nextLine).replaceFirst("Experiment", "Study");
                if (studyDesc == null) {
                    studyDesc = new ArrayList<String>();
                }
                studyDesc.add(line);
            } else if (rowName.startsWith("Person")) {
                String line = arrayToString(nextLine).replaceFirst("Person", "Study Person");
                investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION).add(line);
            } else if (rowName.startsWith("PubMed")) {
                String line = arrayToString(nextLine).replaceFirst("PubMed", "Study PubMed");

                investigationSections.get(InvestigationSections.STUDY_PUBLICATION_SECTION).add(line);
//                            if (publicationLines == null) {
//                                publicationLines = new ArrayList<String>();
//                            }
//                            publicationLines.add(line);
            }

            //This is to handle ArrayExpress GEO to MAGE converter propagating PubMed ID to the Publication DOI field
            else if (rowName.startsWith("Publication DOI")) {

                String line = arrayToString(nextLine).replaceFirst("Publication DOI", "Study Publication DOI");
                if (publicationLines == null) {
                    publicationLines = new ArrayList<String>();
                }
                publicationLines.add(line);
                investigationSections.get(InvestigationSections.STUDY_PUBLICATION_SECTION).add(line);
            } else if ((rowName.startsWith("Publication")) && !(rowName.contains("DOI"))) {

                String line = arrayToString(nextLine).replaceFirst("Publication", "Study Publication");
                if (publicationLines == null) {
                    publicationLines = new ArrayList<String>();
                }
                publicationLines.add(line);
                investigationSections.get(InvestigationSections.STUDY_PUBLICATION_SECTION).add(line);
            }

            //Now Dealing with element from Study Factor Section
            else if (rowName.startsWith("Experimental Factor Name")) {
                String line = arrayToString(nextLine).toLowerCase().replaceFirst("experimental factor name", "Study Factor Name");
                factorLines.set(0, line);
            } else if (rowName.startsWith("Experimental Factor Type")) {
                String line = arrayToString(nextLine).toLowerCase().replaceFirst("experimental factor type", "Study Factor Type");
                factorLines.set(1, line);
            } else if (rowName.endsWith("Factor Term Accession")) {
                String line = arrayToString(nextLine).replaceFirst("Experimental Factor", "Study Factor Type");
                factorLines.set(2, line);
            } else if (rowName.endsWith("Factor Term Source REF")) {
                String line = arrayToString(nextLine).replaceFirst("Experimental Factor", "Study Factor Type");
                factorLines.set(3, line);
            } else if ((rowName.contains("Experimental Design")) && (!(rowName.contains("Experimental Design Term")))) {

                System.out.println("Experimental Design Tag found at: " + rowName);

                String line = arrayToString(nextLine).replaceFirst("Experimental Design", "Study Design Type");
                designLines.set(0, line);

                for (String designType : designLines) {
                    ConversionProperties.addDesignType(designType);
                }
            }

            //This bit is used to recover information for setting ISA MT and TT in case no Experimental Design is found
            else if (rowName.startsWith("Comment[AEExperimentType")) {

                System.out.println("Alternative Design Tag found at: " + rowName);

                for (String designType : nextLine) {
                    ConversionProperties.addDesignType(designType);
                }

                String line = arrayToString(nextLine).replace("Comment[AEExperimentType]", "Study Design Type");
                designLines.set(0, line);

            } else if (rowName.startsWith("SDRF File")) {

                sdrfFileNames = new String[nextLine.length - 1];
                System.arraycopy(nextLine, 1, sdrfFileNames, 0, nextLine.length - 1);

                System.out.println("number of SDRF files: " + (sdrfFileNames.length));

                //There is more than one SDRF file listed in this submission, now iterating through them:");
                for (String sdrfFileName : sdrfFileNames) {
                    String sdrfUrl = "http://www.ebi.ac.uk/arrayexpress/files/" + accnum + "/" + sdrfFileName;
                    String sdrfFile = DownloadUtils.TMP_DIRECTORY + File.separator + accnum + File.separator + sdrfFileName;
                    sdrfDownloadLocation.add(sdrfFile);
                    DownloadUtils.downloadFile(sdrfUrl, sdrfFile);
                    System.out.println("SDRF found and downloaded: " + sdrfUrl);
                }

                if (assaylines == null) {
                    assaylines = new ArrayList<String>();
                }
                assaylines.add(arrayToString(nextLine).replaceFirst("SDRF File", "Study Assay File Name"));
            } else if (rowName.contains("Investigation")) {
                System.out.println("Processing investigation section.");
                String line = arrayToString(nextLine).replaceFirst("Investigation", "Study");

                investigationLines.add(line);
            } else if (rowName.startsWith("Public R")) {
                String line = arrayToString(nextLine).replaceFirst("Public", "Study Public");

                dateLines.add(line);
            }


            // looks for information about Ontology and Terminologies used in MAGE-TAB document

            else if (rowName.startsWith("Term Source Name")) {

                String tempLine = "";
                tempLine = removeDuplicates(arrayToString(nextLine));
                isaOntoSection.put(0, tempLine);

            } else if (rowName.startsWith("Term Source File")) {

                String tempLine = "";
                tempLine = removeDuplicates(arrayToString(nextLine));
                isaOntoSection.put(1, tempLine);

                System.out.println("IS THIS A FILE" + tempLine);

            } else if (rowName.startsWith("Term Source Version")) {
                String tempLine = "";
                tempLine = removeDuplicates(arrayToString(nextLine));
                isaOntoSection.put(2, tempLine);

            } else if (rowName.startsWith("Term Source Description")) {

                String tempLine = "";
                tempLine = removeDuplicates(arrayToString(nextLine));
                isaOntoSection.put(3, tempLine);

            }
        }
    }

    /**
     * Will take an Array and produce the String equivalent and separated with a given separator
     *
     * @param array - String array to be converted.
     * @return String representation of the array
     */
    private String arrayToString(String[] array) {
        StringBuilder string = new StringBuilder();

        int count = 0;
        for (String value : array) {
            string.append(value);
            if (count != array.length - 1) {
                string.append('\t');
            }
            count++;
        }

        return string.toString();
    }

    private void printOntologySourceRefSection(PrintStream invPs) {
        //Outputting the ISA-TAB Ontology Section
        invPs.println("ONTOLOGY SOURCE REFERENCE");

        for (Map.Entry<Integer, String> e : isaOntoSection.entrySet())
            invPs.println(e.getValue());
    }

    private void printInvestigationSection(String accnum, PrintStream invPs) {
        //Outputing ISA-TAB Investigation Section which is always empty as MAGE-TAB does not support this.
        invPs.println("INVESTIGATION\n" +
                "Investigation Identifier\n" +
                "Investigation Title\n" +
                "Investigation Description\n" +
                "Investigation Submission Date\n" +
                "Investigation Public Release Date\n" +
                "INVESTIGATION PUBLICATIONS\n" +
                "Investigation PubMed ID\n" +
                "Investigation Publication DOI\n" +
                "Investigation Publication Author List\n" +
                "Investigation Publication Title\n" +
                "Investigation Publication Status\n" +
                "Investigation Publication Status Term Accession Number\n" +
                "Investigation Publication Status Term Source REF\n" +
                "INVESTIGATION CONTACTS\n" +
                "Investigation Person Last Name\n" +
                "Investigation Person First Name\n" +
                "Investigation Person Mid Initials\n" +
                "Investigation Person Email\n" +
                "Investigation Person Phone\n" +
                "Investigation Person Fax\n" +
                "Investigation Person Address\n" +
                "Investigation Person Affiliation\n" +
                "Investigation Person Roles\n" +
                "Investigation Person Roles Term Accession Number\n" +
                "Investigation Person Roles Term Source REF\n" +
                "\nSTUDY\n" +
                "Study Identifier" + "\t" + accnum);


        for (String investigationLine : investigationLines) {
            invPs.println(investigationLine);
        }
    }

    private Map<String, List<String>> mergeTables(List<Map<String, List<String>>> studies) {

        Set<String> allKeys = new LinkedHashSet<String>();
        Map<String, List<String>> resultMap = new LinkedHashMap<String, List<String>>();

        for (Map<String, List<String>> tables : studies) {
            allKeys.addAll(tables.keySet());
        }

        for (String key : allKeys) {

            List<List<String>> keyLists = new ArrayList<List<String>>();

            for (Map<String, List<String>> tables : studies) {
                if (tables.containsKey(key)) {
                    keyLists.add(tables.get(key));
                }
            }

            List<String> newList = new ArrayList<String>();

            for (List<String> keyList : keyLists) {
                newList.addAll(keyList);
            }

            resultMap.put(key, newList);
        }

        return resultMap;
    }


    /**
     * A method to remove duplicate entries in Ontology Section
     */
    private String removeDuplicates(String ontoline) {

        String newLine = "";

        String[] stringArray = ontoline.split("\\t");

        List<String> stringArrayList = new ArrayList<String>(Arrays.asList(stringArray));

        Set<String> set = new LinkedHashSet<String>(stringArrayList);

        for (String aSet : set) {
            newLine = newLine + aSet + "\t";

        }
        return newLine;

    }


    /**
     * A method that uses MAGE-TAB Experiment Design information to deduce ISA Measurement and Technology Types.
     *
     * @param line
     * @return A Pair of Strings containing the Measurement and Technology Types to be output
     *         TODO: rely on an xml configuration file to instead of hard coded values -> easier to maintain in case of changes in ArrayExpress terminology
     */
    private List<AssayType> getMeasurementAndTech(String line) {

        // Line can contain multiple technologies. We must output them all as AssayTypes.

        List<AssayType> assayTypes = new ArrayList<AssayType>();

        if (line.matches("(?i).*ChIP-Chip.*")) {
            assayTypes.add(new AssayType("protein-DNA binding site identification", "DNA microarray", "ChIP-Chip"));
        }

        if ((line.matches("(?i).*RNA-seq.*")) || (line.matches("(?i).*RNA-Seq.*")) || (line.matches("(?i).*transcription profiling by high throughput sequencing.*"))) {
            assayTypes.add(new AssayType("transcription profiling", "nucleotide sequencing", "RNA-Seq"));
            isaProtocolSection.put(0, investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).get(0).concat("\tlibrary construction\tnucleic acid sequencing"));
            isaProtocolSection.put(1, investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).get(1).concat("\tlibrary construction\tnucleic acid sequencing"));

        }

        if (line.matches(".*transcription profiling by array.*") || line.matches("dye_swap_design")) {
            assayTypes.add(new AssayType("transcription profiling", "DNA microarray", "GeneChip"));
        }
        if (line.matches("(?i).*methylation profiling by array.*")) {
            assayTypes.add(new AssayType("DNA methylation profiling", "DNA microarray", "Me-Chip"));

        }
        if (line.matches("(?i).*comparative genomic hybridization by array.*")) {
            assayTypes.add(new AssayType("comparative genomic hybridization", "DNA microarray", "CGH-Chip"));
        }
        if (line.matches(".*genotyping by array.*")) {
            assayTypes.add(new AssayType("SNP analysis", "DNA microarray", "SNPChip"));
        }

        if ((line.matches("(?i).*ChIP-Seq.*")) || (line.matches("(?i).*chip-seq.*"))) {
            System.out.println("ASSAYTYPE IS: " + line);

            assayTypes.add(new AssayType("protein-DNA binding site identification", "nucleotide sequencing", "ChIP-Seq"));
            isaProtocolSection.put(0, investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).get(0).concat("\tlibrary construction\tnucleic acid sequencing"));
            isaProtocolSection.put(1, investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).get(1).concat("\tlibrary construction\tnucleic acid sequencing"));
            System.out.println("PROTOCOL INSERTION: " + isaProtocolSection.put(0, investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).get(0).concat("\tlibrary construction\tnucleic acid sequencing")));
        }

        return assayTypes;

    }


}
