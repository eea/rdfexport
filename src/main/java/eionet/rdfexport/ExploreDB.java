package eionet.rdfexport;

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
 * $Id: ExportMDB.java 12552 2012-08-28 10:03:28Z roug $
 */
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * A class representing fkColumn-pkColumn pairs and the order of pkColumns. Tables are not given, so it's kind of out of context.
 */
class FkColumns {

    /** The map's keys are foreign-key columns, values are their corresponding primary key columns */
    Map<String, String> fkToPkColumns = new HashMap<String, String>();
    /** The order in which the foreign-key columns have been set. */
    Map<Integer, String> positions = new HashMap<Integer, String>();

    /**
     * Returns the number of fkColumn-pkColumn pairs in this object.
     *
     * @return
     */
    int getSize() {
        return fkToPkColumns.size();
    }
}

/**
 * A class withholding information about a table found in the database.
 */
class TableSpec {

    /** Name of table. */
    String tableName;

    /** List of columns found on this table. */
    List<String> columns;

    /** Columns constituting the primary key, in that same order. */
    List<String> pkColumns;

    /**
     * A map representing foreign keys found on this table. The map's keys stand for imported tables (aka pkTables). The map's
     * values represent foreign keys pointing to the imported table. Every such foreign key is given as a map, where the key is the
     * foreign key's name, and its value of {@link FkColumns} type.
     */
    Map<String, Map<String, FkColumns>> fkMap;

    /**
     * Constructor.
     */
    TableSpec(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Adds a column found on this table.
     *
     * @param col
     */
    void addColumn(String col) {
        if (columns == null) {
            columns = new ArrayList<String>();
        }
        columns.add(col.toLowerCase());
    }

    /**
     * Adds a foreign-key / primary-key column relation found on this table.
     *
     * @param pkTable
     *            - table whose primary key is imported
     * @param fkName
     *            - foreign key name
     * @param fkCol
     *            - foreign key column name
     * @param pkCol
     *            - corresponding primary key column name in the imported table
     * @param pos
     *            - position of the given foreign key column in this foreign key (may start from 0 or 1, depending on JDBC driver).
     */
    public void addFkColumn(String pkTable, String fkName, String fkCol, String pkCol, int pos) {

        if (fkMap == null) {
            fkMap = new HashMap<String, Map<String, FkColumns>>();
        }

        Map<String, FkColumns> fKeys = fkMap.get(pkTable);
        if (fKeys == null) {
            fKeys = new HashMap<String, FkColumns>();
            fkMap.put(pkTable, fKeys);
        }

        FkColumns fkColumns = fKeys.get(fkName);
        if (fkColumns == null) {
            fkColumns = new FkColumns();
            fKeys.put(fkName, fkColumns);
        }

        fkColumns.fkToPkColumns.put(fkCol.toLowerCase(), pkCol.toLowerCase());
        fkColumns.positions.put(pos, fkCol.toLowerCase());
    }

    /**
     * Returns a map of columns that are simple (i.e. non-compound) foreign keys pointing to some simple primary keys of other
     * tables. The map's keys are names of such columns. The tables they point to are the map's values.
     *
     * As an input, this method takes a map representing simple primary keys of all tables in this database. If a table does not
     * have simple key, it is not listed here. The map's keys are table names, and the values are the names of single columns that
     * constitute the particular table's primary key.
     *
     * @param tablesPkColumns
     * @return
     */
    private Map<String, String> getSimpleForeignKeysToTables(Map<String, String> tablesPkColumns) {

        HashMap<String, String> result = new HashMap<String, String>();
        if (fkMap != null && !fkMap.isEmpty()) {

            for (Entry<String, Map<String, FkColumns>> tableEntry : fkMap.entrySet()) {
                String pkTable = tableEntry.getKey();
                Collection<FkColumns> fKeys = tableEntry.getValue().values();
                for (FkColumns fkColumns : fKeys) {
                    if (fkColumns.getSize() == 1) {
                        String fkColumn = fkColumns.fkToPkColumns.keySet().iterator().next();
                        String pkColumn = fkColumns.fkToPkColumns.values().iterator().next();
                        if (pkColumn.equalsIgnoreCase(tablesPkColumns.get(pkTable))) {
                            result.put(fkColumn, pkTable);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Generate a SQL query for a table.
     *
     * @param jdbcSubProtocol
     *            - used for constructing DB vendor-specific dialects.
     * @param tablesPkColumns
     *            - simple primary keys of all tables (key = table, value = table's primary key column).
     * @param interActiveMode
     *            - if true, user will be prompted for each discovered table and foreign key
     * @return
     */
    public String createQuery(String jdbcSubProtocol, Map<String, String> tablesPkColumns, boolean interActiveMode) {

        Map<String, String> simpleForeignKeys = getSimpleForeignKeysToTables(tablesPkColumns);

        StringBuilder query = new StringBuilder("SELECT ");
        if (pkColumns.isEmpty()) {
            query.append("'@' AS id");
        } else {
            // Concatenate primary key columns.
            String pksConcatenated = concatColumns(pkColumns, jdbcSubProtocol);
            query.append(pksConcatenated).append(" AS id, ").append(pksConcatenated).append(" AS 'rdfs:label'");
        }

        String colEscapeStart = "\u0060";
        String colEscapeEnd = "\u0060";
        if ("access".equalsIgnoreCase(jdbcSubProtocol)) {
            colEscapeStart = "[";
            colEscapeEnd = "]";
        }

        ArrayList<String> fkReferences = new ArrayList<String>();
        for (int i = 0; i < columns.size(); i++) {

            String col = columns.get(i);
            String label = (col.substring(0, 1).toLowerCase() + col.substring(1)).replace(" ", "_");
            String pkTable = simpleForeignKeys.get(col);
            if (pkTable != null) {
                boolean exportAsReference =
                        interActiveMode == false ? true : ExploreDB.readUserInputBoolean(tableName + "." + col + " is a FK to "
                                + pkTable + ". Export as reference?");
                if (exportAsReference == true) {
                    label += "->" + pkTable;
                    fkReferences.add(col + "->" + pkTable);
                }
            } else {
                pkTable = getFirstMatchingKey(tablesPkColumns, col);
                if (pkTable != null) {
                    boolean exportAsReference =
                            interActiveMode == false ? true : ExploreDB.readUserInputBoolean(tableName + "." + col
                                    + " has the same name as PK in " + pkTable + ". Export as reference?");
                    if (exportAsReference == true) {
                        label += "->" + pkTable;
                        fkReferences.add(col + "->" + pkTable);
                    }
                }
            }

            query.append(", ").append(colEscapeStart).append(col).append(colEscapeEnd).append(" AS '").append(label).append("'");
        }
        query.append(" FROM ").append(tableName);

        // if (pkColumns != null && !pkColumns.isEmpty()){
        // System.out.println(tableName + " PK columns: " + pkColumns);
        // }
        // if (fkReferences != null && !fkReferences.isEmpty()){
        // System.out.println(tableName + " FK references: " + fkReferences);
        // }
        // System.out.println(tableName + " query:" + query);

        return query.toString();
    }

    /**
     * Returns the given map's first key that pairs with the given value.
     *
     * @param map
     * @param value
     * @return
     */
    private String getFirstMatchingKey(Map<String, String> map, String value) {

        for (Entry<String, String> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns a string representing the SQL concatenation of the columns given. The dialect differs depending on the DB-vendor
     * represented by the given JDBC sub-protocol.
     *
     * @param columns
     * @param jdbcSubProtocol
     * @return
     */
    private String concatColumns(List<String> columns, String jdbcSubProtocol) {

        StringBuilder result = new StringBuilder();

        if (columns != null && !columns.isEmpty()) {
            String dbVendor = jdbcSubProtocol.toLowerCase();
            if (dbVendor.equals("mysql")) {

                result.append("concat(''");
                for (String col : columns) {
                    result.append(", ").append(col);
                }
                result.append(")");
            } else if (dbVendor.equals("postgresql")) {

                result.append("''");
                for (String col : columns) {
                    result.append(" || ").append(col);
                }
            } else if (dbVendor.equals("access")) {

                result.append("''");
                for (String col : columns) {
                    result.append(" + CStr(").append(col).append(")");
                }
            } else {
                // System.out.println("Warning: SQL dialect not found for '" + jdbcSubProtocol + "', using ANSI-SQL!");
                result.append("''");
                for (String col : columns) {
                    result.append(" || ").append(col);
                }
            }
        }

        return result.length() == 0 ? "'@'" : result.toString();
    }
}

/**
 * Export Database. The purpose of this class is to discover the table relationships and then create an export in RDF where the
 * tables are interlinked. This code is an early start on an export to RDF module that can automatically find all tables in a
 * database, the internal relations and then export the database as one RDF file.
 *
 * Intended use is to convert MS-Access databases.
 *
 * To use it with MS-Access download the trial version of the HXTT driver from http://www.hxtt.com/access.zip or
 * http://www.hxtt.com/access.html The trial version will only return 1000 rows and allow 50 queries in the same connection.
 *
 */
public class ExploreDB {

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
    /** The JDBC sub-protocol in the URL used to obtain this connection. */
    private String jdbcSubProtocol;
    /** If true, user will be prompted for each discovered table and foreign key. */
    private boolean interActiveMode = false;

    /**
     *
     * Class constructor.
     *
     * @param dbCon
     *            - the database connection to explore
     * @param properties
     *            - properties where to write the discovered tables, queries, etc.
     * @param interActiveMode
     *            if true, prompt user for each discovered table and foreign key
     *
     * @throws SQLException
     */
    public ExploreDB(Connection dbCon, Properties properties, boolean interActiveMode) throws SQLException {

        con = dbCon;
        props = properties;
        this.interActiveMode = interActiveMode;

        String jdbcUrl = con.getMetaData().getURL();
        jdbcSubProtocol = jdbcUrl.substring(5, jdbcUrl.indexOf(':', 5));

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
     * Discover all tables in the database and create SELECT statements for the properties file.
     */
    public void discoverTables() throws SQLException {

        StringBuilder tablesListBuilder = new StringBuilder();

        ResultSet rs = null;
        try {
            DatabaseMetaData dbMetadata = con.getMetaData();
            rs = dbMetadata.getColumns(null, null, "%", "%");

            // Set of tables that should be skipped. Will be populated by user input below.
            HashSet<String> skipTables = new HashSet<String>();
            // This is a reserved table in MS-Access database templates generated from http://dd.eionet.europa.eu, lets skip.
            skipTables.add("VALIDATION_METADATA_DO_NOT_MODIFY");
            skipTables.add("MSysAccessObjects".toUpperCase());
            skipTables.add("MSysAccessXML".toUpperCase());
            skipTables.add("MSysACEs".toUpperCase());
            skipTables.add("MSysObjects".toUpperCase());
            skipTables.add("MSysQueries".toUpperCase());
            skipTables.add("MSysRelationships".toUpperCase());

            while (rs.next()) {

                String tableName = rs.getString(3);
                if (!skipTables.contains(tableName.toUpperCase())) {

                    TableSpec tableSpec = tables.get(tableName);
                    if (tableSpec == null) {

                        boolean exportThisTable =
                                interActiveMode == false ? true : readUserInputBoolean("Export table " + tableName + "?");
                        if (exportThisTable == false) {
                            skipTables.add(tableName.toUpperCase());
                            continue;
                        }

                        tableSpec = new TableSpec(tableName);
                        tables.put(tableName, tableSpec);
                        tablesListBuilder.append(tableName).append(" ");
                    }
                    tableSpec.addColumn(rs.getString(4));
                }
            }
            rs.close();

            // Loop through the discovered tables, and discover each one's primary and foreign keys too.
            // While at it, remember simple (i.e. non-compound) primary keys of every table for later use below.

            HashMap<String, String> tablesPkColumns = new HashMap<String, String>();
            for (Map.Entry<String, TableSpec> entry : tables.entrySet()) {

                String tableName = entry.getKey();
                TableSpec tableSpec = entry.getValue();

                HashMap<Short, String> primKeys = new HashMap<Short, String>();
                rs = dbMetadata.getPrimaryKeys(null, null, tableName);
                while (rs.next()) {
                    primKeys.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME").toLowerCase());
                }
                rs.close();
                tableSpec.pkColumns = ExploreDB.listValuesSortedByKeys(primKeys);
                if (primKeys.size() == 1) {
                    tablesPkColumns.put(tableName, primKeys.values().iterator().next());
                }

                rs = dbMetadata.getImportedKeys(null, null, tableName);
                while (rs.next()) {
                    tableSpec.addFkColumn(rs.getString("PKTABLE_NAME"), rs.getString("FK_NAME"), rs.getString("FKCOLUMN_NAME"),
                            rs.getString("PKCOLUMN_NAME"), rs.getShort("KEY_SEQ"));
                }
            }

            // Now that all tables' primary/foreign keys have been set, create every table's query, and set it in properties
            // that will be later used for RDF generation.
            for (Map.Entry<String, TableSpec> entry : tables.entrySet()) {
                String query = entry.getValue().createQuery(jdbcSubProtocol, tablesPkColumns, interActiveMode);
                props.setProperty(entry.getKey().concat(".query"), query);
            }
        } finally {
            ExploreDB.close(rs);
        }

        props.setProperty("tables", tablesListBuilder.toString());
    }

    /**
     * Close the connection to the database.
     *
     * @throws SQLException
     *             if there is a database problem.
     */
    public void close() throws SQLException {
        if (con != null) {
            con.close();
            con = null;
        }
    }

    /**
     * Do a nice shutdown in case the user forgets to call close().
     *
     * @throws Throwable
     *             - ignored
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Add name of property to table of object properties.
     *
     * @param name
     *            - name of column.
     * @param reference
     *            - will always start with '->'.
     */
    private void addObjectProperty(String name, String reference) {
        objectProperties.put(name, reference);
    }

    /**
     *
     * @param rs
     */
    protected static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                // Deliberately ignore.
            }
        }
    }

    /**
     *
     * @param map
     * @return
     */
    protected static <K extends Comparable<? super K>, V> List<V> listValuesSortedByKeys(Map<K, V> map) {

        ArrayList<V> result = new ArrayList<V>();
        if (map != null && !map.isEmpty()) {

            ArrayList<K> keyList = new ArrayList<K>(map.keySet());
            Collections.sort(keyList);
            for (K key : keyList) {
                result.add(map.get(key));
            }
        }
        return result;
    }

    /**
     *
     * @param question
     * @return
     */
    protected static boolean readUserInputBoolean(String question) {

        for (int i = 0; i < 10; i++) {
            System.out.print(question + " (y/n): ");
            String line = Execute.USER_INPUT.nextLine().trim().toLowerCase();
            if (line.equals("y")) {
                return true;
            } else if (line.equals("n")) {
                return false;
            }
        }

        System.out.println("Tried 10 times, assuming the answer is 'n'!");
        return false;
    }
}
