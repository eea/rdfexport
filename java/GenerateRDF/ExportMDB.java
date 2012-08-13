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
 * Agency.  Portions created by TripleDev are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *  Søren Roug, EEA
 *  Jaanus Heinlaid, TripleDev
 *
 * $Id$
 */
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

class TableSpec {
    /** Name of table. */
    String tableName;
    /** Datatype of column. */
    ArrayList<String> columns;

    /**
     * Constructor.
     */
    TableSpec() {
        tableName = "";
        columns = new ArrayList<String>();
    }

    public void addColumn(String col) {
        columns.add(col.toLowerCase());
    }

    /**
     * Generate a SQL query for a table.
     */
    public String createQuery() {
        StringBuilder columnList = new StringBuilder();

        columnList.append("SELECT '##'");
        for (int i = 0; i < columns.size(); i++) {
            String c = columns.get(i);
            columnList.append(", ");
            columnList.append(c);
//          columnList.append(" AS '");
//          String label = c.substring(0, 1).toLowerCase() + c.substring(1);
//          columnList.append(label);
//          columnList.append("'");
        }
        columnList.append(" FROM " + tableName);

        return columnList.toString();
    }
}


/**
 * Export Database. The purpose of this class is to discover the table relationships
 * and then create an export in RDF where the tables are interlinked.
 * This code is an early start on an export to RDF module that
 * can automatically find all tables in a database, the internal
 * relations and then export the database as one RDF file.
 *
 * Intended use is to convert MS-Access databases.
 *
 * To use it with MS-Access download the trial version of the HXTT driver from
 * http://www.hxtt.com/access.zip or http://www.hxtt.com/access.html
 * The trial version will only return 1000 rows and allow 50 queries in the
 * same connection.
 *
 */
public class ExportMDB {
    /** Connection to database. */
    private Connection con;
    /** The namespaces to add to the rdf:RDF element. */
    private HashMap<String, String> namespaces;
    /** The properties that are object properties. They point to another object. */
    private HashMap<String, String> objectProperties;
    /** The datatype mappings. */
    private HashMap<String, String> datatypeMap;
    /** All the tables in the database. */
    private HashMap<String, TableSpec> tables;
    /** Hashtable of loaded properties. */
    private Properties props;

    /**
     * Constructor.
     * @param dbCon - The database connection
     * @param propFilename - The file to load the configuration from
     *
     * @throws IOException - if the properties file is missing
     * @throws SQLException - if the SQL database is not available
     * @throws ClassNotFoundException - if the SQL driver is unavailable
     * @throws InstantiationException - if the SQL driver can't be instantiatied
     * @throws IllegalAccessException - unknown
     */
    public ExportMDB(Connection dbCon, Properties properties) throws IOException,
                                SQLException, ClassNotFoundException,
                                InstantiationException, IllegalAccessException {
        props = properties;


        con = dbCon;

        namespaces = new HashMap<String, String>();
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

        objectProperties = new HashMap<String, String>();
        datatypeMap = new HashMap<String, String>();
        tables = new HashMap<String, TableSpec>();

        // Get the objectproperties from the properties file.
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("objectproperty.")) {
                String value = props.getProperty(key);
                addObjectProperty(key.substring(15), "->".concat(value));
            } else if (key.startsWith("datatype.")) {
                datatypeMap.put(key.substring(9), props.getProperty(key));
            }
        }
    }

    /**
     * Do a nice shutdown in case the user forgets to call close().
     *
     * @throws Throwable - ignored
     */
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Close the connection to the database.
     *
     * @throws SQLException if there is a database problem.
     */
    public void close() throws SQLException {
        if (con != null) {
            con.close();
            con = null;
        }
    }


    /**
     * Discover all tables in the database and create SELECT statements for the properties file.
     */
    public void discoverTables() throws SQLException {
        Statement stmt = null;
        StringBuilder tableList;
        TableSpec tableRec = null;
        String currTable = null;
        Boolean firstTime = true;

        tableList = new StringBuilder();
        try {
            DatabaseMetaData dmd = con.getMetaData();

            ResultSet rs1 = dmd.getColumns(null, null, "%", "%");
            while (rs1.next()) {
                String table = rs1.getString(3);
                if (!table.equals(currTable)) {
                    if (!firstTime) {
                        String tableQueryKey = currTable.concat(".query");
                        props.setProperty(tableQueryKey, tableRec.createQuery());
                    }
                    currTable = table;
                    tableRec = new TableSpec();
                    tableRec.tableName = table;
                    tables.put(table, tableRec);
                    if (tableList.length() != 0) tableList.append(" ");
                    tableList.append(table);
                    firstTime = false;
                }
                tableRec.addColumn(rs1.getString(4));

            }
            if (!firstTime) {
                String tableQueryKey = currTable.concat(".query");
                props.setProperty(tableQueryKey, tableRec.createQuery());
            }
            //TODO: Find the primary key of each table. The purpose is
            //to construct the subject URL for foreign keys.
            // You can use:
            // dmd.getPrimaryKeys(null, ss, rs2.getString(3))
            for(String t : tables.keySet()) {
                ResultSet pks = dmd.getPrimaryKeys(null, null, t);
            }

            //TODO: Find the references from this table to other tables'
            //primary keys. These will be used to create links in the RDF output
            // You can use:
            // dmd.getImportedKeys(null, ss, rs2.getString(3))
            // If no primary key exists, then we use the table row.
            // (indicated by making the first column be '##')

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
        props.setProperty("tables", tableList.toString());
    }


    /**
     * Add name of property to table of object properties.
     *
     * @param name - name of column.
     * @param reference - will always start with '->'.
     */
    private void addObjectProperty(String name, String reference) {
        objectProperties.put(name, reference);
    }



    /**
     * Main routine. Primarily to demonstrate the use.
     * Flags: -p <i>filename</i> - save the discovered information as a properties file.
     *        -f <i>filename</i> - load the template properties from the specified file.
     *        -m <i>filename</i> - the name of the MS-Access file to investigate.
     */
    public static void main(String[] args) {
        ArrayList<String> unusedArgs;
        String[] tables;
        String rdfPropFilename = "exportmdb.properties";
//      String dbPropFilename = "database.properties";
        String mdbFilename = null;
        String writeProperties = null;

        unusedArgs = new ArrayList<String>(args.length);

        // Parse arguments.
        for (int a = 0; a < args.length; a++) {
            if (args[a].startsWith("-p")) {
                if (args[a].length() > 2) writeProperties = args[a].substring(2);
                else writeProperties = args[++a];
//          } else if (args[a].equals("-d")) {
//              dbPropFilename = args[++a];
//          } else if (args[a].startsWith("-d")) {
//              dbPropFilename = args[a].substring(2);
            } else if (args[a].startsWith("-f")) {
                if (args[a].length() > 2) rdfPropFilename = args[a].substring(2);
                else rdfPropFilename = args[++a];
            } else if (args[a].startsWith("-m")) {
                if (args[a].length() > 2) mdbFilename = args[a].substring(2);
                else mdbFilename = args[++a];
            } else {
                 unusedArgs.add(args[a]);
            }
        }
        try {
//          Properties props = new Properties();
            Properties rdfProps = new Properties();
//          props.load(new FileInputStream(dbPropFilename));

//          String driver = props.getProperty("driver");
//          String dbUrl = props.getProperty("database");
//          String userName = props.getProperty("user");
//          String password = props.getProperty("password");

            String driver = "com.hxtt.sql.access.AccessDriver";
            String dbUrl = "jdbc:access:/".concat(mdbFilename);
            String userName = "user";
            String password = "password";

            Class.forName(driver).newInstance();
            Connection con = DriverManager.getConnection(dbUrl, userName, password);
            rdfProps.load(new FileInputStream(rdfPropFilename));

            ExportMDB inspector = new ExportMDB(con, rdfProps);
            inspector.discoverTables();
            if (writeProperties != null) {
                FileOutputStream propOut = new FileOutputStream(writeProperties);
                rdfProps.store(propOut,"");
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
                exporter.close();
                con.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
//vim: set expandtab sw=4 :
