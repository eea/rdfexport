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
 * The Original Code is RDFExport 1.0
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Juhan Voolaid
 *        Søren Roug
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
import java.util.Properties;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;

/**
 * RDF export main class. Executed from command line.
 */
public final class Execute {

    /** Default file path of the RDF export properties file. */
    private static final String DEFAULT_INPUT_PROPS_FILE_PATH = "rdfexport.properties";

    /** Will be used as user input scanner from command line. */
    protected static final Scanner USER_INPUT = new Scanner(System.in);

    /** List of unrecognized command line arguments. They will be interpreted as names of tables to export. */
    private String[] unusedArguments = new String[0];

    /** Path to the input export properties file. */
    private String inputPropsFilePath = null;

    /** Path to the file where the auto-discovered tables and generated queries shall be written into, if requested so. */
    private String outputPropsFilePath = null;

    /**
     * The template file used for the export's pre-configuration. Contains the JDBC driver's class name, various driver specific
     * properties, the datatype mappings and the namespaces. The user then runs this command:
     *
     * rdfexport -T template.properties -x -m Waterbase_Rivers_DDviolations_solutions.mdb -p rdfexport.properties
     *
     * This command copies the properties from template.properties into rdfexport.properties (i.e. the one identified by "-p")
     * and also constructs a db.database=jdbc:access:/Waterbase_Rivers_DDviolations_solutions.mdb that it puts
     * into rdfexport.properties. From then on, the user can simply run this command:
     *
     * rdfexport -f rdfexport.properties
     */
    private String templatePropsFilePath = null;

    /** Driver class given on command line. */
    private String jdbcDriver;

    /** Database URL given on command line. */
    private String jdbcUrl;

    /** Database user name given on command line. */
    private String userName;

    /** Database password given on command line. */
    private String password;

    /** If true, the tables and primary/foreign keys shall be auto-discovered, disregarding the ones given in properties file. */
    private boolean selfExplore = false;

    /** Path to the file where the RDF shall be generated into. */
    private String rdfOutputFilePath = null;

    /** If true, the generated RDF output shall be zipped. */
    private boolean zipOutput = false;

    /** MS-Access to export. Concatenated to the value of "database" property from database connection properties file. */
    private String mdbFilePath;

    /** The URI of the vocabulary of the RDF output that shall be generated. */
    private String vocabularyUri = null;

    /** The XML base URI of the generated RDF output. */
    private String baseUri = null;

    /** If true, the auto-disovery mode will ask for each table and foreign key whether to export it. */
    private boolean interActiveMode = false;

    /** The primary-key value of the row to export, if only one row from one table is to be exported. */
    private String rowId = null;

    /** The map of properties, based on which the RDF generation will be executed or the output properties file generated. */
    private Properties props = new Properties();

    /**
     * Constructs from the given command line arguments.
     *
     * @param args - command line arguments.
     * @throws IOException - if anything goes wrong with reading the properties and/or writing the output
     */
    private Execute(String[] args) throws IOException {

        // First, parse the command line arguments.
        parseArguments(args);

        // Use default path for the input properties file if not given from command line arguments.
        if (inputPropsFilePath == null || inputPropsFilePath.isEmpty()) {
            inputPropsFilePath = DEFAULT_INPUT_PROPS_FILE_PATH;
        }

        // If properties template file and properties output file both given, then we're in the "pre-configuration" mode,
        // meaning that we're not going to generate RDF, but we shall generate a properties output file based on the
        // properties template file and auto-discovered information about the database if auto-discovery turned on.
        // Otherwise we shall load properties from the properties input file.
        if (selfExplore) {
            if (templatePropsFilePath != null && !templatePropsFilePath.isEmpty()) {
    //              && outputPropsFilePath != null && !outputPropsFilePath.isEmpty()) {
                File file = new File(templatePropsFilePath);
                if (!file.exists() || !file.isFile()) {
                    throw new IllegalArgumentException("Failed to find template properties at " + templatePropsFilePath);
                }
            } else {
                //props.load(Execute.class.getResourceAsStream("/someProps.properties"));
                // Provide backup template from JAR file
                templatePropsFilePath = Execute.class.getClassLoader().getResource("explore.properties").getFile();
            }
            Execute.loadProperties(props, templatePropsFilePath);
        } else {
            File file = new File(inputPropsFilePath);
            if (!file.exists() || !file.isFile()) {
                throw new IllegalArgumentException("Failed to find input properties at " + inputPropsFilePath);
            }
            Execute.loadProperties(props, inputPropsFilePath);
        }

        // If base URI was not given in command line, load it from the input properties,
        // otherwise place it into the loaded input properties map.
        if (baseUri == null) {
            baseUri = props.getProperty("baseurl");
        } else {
            props.setProperty("baseurl", baseUri);
        }

        if (vocabularyUri == null) {
            // Get the vocabulary URI from the loaded input properties.If it's null or empty, then generate it on the basis of the
            // given base URI. If the latter is not given either, just set it to "#properties/".
            // Finally, place the resulting vocabulary URI into the loaded input properties map.
            vocabularyUri = props.getProperty("vocabulary");
            if (vocabularyUri == null || vocabularyUri.isEmpty()) {

                // TODO: The 'vocabulary' property has to be generated if it is not in the template file.
                // and then written to the properties file.

                if (baseUri == null || baseUri.isEmpty()) {
                    vocabularyUri = "#properties/";
                } else {
                    vocabularyUri = baseUri.concat("properties/");
                }
                props.setProperty("vocabulary", vocabularyUri);
            }
        }
    }

    /**
     * Parses the given command line arguments, and sets the corresponding fields of this object.
     *
     * @param args
     *         - the arguments
     */
    private void parseArguments(String[] args) {

        OptionParser op = new OptionParser(args, "xazp:i:m:o:f:B:D:J:U:P:T:V:");
        selfExplore = op.getOptionFlag("x");
        if (selfExplore) {
            interActiveMode = op.getOptionFlag("a");
        }
        zipOutput = op.getOptionFlag("z");
        outputPropsFilePath = op.getOptionArgument("p");
        baseUri = op.getOptionArgument("b");
        templatePropsFilePath = op.getOptionArgument("T");
        mdbFilePath = op.getOptionArgument("m");
        rdfOutputFilePath = op.getOptionArgument("o");
        if ("-".equals(rdfOutputFilePath)) {
            rdfOutputFilePath = null; // Linux convention
        }
        inputPropsFilePath = op.getOptionArgument("f");
        rowId = op.getOptionArgument("i");
        jdbcUrl = op.getOptionArgument("J");
        jdbcDriver = op.getOptionArgument("D");
        userName = op.getOptionArgument("U");
        password = op.getOptionArgument("P");
        vocabularyUri = op.getOptionArgument("V");

        unusedArguments = op.getUnusedArguments();
    }

    /**
     * The core method that performs the auto-discovery of tables/keys (if requested) and generates the RDF. Assumes input
     * properties and command line arguments have been processed, and corresponding fields set in the object.
     *
     * @throws SQLException
     *             - if the SQL database is not available
     * @throws IOException
     *             - if the properties file is missing
     */
    private void run() throws SQLException, IOException {

        Connection conn = null;
        OutputStream outputStream = System.out;
        try {
            conn = getConnection();

            if (selfExplore) {
                ExploreDB dbExplorer = new ExploreDB(conn, props, interActiveMode);
                dbExplorer.discoverTables(true);
            }

            if (outputPropsFilePath != null && !outputPropsFilePath.isEmpty()) {
                Execute.outputProperties(outputPropsFilePath, props);
                return;
            }

            if (rdfOutputFilePath != null && !rdfOutputFilePath.isEmpty()) {
                outputStream = new FileOutputStream(rdfOutputFilePath);
            }

            if (zipOutput) {
                outputStream = new GZIPOutputStream(outputStream);
            }

            GenerateRDF exporter = new GenerateRDF(outputStream, conn, props);

            String[] tablesToExport = (unusedArguments.length == 0) ? exporter.getAllTables() : unusedArguments;

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
     *         - The properties object
     * @param filePath
     *         - The file path to load the properties from
     * @throws IOException
     *             - if the properties file is missing
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
     * Utility method that returns a connection on the database given on the command line or in the properties file.
     *
     * @return
     *             - the connection
     * @throws SQLException
     *             - if the SQL database is not available
     */
    private Connection getConnection() throws SQLException {

        if (jdbcUrl != null) {
            props.setProperty("db.database", jdbcUrl);
        }
        // An MS Access file given from command line always overrides the one given in properties file.
        if (mdbFilePath != null && !mdbFilePath.isEmpty()) {

            File mdbFile = new File(mdbFilePath);
            if (!mdbFile.exists() || !mdbFile.isFile()) {
                throw new IllegalArgumentException("The given file is not found: " + mdbFilePath);
            }

            jdbcUrl = "jdbc:access:/" + mdbFile;
            props.setProperty("db.database", jdbcUrl);
        } else {
            jdbcUrl = props.getProperty("db.database");
        }

        if (jdbcDriver != null) {
            props.setProperty("db.driver", jdbcDriver);
        }
        String driver = props.getProperty("db.driver");

        if (userName != null) {
            props.setProperty("db.user", userName);
        } else {
            userName = props.getProperty("db.user");
        }
        if (password != null) {
            props.setProperty("db.password", password);
        } else {
            password = props.getProperty("db.password");
        }

        if (driver == null || driver.isEmpty()) {
            throw new IllegalArgumentException("The database driver property must not be empty!");
        }
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            throw new IllegalArgumentException("Failed to detect database from command line or properties file!");
        }

        try {
            Class.forName(driver);
            return DriverManager.getConnection(jdbcUrl, userName, password);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to find the driver class: " + driver, e);
        }
    }

    /**
     * Writes the given properties into the given file path.
     *
     * @param outputFilePath
     *         - The pathname to write the properties to
     * @param properties
     *         - The properties object
     * @throws IOException if the file can't be written
     */
    private static void outputProperties(String outputFilePath, Properties properties) throws IOException {

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outputFilePath);
            SortedProperties props = new SortedProperties();
            props.putAll(properties);
            props.store(outputStream, "");
        } finally {
            Execute.close(outputStream);
        }
    }

    /**
     * Close input stream.
     *
     * @param inputStream - file
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
     * Close output stream.
     *
     * @param outputStream - file
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
     * Close database connection.
     *
     * @param conn - database connection
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
     * Main method.
     *
     * @param args - the command line arguments provided by the operating system.
     * @throws SQLException
     *             - if the SQL database is not available
     * @throws IOException
     *             - if the properties file is missing
     */
    public static void main(String[] args) throws SQLException, IOException {

        if (args == null || args.length == 0 || args[0].equalsIgnoreCase("-?") || args[0].equalsIgnoreCase("-h")) {
            printUsage();
            return;
        }

        Execute execute = new Execute(args);
        execute.run();
    }

    /**
     * Prints usage text to console.
     */
    private static void printUsage() {
        System.out.println("Usage: This command accepts the following command line arguments:");
        System.out.println();
        System.out.println(" -f input_properties_file    Path of the input properties file containing everything needed"
                + " for RDF generation. That includes the database's JDBC url, JDBC driver class name,"
                + " datatype mappings, namespaces, SQL queries to export, etc.");

        System.out.println(" -o rdf_output_file          Path of the RDF output file to be generated.");

        System.out.println(" -T template_properties_file From this file and auto-discovered info about the database,"
                + " the output_properties_file is generated"
                + " that can then be used as an input_properties_file for multiple reuse.");

        System.out.println(" -J jdbc_database_url        The URL to the database.");
        System.out.println(" -D jdbc_driver_class        For MySQL use com.mysql.jdbc.Driver.");
        System.out.println(" -U database_user            The user to log into the database.");
        System.out.println(" -P password                 The password for the database.");

        System.out.println(" -p output_properties_file   Generated from template_properties_file and auto-discovered info."
                + " If -T and -p have been specified, then -f is ignored and no RDF output generated."
                + " Instead, the output_properties_file will be generated and the program exits.");

        System.out.println(" -z                          The RDF output file will be zipped. if this argument is present.");

        System.out.println(" -m                          Path of the MS Access file to query from."
                + " Overrides the one given in input_properties_file or template_properties_file.");
        System.out.println(" -x                          Tables/keys of the database will be auto-discovered.");
        System.out.println(" -xa                         Tables/keys will be auto-discovered, user prompted for confirmation.");
        System.out.println(" -B base_uri                 Base URI which overrides the one in the"
                + " input_properties_file or template_properties_file.");
        System.out.println(" -V vocabulary_uri           Vocabulary URI which overrides the one in the"
                + " input_properties_file or template_properties_file.");
        System.out.println(" -i rowId                    Only records with this primary key value will be exported.");
        System.out.println(" -h or -?                    Show this help");
        System.out.println("Unrecognized arguments will be treated as names of tables to export");
    }
}
