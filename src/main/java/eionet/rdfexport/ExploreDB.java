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
package eionet.rdfexport;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * A class representing fkColumn-pkColumn pairs and the order of pkColumns.
 * Tables are not given, so it's kind of out of context.
 */
class FkColumns {

    /** The map's keys are foreign-key columns, values are their corresponding
     * primary key columns. */
    Map<String, String> fkToPkColumns = new HashMap<String, String>();
    /** The order in which the foreign-key columns have been set. */
    Map<Integer, String> positions = new HashMap<Integer, String>();

    /**
     * Returns the number of fkColumn-pkColumn pairs in this object.
     *
     * @return the size
     */
    int getSize() {
        return fkToPkColumns.size();
    }
}

/**
 * A class holding information about a table found in the database.
 */
class TableSpec {

    /** The datatype mappings. */
    private HashMap<Integer, String> datatypeMap;

    /** Name of table. */
    String tableName;

    /** The map of columns found on this table. Key = column name, value = column data type as in java.sql.Types. */
    LinkedHashMap<String, Integer> columns;

    /** Columns constituting the primary key, in that same order. */
    List<String> pkColumns;

    /**
     * A map representing foreign keys found on this table. The map's keys stand
     * for imported tables (aka pkTables). The map's values represent foreign
     * keys pointing to the imported table. Every such foreign key is given as a
     * map, where the key is the foreign key's name, and its value of {@link FkColumns} type.
     */
    Map<String, Map<String, FkColumns>> fkMap;

    /** Hashtable of loaded properties. */
    private Properties properties;

    /** The JDBC sub-protocol in the URL used to obtain this connection. */
    private String jdbcSubProtocol;


    /**
     * Constructor.
     *
     * @param tableName
     *         - Name of table
     * @param datatypeMap
     *         - The datatype mappings
     */
    TableSpec(String tableName, HashMap<Integer, String> datatypeMap) {
        this.tableName = tableName;
        this.datatypeMap = datatypeMap;
    }

    public void setProperties(final Properties props) {
        this.properties = props;
    }

    public void setJDBCSubProtocol(final String protocol) {
        this.jdbcSubProtocol = protocol;
    }

    /**
     * Make a RDF predicate name. That means, no spaces in the name.
     *
     * @param col - name of table column
     * @return syntactically valid label
     */
    private String makeLabel(String col) {
        return col.toLowerCase().replace(" ", "_");
    }

    /**
     * Adds a column found on this table.
     *
     * @param col The column's name.
     * @param dataType The column's data type as in java.sql.Types.
     */
    void addColumn(String col, int dataType) {
        if (columns == null) {
            columns = new LinkedHashMap<String, Integer>();
        }
        //columns.put(col.toLowerCase(), dataType);
        columns.put(col, dataType);
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

        //TEST//fkColumns.fkToPkColumns.put(fkCol.toLowerCase(), pkCol.toLowerCase());
        fkColumns.fkToPkColumns.put(fkCol, pkCol);
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
     * @param tablesPkColumns map representing simple primary keys of all tables in this database
     * @return map of foreign keys
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
     * @param tablesPkColumns
     *            - simple primary keys of all tables (key = table, value = table's primary key column).
     * @param interActiveMode
     *            - if true, user will be prompted for each discovered table and foreign key
     * @param addDataTypes
     *            - if true, the table labels in the SQL query will have data types, like customer_id^^xsd:int
     * @return the SQL query
     */
    public String createQuery(Map<String, String> tablesPkColumns, boolean interActiveMode,
            boolean addDataTypes) {

        Map<String, String> simpleForeignKeys = getSimpleForeignKeysToTables(tablesPkColumns);

        String aliasEscapeStart = properties.getProperty("sqldialect." + jdbcSubProtocol + ".alias.before");
        String aliasEscapeEnd = properties.getProperty("sqldialect." + jdbcSubProtocol + ".alias.after");

        StringBuilder query = new StringBuilder("SELECT ");
        if (pkColumns.isEmpty()) {
            query.append("'@' AS id");
        } else {
            // Concatenate primary key columns.
            String pksConcatenated = concatColumns(pkColumns);
            query.append(pksConcatenated).append(" AS id, ").append(pksConcatenated).append(" AS ")
                .append(aliasEscapeStart).append("rdfs:label").append(aliasEscapeEnd);
        }

        String colEscapeStart = properties.getProperty("sqldialect." + jdbcSubProtocol + ".column.before");
        String colEscapeEnd = properties.getProperty("sqldialect." + jdbcSubProtocol + ".column.after");

        ArrayList<String> fkReferences = new ArrayList<String>();
        for (Map.Entry<String, Integer> columnEntry : columns.entrySet()) {

            String col = columnEntry.getKey();
            String type = getXsdDataType(columnEntry.getValue());
            String label = makeLabel(col);
            String pkTable = simpleForeignKeys.get(col);
            if (pkTable != null) {
                boolean exportAsReference =
                        !interActiveMode ? true : ExploreDB.readUserInputBoolean(tableName + "." + col + " is a FK to "
                                + pkTable + ". Export as reference?");
                if (exportAsReference) {
                    label += "->" + ExploreDB.encodeSegment(pkTable);
                    fkReferences.add(col + "->" + pkTable);
                }
            } else {
                pkTable = getFirstMatchingKey(tablesPkColumns, col);
                if (pkTable != null && !pkTable.equals(tableName)) {
                    boolean exportAsReference =
                            !interActiveMode ? true : ExploreDB.readUserInputBoolean(tableName + "." + col
                                    + " has the same name as PK in " + pkTable + ". Export as reference?");
                    if (exportAsReference) {
                        label += "->" + ExploreDB.encodeSegment(pkTable);
                        fkReferences.add(col + "->" + pkTable);
                    }
                }
            }

            query.append(", ").append(colEscapeStart).append(col).append(colEscapeEnd);
            query.append(" AS ");
            query.append(aliasEscapeStart);
            query.append(label);
            if (addDataTypes && !label.contains("->")) {
                if (type.equals("xsd:string")) {
                    // notation for an empty language code
                    query.append("@");
                    query.append(aliasEscapeEnd);
                } else {
                    query.append("^^").append(type).append(aliasEscapeEnd);
                }
            } else {
                query.append(aliasEscapeEnd);
            }
        }
        query.append(" FROM ").append(colEscapeStart).append(tableName).append(colEscapeEnd);

        return query.toString();
    }

    /**
     * Returns an XML Schema data type for the given SQL type (as in java.sql.Types).
     *
     * @param sqlType The given SQL type.
     * @return the XSD type for RDF.
     */
    private String getXsdDataType(int sqlType) {
        String r = datatypeMap.get(Integer.valueOf(sqlType));
        return (r == null) ? "xsd:string" : r;
    }

    /**
     * Returns the given map's first key that pairs with the given value.
     *
     * @param map  Map representing simple primary keys of all tables in this database
     * @param value Name of column
     * @return map's first key that pairs with the given value
     */
    private String getFirstMatchingKey(Map<String, String> map, String value) {

        for (Entry<String, String> entry : map.entrySet()) {
            if (value.equalsIgnoreCase(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Returns a string representing the SQL concatenation of the columns given. The dialect differs depending on the DB-vendor
     * represented by the JDBC sub-protocol.
     *
     * @param columns A list of columns
     * @return Proper SQL syntax of concatenated columns
     */
    private String concatColumns(List<String> columns) {

        StringBuilder result = new StringBuilder();

        String strategy = properties.getProperty("sqldialect." + jdbcSubProtocol + ".concat");
        if (columns != null && !columns.isEmpty()) {
            if (strategy.equals("concat")) {
                result.append("concat(''");
                for (String col : columns) {
                    result.append(", ").append(col);
                }
                result.append(")");
            } else if (strategy.equals("and")) {
                result.append("''");
                for (String col : columns) {
                    result.append(" || ").append(col);
                }
            } else if (strategy.equals("plus")) {
                result.append("''");
                for (String col : columns) {
                    result.append(" + CStr(").append(col).append(")");
                }
            } else {
                result.append("''");
                for (String col : columns) {
                    result.append(" ERROR ").append(col);
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
    /** The datatype mappings. */
    private HashMap<Integer, String> datatypeMap;
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
     *            - if a database access error occurs
     */
    public ExploreDB(Connection dbCon, Properties properties, boolean interActiveMode) throws SQLException {

        con = dbCon;
        props = properties;
        this.interActiveMode = interActiveMode;

        jdbcSubProtocol = getDBProductName(con);

        namespaces = new HashMap<String, String>();
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

        datatypeMap = new HashMap<Integer, String>();
        tables = new HashMap<String, TableSpec>();

        // Get the datatypes from the properties file.
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("datatype.")) {
                Integer type = Datatypes.getSQLType(key.substring(9));
                datatypeMap.put(type, props.getProperty(key));
            }
        }
    }

    /**
     * Get the database product name in lower case.
     *
     * @param dbCon
     *            - the database connection to explore
     * @return the sub-protocol (currently)
     * @throws SQLException
     *            - if a database access error occurs
     */
    static String getDBProductName(Connection dbCon) throws SQLException {
        String jdbcUrl = dbCon.getMetaData().getURL();
        return jdbcUrl.substring(5, jdbcUrl.indexOf(':', 5)).toLowerCase();
        // Could also have used:
        // return dbCon.getMetaData().getDatabaseProductName().toLowerCase()
    }

    /**
     * Get set of tables that should be skipped. Will be amended by user input.
     * Note: tables should already be encoded in properties file, but might also
     * be different case.
     *
     * @return hashtable of table names.
     */
    private HashSet<String> getTablesToSkip() {
        HashSet<String> skipTables = new HashSet<String>();
        String skipTablesProperty = props.getProperty("sqldialect." + jdbcSubProtocol + ".skiptables");
        if (skipTablesProperty != null && !skipTablesProperty.isEmpty()) {
            String[] skipTablesList = skipTablesProperty.split("\\s+");
            for (String t : skipTablesList) {
                skipTables.add(encodeSegment(t));
            }
        }
        return skipTables;
    }
    /**
     * Discover all tables in the database and create SELECT statements for the properties file.
     *
     * @param addDataTypes If true, then add the RDF data types.
     * @throws SQLException
     *            - if a database access error occurs
     */
    public void discoverTables(boolean addDataTypes) throws SQLException {
        DatabaseMetaData dbMetadata = con.getMetaData();

        getTablesFromDB(dbMetadata);
        HashMap<String, String> tablesPkColumns = discoverKeys(dbMetadata);

        // Now that all tables' primary/foreign keys have been set, create every table's query, and set it in properties
        // that will be later used for RDF generation.
        for (Map.Entry<String, TableSpec> entry : tables.entrySet()) {
            String query = entry.getValue().createQuery(tablesPkColumns, interActiveMode, addDataTypes);
            String segment = encodeSegment(entry.getKey());
            props.setProperty(segment.concat(".query"), query);
        }

    }

    /**
     * Investigate the database for a list of tables. Then check if they should be skipped.
     *
     * @param dbMetadata - The metadata from the database
     * @throws SQLException
     *            - if a database access error occurs
     */
    private void getTablesFromDB(DatabaseMetaData dbMetadata) throws SQLException {
            StringBuilder tablesListBuilder = new StringBuilder();
            ResultSet rs = null;
        
        try {
            rs = dbMetadata.getColumns(null, null, "%", "%");

            HashSet<String> skipTables = getTablesToSkip();

            while (rs.next()) {

                String tableName = rs.getString(3);
                if (!skipTables.contains(encodeSegment(tableName))) {

                    TableSpec tableSpec = tables.get(tableName);
                    if (tableSpec == null) {

                        boolean exportThisTable =
                                !interActiveMode ? true : readUserInputBoolean("Export table " + tableName + "?");
                        if (!exportThisTable) {
                            skipTables.add(encodeSegment(tableName));
                            continue;
                        }

                        tableSpec = new TableSpec(tableName, datatypeMap);
                        tableSpec.setProperties(props);
                        tableSpec.setJDBCSubProtocol(jdbcSubProtocol);
                        tables.put(tableName, tableSpec);
                        String segment = encodeSegment(tableName);
                        tablesListBuilder.append(segment).append(" ");
                    }
                    tableSpec.addColumn(rs.getString(4), rs.getInt(5));
                }
            }
            props.setProperty("tables", tablesListBuilder.toString());
        } finally {
            ExploreDB.close(rs);
        }
    }

    /**
     * Loop through the discovered tables, and discover each one's primary and foreign keys too.
     * While at it, remember simple (i.e. non-compound) primary keys of every table for later use below.
     *
     * @param dbMetadata - The metadata from the database
     * @return hashtable of discovered key pairs
     * @throws SQLException
     *            - if a database access error occurs
     */
    private HashMap<String, String> discoverKeys(DatabaseMetaData dbMetadata) throws SQLException {
        ResultSet rs = null;
        HashMap<String, String> tablesPkColumns = new HashMap<String, String>();
        for (Map.Entry<String, TableSpec> entry : tables.entrySet()) {

            String tableName = entry.getKey();
            TableSpec tableSpec = entry.getValue();

            HashMap<Short, String> primKeys = new HashMap<Short, String>();
            rs = dbMetadata.getPrimaryKeys(null, null, tableName);
            while (rs.next()) {
                primKeys.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME").toLowerCase());
            }
            ExploreDB.close(rs);

            tableSpec.pkColumns = ExploreDB.listValuesSortedByKeys(primKeys);
            if (primKeys.size() == 1) {
                tablesPkColumns.put(tableName, primKeys.values().iterator().next());
            }

            rs = dbMetadata.getImportedKeys(null, null, tableName);
            while (rs.next()) {
                tableSpec.addFkColumn(rs.getString("PKTABLE_NAME"), rs.getString("FK_NAME"), rs.getString("FKCOLUMN_NAME"),
                        rs.getString("PKCOLUMN_NAME"), rs.getShort("KEY_SEQ"));
            }
            // No close of rs?
        }
        return tablesPkColumns;
    }


    /**
     * Close a result set ignoring exceptions.
     * @param rs - result set to close
     */
    protected static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                // Deliberately ignore closing exceptions.
            }
        }
    }

    /**
     * List values sorted by keys.
     * @param map of key/value pairs
     * @param <K> Key type
     * @param <V> Value type
     * @return sorted list of values by key
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
     * Post a yes/no question to the user and return the answer.
     * @param question to ask the user
     * @return the answer in boolean
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

    /**
     * Make a segment of an RDF predicate. That means, no spaces in the name.
     *
     * @param table - name of table
     * @return syntactically valid segment
     */
    static String encodeSegment(String table) {
        return StringEncoder.encodeToIRI(table.toLowerCase());
    }

}
