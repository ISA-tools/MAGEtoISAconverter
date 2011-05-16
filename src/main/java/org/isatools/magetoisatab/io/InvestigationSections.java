package org.isatools.magetoisatab.io;

public enum InvestigationSections {

    STUDY_PROTOCOL_SECTION("STUDY PROTOCOLS",
            new String[]{"Study Protocol Name", "Study Protocol Type"}),

    STUDY_CONTACT_SECTION("STUDY CONTACTS", new String[]{"blah", "blah2"});

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
