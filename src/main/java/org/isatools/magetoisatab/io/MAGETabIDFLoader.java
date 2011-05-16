package org.isatools.magetoisatab.io;



import com.sun.tools.javac.util.Pair;
import org.apache.log4j.Logger;

import javax.swing.text.html.HTMLDocument;
import java.util.List;
import java.util.*;
import java.lang.*;
import java.net.*;
import java.io.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;



/**
* Created by the ISA team
*
* @author Philippe Rocca-Serra (proccaserra@gmail.com)
*         <p/>
*         Date: 02/03/2011
*         Time: 18:02
*/



/**
 A class to convert MAGE-TAB idf file to an ISA-TAB investigation file.
 The input can be either an ArrayExpress Accession number or the full address of a file on a local file system.
 The output is an ISA-TAB investigation file.
  TODO: implement testing structure of accession number s/E-\w+-\d+/
  TODO: exception handling such as "access denied" or "file not found"

 */

public class MAGETabIDFLoader {

    private static final Logger log = Logger.getLogger(MAGETabIDFLoader.class.getName());

    public static final Character TAB_DELIM = '\t';

    public String userUrl;


    public List<String> publicationLines;
    public List<String> factorLines;
    public List<String> investigationLines;
    public List<String> studyDesc;
    public List<String> designLines;
    public List<String> commentLines;
    public List<String> assaylines;
    public List<String> dateLines;
    public List<String> ontoLines;

    Map<InvestigationSections, List<String>> investigationSections;


    public MAGETabIDFLoader() {
        investigationSections = new HashMap<InvestigationSections, List<String>>();
    }

    public void loadidfTab(String url, String accnum) throws IOException {

        try {


           File file =  new File(url);

            boolean success = (new File("data/"+accnum)).mkdir();
                if (success) {
                System.out.println("Directory: " + accnum + " created");
            }

            if (file.exists()) {

            Scanner sc = new Scanner(file);

            String line;

            Pattern commentregex = Pattern.compile("^Comment");


            while (sc.hasNextLine()) {

                if ((line = sc.nextLine()) != null) {

                    Matcher commentmatcher = commentregex.matcher(line);
                    //Matcher qcmatcher = qcregex.matcher(line);

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
                        if (publicationLines == null)   {
                            publicationLines = new ArrayList<String>();
                        }
                         publicationLines.add(line);


                    } else if (line.startsWith("Publication")) {

                        line = line.replaceFirst("Publication", "Study Publication");
                        if (publicationLines == null)   {
                            publicationLines = new ArrayList<String>();
                        }
                         publicationLines.add(line);


                    } else if (line.startsWith("Experimental Factor")) {

                        line = line.replaceFirst("Experimental Factor", "Study Factor");
                        line = line.replaceFirst("Study Factor Term", "Study Factor Type Term");
                        if (factorLines == null) {
                            factorLines = new ArrayList<String>();
                        }
                        factorLines.add(line);
                    } else if (line.startsWith("Experimental Design")) {

                        line = line.replaceFirst("Experimental Design", "Study Design Type");
                        if (designLines == null) {
                            designLines = new ArrayList<String>();
                        }
                        designLines.add(line);


                    } else if (line.startsWith("SDRF File")) {
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


                    } else if  (line.startsWith("Term Source Name")) {

                        if ( ontoLines ==null ) {

                            ontoLines = new ArrayList<String>();
                        }
                        ontoLines.add(line);

                    } else if  (line.startsWith("Term Source File")) {

                        if ( ontoLines ==null ) {

                            ontoLines = new ArrayList<String>();
                        }
                        ontoLines.add(line);

                    } else if  (line.startsWith("Term Source Version")) {

                        if ( ontoLines ==null ) {

                            ontoLines = new ArrayList<String>();
                        }
                        ontoLines.add(line);


                    } else if (commentmatcher.find()) {

                        System.out.println("comment line: " + line + "\n");
                        if (commentLines == null) {
                            commentLines = new ArrayList<String>();
                        }
                        commentLines.add(line);


                    } else {
                        System.out.println("regular line: " + line + "\n");
                    }

                } else {

                    sc.close();
                }
            }

            PrintStream invPs = new PrintStream(new File("data/"+accnum+"/i_"+accnum+"_investigation.txt"));
            invPs.println("ONTOLOGY SOURCE REFERENCE");
                for (String ontoLine : ontoLines) {

                    invPs.println(ontoLine);
                }

           invPs.println("Term Source Description\n" +
                    "INVESTIGATION\n" +
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
                    "Study Identifier"+"\t"+accnum);


                for (String investigationLine : investigationLines) {
                    invPs.println(investigationLine);
                }

            invPs.println("Study Submission Date"+"\t"+"2011-03-01");

                for (String dateLine : dateLines) {
                    invPs.println(dateLine);
                }

                for (String aStudyDesc : studyDesc) {
                    invPs.println(aStudyDesc);
                }

            invPs.println("Study File Name" + "\t" + "s_" + accnum + "_studysample.txt");

            invPs.println("STUDY DESIGN DESCRIPTORS");

            for (int i = 0; i < designLines.size()-1; i++) {
                invPs.println(designLines.get(i));
            }

            invPs.println(
                    "Study Design Type Term Accession Number\n" +
                    "Study Design Type Term Source REF");


            // System.out.println("there are" + protocolLines.length + "lines related to protocols\n");

            invPs.println("STUDY PUBLICATIONS");

            Map<Integer,String> IsaPublicationSection = new HashMap<Integer,String>();

              //HashMap initialization to define canonical block structure
              IsaPublicationSection.put(0,"Study Publication PubMed ID");
              IsaPublicationSection.put(1,"Study Publication DOI");
              IsaPublicationSection.put(2,"Study Publication Authors List");
              IsaPublicationSection.put(3,"Study Publication Title");
              IsaPublicationSection.put(4,"Study Publication Status");
              IsaPublicationSection.put(5,"Study Publication Status Term Accession Number");
              IsaPublicationSection.put(6,"Study Publication Status Term Source REF");


             if (publicationLines.size()>0) {
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
                     if (publicationLine.contains("Status")) {
                         IsaPublicationSection.put(4, publicationLine);
                     }
                     if (publicationLine.contains("Term")) {
                         IsaPublicationSection.put(5, publicationLine);
                     }
                     if (publicationLine.contains("Accession")) {
                         IsaPublicationSection.put(6, publicationLine);
                     }
                 }
             }

            //we now output the Publication Section of an ISA Study
                for (Map.Entry<Integer, String> e : IsaPublicationSection.entrySet())
                    invPs.println(e.getValue());


           /* for (int i = 0; i < publicationLines.size(); i++) {
                invPs.println(publicationLines.get(i));
            }
*/


            // Now Creating the Factor Section
            invPs.println("STUDY FACTORS");

                for (String factorLine : factorLines) {
                    invPs.println(factorLine);
                }


            //Now creating the Assay Section:
            invPs.println("STUDY ASSAYS");


             StringBuffer alpha = getMeasurementAndTech(designLines.get(0)).fst;
                          invPs.println(alpha);

            invPs.println("Study Assay Measurement Type Term Accession Number\n" +
                    "Study Assay Measurement Type Term Source REF");


             StringBuffer beta = getMeasurementAndTech(designLines.get(0)).snd;

             invPs.println(beta);


            invPs.println("Study Assay Technology Type Term Accession Number\n" +
                    "Study Assay Technology Type Term Source REF\n" +
                    "Study Assay Technology Platform");


            invPs.println("Study Assay File Name" + "\t" + "a_" + accnum + "_assay.txt");




            //Now creating the Protocol section
            invPs.println("STUDY PROTOCOLS");



             Map<Integer,String> IsaProtocolSection = new HashMap<Integer,String>();

              //HashMap initialization to define canonical block structure
              IsaProtocolSection.put(0,"Study Protocol Name");
              IsaProtocolSection.put(1,"Study Protocol Type");
              IsaProtocolSection.put(2,"Study Protocol Type Term Accession Number");
              IsaProtocolSection.put(3,"Study Protocol Type Term Source REF");
              IsaProtocolSection.put(4,"Study Protocol Description");
              IsaProtocolSection.put(5,"Study Protocol URI");
              IsaProtocolSection.put(6,"Study Protocol Version");
              IsaProtocolSection.put(7,"Study Protocol Parameters Name");
              IsaProtocolSection.put(8,"Study Protocol Parameters Name Term Accession Number");
              IsaProtocolSection.put(9,"Study Protocol Parameters Name Term Source REF");
              IsaProtocolSection.put(10,"Study Protocol Components Name");
              IsaProtocolSection.put(11,"Study Protocol Components Type");
              IsaProtocolSection.put(12,"Study Protocol Components Type Term Accession Number");
              IsaProtocolSection.put(13,"Study Protocol Components Type Term Source REF");

             if (investigationSections.get(InvestigationSections.STUDY_PROTOCOL_SECTION).size() >0) {

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

                         String tempComponent = protocolLine.replaceAll("Software", "Component Name");
                         tempComponent = protocolLine.replaceAll("Hardware", "Component Name");
                         IsaProtocolSection.put(10, tempComponent);
                     }
                 }
            }

                //we now output the Protocol Section of an ISA Study
                for (Map.Entry<Integer, String> e : IsaProtocolSection.entrySet())
                    invPs.println(e.getValue());



            // Let's now deal with the Contact Information Section

            invPs.println("STUDY CONTACTS");

            Map<Integer,String> IsaContactSection = new HashMap<Integer,String>();

              //HashMap initialization to define canonical block structure
              IsaContactSection.put(0,"Study Person Last Name");
              IsaContactSection.put(1,"Study Person First Name");
              IsaContactSection.put(2,"Study Person Mid Initials");
              IsaContactSection.put(3,"Study Person Email");
              IsaContactSection.put(4,"Study Person Phone");
              IsaContactSection.put(5,"Study Person Fax");
              IsaContactSection.put(6,"Study Person Address");
              IsaContactSection.put(7,"Study Person Affiliation");
              IsaContactSection.put(8,"Study Person Roles");
              IsaContactSection.put(9,"Study Person Roles Term Accession Number");
              IsaContactSection.put(10,"Study Person Roles Term Source REF");


             if (investigationSections.get(InvestigationSections.STUDY_CONTACT_SECTION).size()>0) {

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
             } else { System.out.println("life sucks\n");}


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
     * @param line
     * @return  A Pair of Strings containing the Measurement and Technology Types to be output
     * TODO: rely on an xml configuration file to instead of hard coded values -> easier to maintain in case of changes in ArrayExpress terminology
     */

    private Pair<StringBuffer, StringBuffer> getMeasurementAndTech(String line) {

        List<String> measurements = new ArrayList<String>();
        List<String> technologies = new ArrayList<String>();

        StringBuffer MeasurementsAsString = new StringBuffer();
        StringBuffer TechnologiesAsString = new StringBuffer();

        Pair<StringBuffer, StringBuffer> resultMtTt = new Pair<StringBuffer, StringBuffer>(MeasurementsAsString, TechnologiesAsString);



        if (line.matches("(?i).*ChIP-Chip.*")) {

            System.out.println("@@@" + line);
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


        MeasurementsAsString.append("Study Assay Measurement Type\t");
        TechnologiesAsString.append("Study Assay Technology Type\t");

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


