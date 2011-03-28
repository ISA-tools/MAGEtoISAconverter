package org.isatools.magetoisatab.io;



import com.sun.tools.javac.util.Pair;
import org.apache.log4j.Logger;

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
    public List<String> protocolLines;
    public List<String> contactLines;
    public List<String> publicationLines;
    public List<String> factorLines;
    public List<String> investigationLines;
    public List<String> studyDesc;
    public List<String> designLines;
    public List<String> commentLines;
    public List<String> assaylines;
    public List<String> dateLines;
    public List<String> ontoLines;


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
                        if (protocolLines == null) {
                            protocolLines = new ArrayList<String>();
                        }
                        protocolLines.add(line);

                    } else if (line.startsWith("Experiment Desc")) {

                        line = line.replaceFirst("Experiment", "Study");
                        if (studyDesc == null) {
                            studyDesc = new ArrayList<String>();
                        }
                        studyDesc.add(line);

                    } else if (line.startsWith("Person")) {

                        line = line.replaceFirst("Person", "Study Person");
                        if (contactLines == null) {
                            contactLines = new ArrayList<String>();
                        }

                        contactLines.add(line);

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
                        for (int i = 0; i < ontoLines.size(); i++) {

                            invPs.println(ontoLines.get(i));
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


            for (int i = 0; i < investigationLines.size(); i++) {
                invPs.println(investigationLines.get(i));
            }

            invPs.println("Study Submission Date"+"\t"+"2011-03-01");

            for (int i = 0; i < dateLines.size(); i++) {
                invPs.println(dateLines.get(i));
            }

            for (int i = 0; i < studyDesc.size(); i++) {
                invPs.println(studyDesc.get(i));
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

            for (int i = 0; i < publicationLines.size(); i++) {
                invPs.println(publicationLines.get(i));
            }


            invPs.println("STUDY FACTORS");

            for (int i = 0; i < factorLines.size(); i++) {
                invPs.println(factorLines.get(i));
            }



            invPs.println(
                    "Study Factor Type Term Accession Number\n" +
                            "Study Factor Type Term Source REF");

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

            invPs.println("STUDY PROTOCOLS");

            System.out.println(protocolLines.get(0));
             System.out.println(protocolLines.get(1));
             System.out.println(protocolLines.get(2));
             System.out.println(protocolLines.get(3));
             invPs.println(protocolLines.get(0));
             invPs.println(protocolLines.get(1));


            invPs.println("Study Protocol Type Term Accession Number\n" +
                    "Study Protocol Type Term Source REF");


             invPs.println(protocolLines.get(2));
                     invPs.println("Study Protocol URI\n" +
                    "Study Protocol Version");

               invPs.println(protocolLines.get(3));

                     invPs.println("Study Protocol Parameters Name Term Accession Number\n" +
                    "Study Protocol Parameters Name Term Source REF\n" +
                    "Study Protocol Components Name\n" +
                    "Study Protocol Components Type\n" +
                    "Study Protocol Components Type Term Accession Number\n" +
                    "Study Protocol Components Type Term Source REF");



            invPs.println("STUDY CONTACTS   ");

            for (int i = 0; i < contactLines.size(); i++) {
                invPs.println(contactLines.get(i));
            }


            invPs.println(
                    "Study Person Role Term Accession Number\n" +
                            "Study Person Role Term Source REF");

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

        } else if (line.matches(".*methylation profiling by array.*")) {

            measurements.add("DNA methylation profiling");
            technologies.add("DNA microarray");

        } else if (line.matches(".*comparative genomic hybridization by array.*")) {

            measurements.add("comparative genomic hybridization");
            technologies.add("DNA microarray");

        } else if (line.matches(".*genotyping by array.*")) {

            measurements.add("SNP analysis");
            technologies.add("DNA microarray");

        } else if (line.matches(".*transcription profiling by high throughput sequencing.*")) {

            measurements.add("transcription profiling");
            technologies.add("nucleotide sequencing");

        } else if (line.matches(".*ChIP-Seq.*")) {

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


