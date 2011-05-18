package org.isatools.magetoisatab.io;


import com.sun.tools.javac.util.Pair;
import org.apache.log4j.Logger;

import javax.swing.text.html.HTMLDocument;
import java.beans.DesignMode;
import java.util.List;
import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;



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

public class MAGETabIDFLoader {

    private static final Logger log = Logger.getLogger(MAGETabIDFLoader.class.getName());

    public static final Character TAB_DELIM = '\t';

    public List<String> investigationLines;

    public Map<Integer, String> IsaPublicationSection = new HashMap<Integer, String>() {
        {
            //HashMap initialization to define canonical block structure
            put(0, "Study Publication PubMed ID");
            put(1, "Study Publication DOI");
            put(2, "Study Publication Authors List");
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

    public List<String> ontoLines = new ArrayList<String>() {
        {
            add("Term Source Name");
            add("Term Source File");
            add("Term Source Version");
            add("Term Source Description");
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


    public   Map<Integer, String> IsaProtocolSection = new HashMap<Integer, String>(){
        {
          //HashMap initialization to define canonical block structure
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

    Map<InvestigationSections, List<String>> investigationSections;





    public MAGETabIDFLoader() {
        investigationSections = new HashMap<InvestigationSections, List<String>>();
    }


    public void loadidfTab(String url, String accnum) throws IOException {

        try {

            File file = new File(url);

            boolean success = (new File("data/" + accnum)).mkdirs();
            if (success) {
                System.out.println("Directory: " + accnum + " created");
            }

            if (file.exists()) {

                Scanner sc = new Scanner(file);

                String line;

                while (sc.hasNextLine()) {

                    if ((line = sc.nextLine()) != null) {

                        if (line.startsWith("Protocol")) {

                            line = line.replaceFirst("Protocol", "Study Protocol");
                            if (!investigationSections.containsKey(InvestigationSections.STUDY_PROTOCOL_SECTION)) {
                                investigationSections.put(InvestigationSections.STUDY_PROTOCOL_SECTION, new ArrayList<String>());
                            }

                            investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).add(line);


                        } else if (line.startsWith("Experiment Desc")) {

                            line = line.replaceFirst("Experiment", "Study");
                            if (studyDesc == null) {
                                studyDesc = new ArrayList<String>();
                            }
                            studyDesc.add(line);

                        } else if (line.startsWith("Person")) {

                            line = line.replaceFirst("Person", "Study Person");
                            if (!investigationSections.containsKey(InvestigationSections.STUDY_CONTACT_SECTION)) {
                                investigationSections.put(InvestigationSections.STUDY_CONTACT_SECTION, new ArrayList<String>());
                            }

                            investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION).add(line);

                        } else if (line.startsWith("PubMed")) {
                            line = line.replaceFirst("PubMed", "Study PubMed");
                            if (publicationLines == null) {
                                publicationLines = new ArrayList<String>();
                            }
                            publicationLines.add(line);
                        } else if (line.startsWith("Publication")) {

                            line = line.replaceFirst("Publication", "Study Publication");
                            if (publicationLines == null) {
                                publicationLines = new ArrayList<String>();
                            }
                            publicationLines.add(line);
                        }


                        //Now Dealing with element from Protocol Section

                        else if (line.startsWith("Experimental Factor Name")) {
                            line = line.toLowerCase();
                            line = line.replaceFirst("experimental factor name", "Study Factor Name");
                            factorLines.set(0, line);
                        } else if (line.startsWith("Experimental Factor Type")) {
                            line = line.toLowerCase();
                            line = line.replaceFirst("experimental factor type", "Study Factor Type");
                            factorLines.set(1, line);
                        } else if (line.endsWith("Factor Term Accession")) {
                            line = line.replaceFirst("Experimental", "Study");
                            factorLines.set(2, line);
                        } else if (line.endsWith("Factor Term Source REF")) {
                            line = line.replaceFirst("Experimental", "Study");
                            factorLines.set(3, line);
                        } else if ((line.contains("Experimental Design")) && (!(line.contains("Experimental Design Term")))) {
                            line = line.replaceFirst("Experimental Design", "Study Design Type");
                            designLines.set(0, line);
                        }
//                    else if (line.startsWith("Experimental Design Term Accession")) {
//                        line=line.replaceAll("Experimental Design", "Study Design Type");
//                        designLines.set(1,line);
//                    }
//
//                    else if (line.startsWith("Experimental Design Term Source")) {
//                        line=line.replaceAll("Experimental Design", "Study Design Type");
//                        designLines.set(2,line);
//                    }

                        //This bit is used to recover information for setting ISA MT and TT in case no Experimental Design is found
                        else if (line.startsWith("Comment[AEExperimentType")) {
                            System.out.println("Alternative Design Tag found at: " + line);
                            line = line.replace("Comment[AEExperimentType]", "Study Design Type");
                            designLines.set(0, line);
                        }


                        //
                        else if (line.startsWith("SDRF File")) {
                            line = line.replaceFirst("SDRF File", "Study Assay File Name");
                            if (assaylines == null) {
                                assaylines = new ArrayList<String>();
                            }
                            assaylines.add(line);
                        } else if (line.startsWith("Investigation")) {
                            line = line.replaceFirst("Investigation", "Study");
                            if (investigationLines == null) {
                                investigationLines = new ArrayList<String>();
                            }
                            investigationLines.add(line);
                        } else if (line.startsWith("Public")) {
                            line = line.replaceFirst("Public", "Study Public");
                            if (dateLines == null) {
                                dateLines = new ArrayList<String>();
                            }
                            dateLines.add(line);
                        }


                        // looks for information about Ontology and Terminologies used in MAGE-TAB document
                        else if (line.startsWith("Term Source Name")) {

                            ontoLines.set(0, line);

                        } else if (line.startsWith("Term Source File")) {

                            ontoLines.set(2, line);

                        } else if (line.startsWith("Term Source Version")) {

                            ontoLines.set(1, line);
                        } else if (line.startsWith("Term Source Description")) {

                            ontoLines.set(3, line);

                        }

                    } else {

                        sc.close();
                    }
                }

                PrintStream invPs = new PrintStream(new File("data/" + accnum + "/i_" + accnum + "_investigation.txt"));
                invPs.println("ONTOLOGY SOURCE REFERENCE");
                for (String ontoLine : ontoLines) {

                    invPs.println(ontoLine);
                }

                invPs.println("INVESTIGATION\n" +
                        "Investigation Identifier\n" +
                        "Investigation Title\n" +
                        "Investigation Description\n" +
                        "Investigation Submission Date\n" +
                        "Investigation Public Release Date\n" +
                        "INVESTIGATION PUBLICATIONS\n" +
                        "Investigation PubMed ID\n" +
                        "Investigation Publication DOI\n" +
                        "Investigation Publication Author list\n" +
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

                invPs.println("Study Submission Date" + "\t" + "2011-03-01");

                for (String dateLine : dateLines) {
                    invPs.println(dateLine);
                }

                for (String aStudyDesc : studyDesc) {
                    invPs.println(aStudyDesc);
                }

                invPs.println("Study File Name" + "\t" + "s_" + accnum + "_studysample.txt");

                invPs.println("STUDY DESIGN DESCRIPTORS");

                for (int i = 0; i < designLines.size(); i++) {

                    invPs.println(designLines.get(i));
                }


                invPs.println("STUDY PUBLICATIONS");




                if (publicationLines.size() > 0) {
                    for (String publicationLine : publicationLines) {

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
                        if (publicationLine.endsWith("Status")) {
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
                    invPs.println(factorLine);
                }


                //Now creating the Assay Section:
                invPs.println("STUDY ASSAYS");


                // We are now trying to get the Measurement and Technology Type from MAGE annotation Experimental Design Type
                //System.out.println("expeirmental design is :" + designLines.get(0));
                StringBuffer alpha = getMeasurementAndTech(designLines.get(0)).fst;
                StringBuffer beta = getMeasurementAndTech(designLines.get(0)).snd;


                // If this fails, we are falling back on checking MAGE-TAB Comment[AEExperimentType] line
                //StringBuffer


                invPs.println(alpha);
                invPs.println("Study Assay Measurement Type Term Accession Number\n" +
                        "Study Assay Measurement Type Term Source REF");

                invPs.println(beta);
                invPs.println("Study Assay Technology Type Term Accession Number\n" +
                        "Study Assay Technology Type Term Source REF\n" +
                        "Study Assay Technology Platform");


                invPs.println("Study Assay File Name" + "\t" + "a_" + accnum + "_assay.txt");


                //Now creating the Protocol section
                invPs.println("STUDY PROTOCOLS");


               if (investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).size() > 0) {

                    for (String protocolLine : investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION)) {

                        if (protocolLine.contains("Name")) {
                            IsaProtocolSection.put(0, protocolLine);
                        }

                        if (protocolLine.contains("Type")) {

                            IsaProtocolSection.put(1, protocolLine);
                        }
                        if (protocolLine.contains("Accession")) {
                            String tempAcc = protocolLine.replaceAll("Term Accession", "Type Term Accession");
                            IsaProtocolSection.put(2, tempAcc);
                        }
                        if (protocolLine.contains("Term Source")) {
                            String tempSource = protocolLine.replaceAll("Term Source", "Type Term Source");
                            IsaProtocolSection.put(3, tempSource);
                        }
                        if (protocolLine.contains("Description")) {
                            IsaProtocolSection.put(4, protocolLine);
                        }

                        if (protocolLine.endsWith("Parameters")) {

                            String tempParam = protocolLine.replaceAll("Parameters", "Parameters Name");
                            IsaProtocolSection.put(5, tempParam);
                        }

                        if ((protocolLine.contains("Software")) || (protocolLine.contains("Hardware"))) {

                            String tempComponent = protocolLine.replaceAll("Software", "Components Name");
                            tempComponent = tempComponent.replaceAll("Hardware", "Components Name");
                            IsaProtocolSection.put(10, tempComponent);
                        }
                    }
                }

                //we now output the Protocol Section of an ISA Study
                for (Map.Entry<Integer, String> e : IsaProtocolSection.entrySet())
                    invPs.println(e.getValue());


                // Let's now deal with the Contact Information Section

                invPs.println("STUDY CONTACTS");

                Map<Integer, String> IsaContactSection = new HashMap<Integer, String>();

                //HashMap initialization to define canonical block structure
                IsaContactSection.put(0, "Study Person Last Name");
                IsaContactSection.put(1, "Study Person First Name");
                IsaContactSection.put(2, "Study Person Mid Initials");
                IsaContactSection.put(3, "Study Person Email");
                IsaContactSection.put(4, "Study Person Phone");
                IsaContactSection.put(5, "Study Person Fax");
                IsaContactSection.put(6, "Study Person Address");
                IsaContactSection.put(7, "Study Person Affiliation");
                IsaContactSection.put(8, "Study Person Roles");
                IsaContactSection.put(9, "Study Person Roles Term Accession Number");
                IsaContactSection.put(10, "Study Person Roles Term Source REF");


                if (investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION).size() > 0) {

                    for (String contactLine : investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION)) {

                        if (contactLine.contains("Last")) {
                            IsaContactSection.put(0, contactLine);
                        }
                        if (contactLine.contains("First")) {
                            IsaContactSection.put(1, contactLine);
                        }
                        if (contactLine.contains("Mid")) {
                            IsaContactSection.put(2, contactLine);
                        }
                        if (contactLine.contains("Email")) {
                            IsaContactSection.put(3, contactLine);
                        }
                        if (contactLine.contains("Phone")) {
                            IsaContactSection.put(4, contactLine);
                        }
                        if (contactLine.contains("Fax")) {
                            IsaContactSection.put(5, contactLine);
                        }
                        if (contactLine.contains("Address")) {
                            IsaContactSection.put(6, contactLine);
                        }
                        if (contactLine.contains("Affiliation")) {
                            IsaContactSection.put(7, contactLine);
                        }
                        if ((contactLine.contains("Roles") && !(contactLine.contains("Roles Term")))) {

                            IsaContactSection.put(8, contactLine);
                        }
                        if (contactLine.contains("Roles Term Accession")) {

                            IsaContactSection.put(9, contactLine);
                        }
                        if (contactLine.contains("Roles Term Source")) {

                            IsaContactSection.put(10, contactLine);
                        }


                    }
                } else {
                    System.out.println("life sucks\n");
                }


                //we now output the Contact Section of an ISA Study
                for (Map.Entry<Integer, String> e : IsaContactSection.entrySet())
                    invPs.println(e.getValue());


            } else {
                System.out.println("ERROR: File not found");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * A method that uses MAGE-TAB Experiment Design information to deduce ISA Measurement and Technology Types.
     *
     * @param line
     * @return A Pair of Strings containing the Measurement and Technology Types to be output
     *         TODO: rely on an xml configuration file to instead of hard coded values -> easier to maintain in case of changes in ArrayExpress terminology
     */

    private Pair<StringBuffer, StringBuffer> getMeasurementAndTech(String line) {

        List<String> measurements = new ArrayList<String>();
        List<String> technologies = new ArrayList<String>();

        StringBuffer MeasurementsAsString = new StringBuffer();
        StringBuffer TechnologiesAsString = new StringBuffer();

        MeasurementsAsString.append("Study Assay Measurement Type\t");
        TechnologiesAsString.append("Study Assay Technology Type\t");

        Pair<StringBuffer, StringBuffer> resultMtTt = new Pair<StringBuffer, StringBuffer>(MeasurementsAsString, TechnologiesAsString);


        if (line.matches("(?i).*ChIP-Chip.*")) {

            measurements.add("protein-DNA binding site identification");
            technologies.add("DNA microarray");

        } else if (line.matches(".*transcription profiling by array.*")) {

            measurements.add("transcription profiling");
            technologies.add("DNA microarray");

        } else if (line.matches("(?i).*methylation profiling by array.*")) {

            measurements.add("DNA methylation profiling");
            technologies.add("DNA microarray");

        } else if (line.matches("(?i).*comparative genomic hybridization by array.*")) {

            measurements.add("comparative genomic hybridization");
            technologies.add("DNA microarray");

        } else if (line.matches(".*genotyping by array.*")) {

            measurements.add("SNP analysis");
            technologies.add("DNA microarray");

        } else if (line.matches("(?i).*transcription profiling by high throughput sequencing.*")) {

            measurements.add("transcription profiling");
            technologies.add("nucleotide sequencing");

        } else if (line.matches("(?i).*ChIP-Seq.*")) {

            measurements.add("protein-DNA binding site identification");
            technologies.add("nucleotide sequencing");

        }


        int val = 0;

        for (String value : measurements) {

            System.out.println(measurements.get(val));
            MeasurementsAsString.append(value);
            if (val != measurements.size() - 1) {
                MeasurementsAsString.append("\t");
            }
            val++;
        }


        int j = 0;

        for (String value : technologies) {

            System.out.println(technologies.get(j));
            TechnologiesAsString.append(value);
            if (j != technologies.size() - 1) {
                TechnologiesAsString.append("\t");
            }
            j++;
        }


        return resultMtTt;

    }


}


