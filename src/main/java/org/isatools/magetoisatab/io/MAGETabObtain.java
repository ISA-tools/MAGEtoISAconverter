package org.isatools.magetoisatab.io;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MAGETabObtain {

    public String userUrl;

    public MAGETabObtain() {
    }

    public void initialise() {
        DownloadUtils.createTmpDirectory();
        System.out.println("Enter ArrayExpress Accession Number: ");
        readInput();
    }


    /**
     * A Method that reads ask for an accession number and checks it is well formed.
     */
    public void readInput() {

        Scanner inputstr = new Scanner(System.in);

        try {
            doConversion(inputstr.nextLine(), "Data");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public File doConversion(String accessionNumber) throws Exception {
        return doConversion(accessionNumber, DownloadUtils.CONVERTED_DIRECTORY);
    }

    public File doConversion(String accessionNumber, String saveDirectory) throws Exception {

        DownloadUtils.CONVERTED_DIRECTORY = saveDirectory;

        String idfUrl, sdrfUrl;

        try {

            Pattern accnumregex = Pattern.compile("^E-[A-Z]{4,}-\\d+");
            Matcher accnummatcher = accnumregex.matcher(accessionNumber);

            if (accnummatcher.find()) {

                idfUrl = "http://www.ebi.ac.uk/arrayexpress/files/" + accessionNumber + "/" + accessionNumber + ".idf.txt";
                sdrfUrl = "http://www.ebi.ac.uk/arrayexpress/files/" + accessionNumber + "/" + accessionNumber + ".sdrf.txt";


                DownloadUtils.createDirectory(DownloadUtils.TMP_DIRECTORY + File.separator + accessionNumber);

                String idfDownloadLocation = DownloadUtils.TMP_DIRECTORY + File.separator + accessionNumber + File.separator + accessionNumber + ".idf.txt";
                String sdrfDownloadLocation = DownloadUtils.TMP_DIRECTORY + File.separator + accessionNumber + File.separator + accessionNumber + ".sdrf.txt";

                DownloadUtils.downloadFile(idfUrl, idfDownloadLocation);
                DownloadUtils.downloadFile(sdrfUrl, sdrfDownloadLocation);


                System.out.println(idfUrl);
                MAGETabIDFLoader idfloader = new MAGETabIDFLoader();
                idfloader.loadidfTab(idfDownloadLocation, accessionNumber);


                System.out.println(sdrfUrl);

                MAGETabSDRFLoader sdrfloader = new MAGETabSDRFLoader();
                sdrfloader.loadsdrfTab(sdrfDownloadLocation, accessionNumber);

                return new File(DownloadUtils.CONVERTED_DIRECTORY + File.separator + accessionNumber);
            } else {

                throw new Exception("Sorry, this does not seem to be a valid ArrayExpress accession number !");

            }

        } catch (IOException ioe) {
            System.out.println("Caught an IO exception :-o");
            ioe.printStackTrace();
        }

        return null;
    }


    public static void main(String[] argv) {
        MAGETabObtain mageReadFunction = new MAGETabObtain();
        if (argv.length > 0) {
            try {
                if (argv.length == 1) {
                    mageReadFunction.doConversion(argv[0]);
                } else {
                    mageReadFunction.doConversion(argv[0], argv[1]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mageReadFunction.initialise();
        }
    }

}