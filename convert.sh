# Change this with your driver. It is added to the Java's class path, so, in case of multiple files, use the usual syntax
# "path:path:..."
#
#
CP="target/MAGETabtoISATab-jar-with-dependencies.jar"

MAGETAB_ACCESSION="E-BUGS-65"
CONVERSION_DIR="/Users/eamonnmaguire/Downloads/Conversion/"

java -Xms256m -Xmx1024m -XX:PermSize=64m -XX:MaxPermSize=128m\
     -cp "$CP" org.isatools.magetoisatab.io.MAGETabObtain
