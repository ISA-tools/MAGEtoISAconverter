package org.isatools.magetoisatab.io;

public enum InvestigationSections {

    STUDY_PROTOCOL_SECTION("STUDY PROTOCOLS",
            new String[]{"Study Protocol Name",
                         "Study Protocol Type",
                         "Study Protocol Type Term Accession",
                         "Study Protocol Type Term Source REF",
                         "Study Protocol Description",
                         "Study Protocol URI",
                         "Study Protocol Version",
                         "Study Protocol Parameters Name",
                         "Study Protocol Parameters Name Term Accession Number",
                         "Study Protocol Parameters Name Term Source REF",
                         "Study Protocol Components Name",
                         "Study Protocol Components Type",
                         "Study Protocol Components Type Term Accession Number",
                         "Study Protocol Components Type Term Source REF"}),

    STUDY_FACTOR_SECTION("STUDY FACTORS",
            new String[]{"Study Factor Name",
                         "Study Factor Type",
                         "Study Factor Type Term Accession",
                         "Study Factor Type Term Source REF"})
    ,
    STUDY_CONTACT_SECTION("STUDY CONTACTS",
            new String[]{"blah", "blah2"}),



     STUDY_PUBLICATION_SECTION("STUDY PUBLICATIONS",
            new String[]{"Study PubMed ID",
                    "Study Publication DOI",
                    "Study Publication Author List",
                    "Study Publication Title",
                    "Study Publication Status",
                    "Study Publication Status Term Accession Number",
                    "Study Publication Status Term Source REF"});

    private String sectionName;

    private String[] sections;

    InvestigationSections(String sectionName, String[] sections) {
        this.sectionName = sectionName;
        this.sections = sections;
    }

    public String[] getSections() {
        return sections;
    }

    public String getSectionName() {
        return sectionName;
    }
}
