/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Content Registry 3
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Juhan Voolaid
 */

package eionet.rdfexport;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

/**
 * Rdf export main class. This provides running the functionality of this libreary on command line.
 *
 * @author Juhan Voolaid
 */
public class Execute {

    /** RDF mode. */
    public static final String RDF_MODE = "rdf";
    /** DB mode. */
    public static final String DB_MODE = "db";

    /**
     * Main method, that let's to execute functionality from GenerateRDF (mode="rdf") and ExportDB (mode="db").
     *
     * @param args
     *            The first element of the arguments is mode ("rdf" or "db"), the following are flags for each mode accordingly.
     */
    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            if (args[0].equalsIgnoreCase(RDF_MODE)) {
                handleRDF(args);
            } else if (args[0].equalsIgnoreCase(DB_MODE)) {
                handleDB(args);
            } else {
                System.out.println("Illegal arguments.");
                printHelp();
            }
        } else {
            System.out.println("No arguments specified.");
            printHelp();
        }
    }

    /**
     * Handles the "rdf" mode, which generates RDF output based on the input properties.
     *
     * <pre>
     * Primarily to demonstrate the use.
     * Flags:   -o <i>filename</i> - save the generated RDF in file.
     *          -f <i>filename</i> - load the properties from the specified file.
     *          -d <i>filename</i> - load the properties for the database connection from the specified file.
     *          -z - gzip the output.
     *          -i <i>identifier</i> - Only export the record with the identifier All other arguments are expected to be table names.
     * </pre>
     *
     * @param args
     */
    private static void handleRDF(String[] args) {
        ArrayList<String> unusedArgs;
        String[] tables;
        String identifier = null;
        String rdfPropFilename = "rdfexport.properties";
        String dbPropFilename = "database.properties";
        Boolean zipIt = false;
        OutputStream outStream = System.out;
        String outputFile = null;

        unusedArgs = new ArrayList<String>(args.length);

        for (int a = 1; a < args.length; a++) {
            if (args[a].equals("-z")) {
                zipIt = true;
            } else if (args[a].startsWith("-o")) {
                if (args[a].length() > 2)
                    outputFile = args[a].substring(2);
                else
                    outputFile = args[++a];
                if ("-".equals(outputFile))
                    outputFile = null; // Linux convention
            } else if (args[a].equals("-d")) {
                dbPropFilename = args[++a];
            } else if (args[a].startsWith("-d")) {
                dbPropFilename = args[a].substring(2);
            } else if (args[a].equals("-f")) {
                rdfPropFilename = args[++a];
            } else if (args[a].startsWith("-f")) {
                rdfPropFilename = args[a].substring(2);
            } else if (args[a].equals("-i")) {
                identifier = args[++a];
            } else if (args[a].startsWith("-i")) {
                identifier = args[a].substring(2);
            } else {
                unusedArgs.add(args[a]);
            }
        }
        try {
            Properties props = new Properties();
            Properties rdfProps = new Properties();
            props.load(new FileInputStream(dbPropFilename));
            // props.load(GenerateRDF.class.getClassLoader().getResourceAsStream(dbPropFilename));

            String driver = props.getProperty("driver");
            String dbUrl = props.getProperty("database");
            String userName = props.getProperty("user");
            String password = props.getProperty("password");

            Class.forName(driver).newInstance();
            Connection con = DriverManager.getConnection(dbUrl, userName, password);
            rdfProps.load(new FileInputStream(rdfPropFilename));
            // rdfProps.load(GenerateRDF.class.getClassLoader().getResourceAsStream(rdfPropFilename));

            if (outputFile != null) {
                outStream = new FileOutputStream(outputFile);
            }
            if (zipIt) {
                outStream = new GZIPOutputStream(outStream);
            }
            GenerateRDF r = new GenerateRDF(outStream, con, rdfProps);

            if (unusedArgs.size() == 0) {
                tables = r.getAllTables();
            } else {
                tables = new String[unusedArgs.size()];
                for (int i = 0; i < unusedArgs.size(); i++) {
                    tables[i] = (String) unusedArgs.get(i).toString();
                }
            }

            for (String table : tables) {
                r.exportTable(table, identifier);
            }
            r.exportDocumentInformation();

            r.writeRdfFooter();
            con.close();
            if (zipIt) {
                GZIPOutputStream g = (GZIPOutputStream) outStream;
                g.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the "db" mode, which exports the MS-Access database into RDF.
     *
     * <pre>
     * Primarily to demonstrate the use.
     * Flags: -p <i>filename</i> - save the discovered information as a properties file.
     *        -f <i>filename</i> - load the template properties from the specified file.
     *        -m <i>filename</i> - the name of the MS-Access file to investigate.
     * </pre>
     *
     * @param args
     */
    private static void handleDB(String[] args) {
        ArrayList<String> unusedArgs;
        String[] tables;
        String rdfPropFilename = "exportmdb.properties";
        String dbPropFilename = "exportmdb.properties";
        String mdbFilename = null;
        String writeProperties = null;

        unusedArgs = new ArrayList<String>(args.length);

        // Parse arguments.
        for (int a = 1; a < args.length; a++) {
            if (args[a].startsWith("-p")) {
                if (args[a].length() > 2)
                    writeProperties = args[a].substring(2);
                else
                    writeProperties = args[++a];
            } else if (args[a].equals("-d")) {
                dbPropFilename = args[++a];
            } else if (args[a].startsWith("-d")) {
                dbPropFilename = args[a].substring(2);
            } else if (args[a].startsWith("-f")) {
                if (args[a].length() > 2)
                    rdfPropFilename = args[a].substring(2);
                else
                    rdfPropFilename = args[++a];
            } else if (args[a].startsWith("-m")) {
                if (args[a].length() > 2)
                    mdbFilename = args[a].substring(2);
                else
                    mdbFilename = args[++a];
            } else {
                unusedArgs.add(args[a]);
            }
        }
        try {
            Properties props = new Properties();
            Properties rdfProps = new Properties();
            props.load(new FileInputStream(dbPropFilename));

            String driver = props.getProperty("driver");
            String dbUrl = props.getProperty("database");
            if (mdbFilename != null) {
                dbUrl = dbUrl.concat(mdbFilename);
            }
            String userName = props.getProperty("user");
            String password = props.getProperty("password");

            // String driver = "com.hxtt.sql.access.AccessDriver";
            // String dbUrl = "jdbc:access:/".concat(mdbFilename);
            // String userName = "user";
            // String password = "password";

            Class.forName(driver).newInstance();
            Connection con = DriverManager.getConnection(dbUrl, userName, password);
            rdfProps.load(new FileInputStream(rdfPropFilename));

            String vocabulary = rdfProps.getProperty("vocabulary");
            String baseurl = rdfProps.getProperty("baseurl");
            if (vocabulary == null || "".equals(vocabulary)) {
                if (baseurl == null) {
                    vocabulary = "#properties/";
                } else {
                    vocabulary = baseurl.concat("properties/");
                }
                rdfProps.setProperty("vocabulary", vocabulary);
            }
            // TODO: the 'vocabulary' property has to generated if it is not in the template file.
            // and then written to the properties file.
            rdfProps.setProperty("driver", driver);
            rdfProps.setProperty("database", dbUrl);
            rdfProps.setProperty("user", userName);
            rdfProps.setProperty("password", password);

            ExportDB inspector = new ExportDB(con, rdfProps);
            inspector.discoverTables();
            if (writeProperties != null) {
                FileOutputStream propOut = new FileOutputStream(writeProperties);
                rdfProps.store(propOut, "");
                propOut.close();
            } else {

                GenerateRDF exporter = new GenerateRDF(System.out, con, rdfProps);

                if (unusedArgs.size() == 0) {
                    tables = exporter.getAllTables();
                } else {
                    tables = new String[unusedArgs.size()];
                    for (int i = 0; i < unusedArgs.size(); i++) {
                        tables[i] = (String) unusedArgs.get(i).toString();
                    }
                }

                for (String table : tables) {
                    exporter.exportTable(table);
                }
                exporter.writeRdfFooter();
                con.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints help text to console.
     */
    private static void printHelp() {
        System.out.println("Usage: java -cp rdf-export-xx.jar eionet.rdfexport.Execute {mode} {flags}");
        System.out.println("If mode is 'rdf', generate rdf mode is selected and the following flags are in use: "
                + "[-o output_file] [-f rdf_properties_file] [-d database_properties_file] [-i identifier_to_export] [-z]");
        System.out.println("Flags: \t -o file name of the generated RDF in file (console output when not specified)");
        System.out.println("\t -f rdf export properties file (rdfexport.properties when not specified)");
        System.out.println("\t -d database connection properties (database.properties when not specified)");
        System.out.println("\t -i only export the record with the identifier, all other arguments are expected to be table names");
        System.out.println("\t -z gzip the output");
        System.out.println("If mode is 'db', export database in rdf mode is selected and the following flags are in use: "
                + "[-p output_file] [-f template_properties_file] [-d database_properties_file] [-m database_file]");
        System.out.println("Flags: \t -p save the discovered information as a properties file");
        System.out.println("\t -f load the template properties from the specified file");
        System.out.println("\t -d load the database properties from the specified file");
        System.out.println("\t -m the name of the database file to investigate");
    }
}
