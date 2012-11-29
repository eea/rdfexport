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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;

/**
 * RDF export main class. Executed from command line.
 */
public class Execute {

    /** Default file path of the database connection properties file. */
    private static final String DEFAULT_DB_PROP_FILE_PATH = "database.properties";

    /** Default file path of the RDF export properties file. */
    private static final String DEFAULT_RDF_PROP_FILE_PATH = "rdfexport.properties";

    /** Will be used as user input scanner from command line. */
    protected static final Scanner USER_INPUT = new Scanner(System.in);

    /** List of unrecognized command line arguments. They will be interpreted as names of tables to export. */
    private ArrayList<String> unusedArguments = new ArrayList<String>();

    /** If true, the generated RDF output shall be zipped. */
    private boolean zipOutput = false;

    /** Path to the file where the RDF shall be generated into. */
    private String outputFilePath = null;

    /** Path to the database connection properties file. */
    private String dbPropFilePath = null;

    /** Path to the RDF export properties file. */
    private String rdfPropFilePath = null;

    /** The primary-key value of the row to export, if only one row from one table is to be exported. */
    private String rowId = null;

    /** If true, the tables and primary/foreign keys shall be auto-discovered, disregarding the ones given in properties file. */
    private boolean selfExplore = false;

    /** Path to the file where the auto-discovered tables and generated queries shall be written into, if requested so. */
    private String propsOutputFilePath = null;

    /** The properties to be loaded from database connection properties file. */
    private Properties dbProps = new Properties();

    /** The properties to be loaded from RDF export properties file. */
    private Properties rdfProps = new Properties();

    /** MS-Access to export. Concatenated to the value of "database" property from database connection properties file. */
    private String msAccessFilePath;

    /** The URI of the vocabulary of the RDF output that shall be generated. */
    private String vocabularyUri = null;

    /** The XML base URI of the generated RDF output. */
    private String baseUri = null;

    /** If true, the auto-disovery mode will ask for each table and foreign key whether to export it. */
    private boolean interActiveMode = false;

    /**
     * Constructor.
     *
     * @param args
     *            - command line arguments.
     * @throws IOException
     *             - if anything goes wrong with reading the properties and/or writing the output
     */
    private Execute(String args[]) throws IOException {

        parseArguments(args);

        if (dbPropFilePath == null || dbPropFilePath.isEmpty()) {
            dbPropFilePath = DEFAULT_DB_PROP_FILE_PATH;
        }

        if (rdfPropFilePath == null || rdfPropFilePath.isEmpty()) {
            rdfPropFilePath = DEFAULT_RDF_PROP_FILE_PATH;
        }

        Execute.loadProperties(dbProps, dbPropFilePath);
        Execute.loadProperties(rdfProps, rdfPropFilePath);

        vocabularyUri = rdfProps.getProperty("vocabulary");
        baseUri = rdfProps.getProperty("baseurl");
        if (vocabularyUri == null || vocabularyUri.isEmpty()) {

            // TODO: The 'vocabulary' property has to be generated if it is not in the template file.
            // and then written to the properties file.

            if (baseUri == null || baseUri.isEmpty()) {
                vocabularyUri = "#properties/";
            } else {
                vocabularyUri = baseUri.concat("properties/");
            }
            rdfProps.setProperty("vocabulary", vocabularyUri);
        }
    }

    /**
     * Parses the given command line arguments, and sets the corresponding fields of this object.
     *
     * @param args
     */
    private void parseArguments(String[] args) {

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-x") || args[i].equals("-xa")) {
                selfExplore = true;
                if (args[i].equals("-xa")) {
                    interActiveMode = true;
                }
            } else if (args[i].equals("-z")) {
                zipOutput = true;
            } else if (args[i].startsWith("-p")) {
                if (args[i].length() > 2) {
                    propsOutputFilePath = args[i].substring(2);
                } else {
                    propsOutputFilePath = args[++i];
                }
            } else if (args[i].startsWith("-m")) {
                if (args[i].length() > 2) {
                    msAccessFilePath = args[i].substring(2);
                } else {
                    msAccessFilePath = args[++i];
                }
            } else if (args[i].startsWith("-o")) {
                if (args[i].length() > 2) {
                    outputFilePath = args[i].substring(2);
                } else {
                    outputFilePath = args[++i];
                }
                if ("-".equals(outputFilePath)) {
                    outputFilePath = null; // Linux convention
                }
            } else if (args[i].equals("-d")) {
                dbPropFilePath = args[++i];
            } else if (args[i].startsWith("-d")) {
                dbPropFilePath = args[i].substring(2);
            } else if (args[i].equals("-f")) {
                rdfPropFilePath = args[++i];
            } else if (args[i].startsWith("-f")) {
                rdfPropFilePath = args[i].substring(2);
            } else if (args[i].equals("-i")) {
                rowId = args[++i];
            } else if (args[i].startsWith("-i")) {
                rowId = args[i].substring(2);
            } else {
                unusedArguments.add(args[i]);
            }
        }
    }

    /**
     * The core method that performs the auto-discovery of tables/keys (if requested) and generates the RDF. Assumes input
     * properties and command line arguments have been processed, and corresponding fields set in the object.
     *
     * @throws SQLException
     * @throws IOException
     */
    private void run() throws SQLException, IOException {

        Connection conn = null;
        OutputStream outputStream = System.out;
        try {
            conn = getConnection();

            if (selfExplore == true) {
                ExploreDB dbExplorer = new ExploreDB(conn, rdfProps, interActiveMode);
                dbExplorer.discoverTables();

                if (propsOutputFilePath != null && !propsOutputFilePath.isEmpty()) {
                    Execute.outputProperties(rdfProps, propsOutputFilePath);
                    return;
                }
            }

            if (outputFilePath != null && !outputFilePath.isEmpty()) {
                outputStream = new FileOutputStream(outputFilePath);
            }

            if (zipOutput == true) {
                outputStream = new GZIPOutputStream(outputStream);
            }

            GenerateRDF exporter = new GenerateRDF(outputStream, conn, rdfProps);

            List<String> tablesToExport = unusedArguments.isEmpty() ? Arrays.asList(exporter.getAllTables()) : unusedArguments;

            // if (rowId == null || rowId.isEmpty()){
            // System.out.println("Exporting these tables: " + tablesToExport);
            // }
            // else{
            // System.out.println("Exporting row " + rowId + " of this table: " + tablesToExport);
            // }

            for (String table : tablesToExport) {
                exporter.exportTable(table, rowId);
            }

            exporter.exportDocumentInformation();
            exporter.writeRdfFooter();

            if (outputStream instanceof GZIPOutputStream) {
                ((GZIPOutputStream) outputStream).finish();
            }
        } finally {
            Execute.close(outputStream);
            Execute.close(conn);
        }
    }

    /**
     * Utility method that populates the given properties from the given file path.
     *
     * @param properties
     * @param filePath
     * @throws IOException
     */
    private static void loadProperties(Properties properties, String filePath) throws IOException {

        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                properties.load(inputStream);
            } finally {
                Execute.close(inputStream);
            }
        }
    }

    /**
     * Utility method that returns a connection on the database given in the properties file.
     *
     * @return
     * @throws SQLException
     */
    private Connection getConnection() throws SQLException {

        String driver = dbProps.getProperty("driver");
        String dbUrl = dbProps.getProperty("database");
        String userName = dbProps.getProperty("user");
        String password = dbProps.getProperty("password");

        if (msAccessFilePath != null && dbUrl != null) {
            dbUrl = dbUrl.concat(msAccessFilePath);
        }

        try {
            Class.forName(driver);
            return DriverManager.getConnection(dbUrl, userName, password);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to find the driver class: " + driver, e);
        }
    }

    /**
     * Writes the given properties into the given file path.
     *
     * @param properties
     * @param outputFilePath
     * @throws IOException
     */
    private static void outputProperties(Properties properties, String outputFilePath) throws IOException {

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outputFilePath);
            properties.store(outputStream, "");
        } finally {
            Execute.close(outputStream);
        }
    }

    /**
     * @param inputStream
     */
    private static void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) {
                // Ignore closing exceptions.
            }
        }
    }

    /**
     * @param outputStream
     */
    private static void close(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Exception e) {
                // Ignore closing exceptions.
            }
        }
    }

    /**
     *
     * @param conn
     */
    private static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // Ignore closing exceptions
            }
        }
    }

    /**
     *
     * @param args
     * @throws SQLException
     * @throws IOException
     */
    public static void main(String[] args) throws SQLException, IOException {

        if (args == null || args.length == 0 || args[0].equalsIgnoreCase("-?")) {
            printUsage();
            return;
        }

        Execute execute = new Execute(args);
        execute.run();
        // System.out.println("Done!");
    }

    /**
     * Prints usage text to console.
     */
    private static void printUsage() {
        System.out.println("This class accepts the following command line arguments:");
        System.out.println(" (note that unrecognized arguments will be treated as names of tables to export)");
        System.out.println();
        System.out.println(" -d database_properties_file   Path to the database connection properties file.");
        System.out.println(" -f rdf_properties_file        Path to the RDF export properties file.");
        System.out.println(" -o rdf_output_file            Path to the RDF output file.");
        System.out.println(" -m ms_access_file             Path to the MS-Access database file to export.");
        System.out.println(" -z                            The RDF output file will be zipped.");
        System.out.println(" -x                            Tables/keys will be auto-discovered.");
        System.out.println(" -xa                           Tables/keys will be auto-discovered, user prompted for confirmation.");
        System.out.println(" -p properties_output_file     If -x or -xa given then auto-discovered info is saved into this file.");
        System.out.println(" -i rowId                      Only records with this primary key value will be exported.");
    }
}
