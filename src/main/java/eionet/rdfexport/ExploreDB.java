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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * Explore Database. The purpose of this class is to discover the table
 * relationships and then create an export in RDF where the tables are
 * interlinked. This code is an early start on an export to RDF module that
 * can automatically find all tables in a database, the internal relations
 * and then export the database as one RDF file.
 *
 * Intended use is to convert MS-Access databases.
 *
 * To use it with MS-Access download the trial version of the HXTT driver
 * from http://www.hxtt.com/access.zip or http://www.hxtt.com/access.html.
 * The trial version will only return 1000 rows and allow 50 queries in
 * the same connection.
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

    /** All the primary columns in all tables. */
    private HashMap<String, String> tablesPkColumns;

    /** Hashtable of loaded properties. */
    private Properties props;
    /** The JDBC sub-protocol in the URL used to obtain this connection. */
    private String jdbcSubProtocol;

    /**
     *
     * Class constructor.
     *
     * @param dbCon
     *            - the database connection to explore
     * @param properties
     *            - properties where to write the discovered tables, queries, etc.
     *
     * @throws SQLException
     *            - if a database access error occurs
     */
    public ExploreDB(Connection dbCon, Properties properties) throws SQLException {

        con = dbCon;
        props = properties;

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
                skipTables.add(t.toLowerCase());
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
        List<TableSpec> tablesToExport = listTables();
        registerTables(tablesToExport);
        createQuery(tablesToExport, addDataTypes);
    }

    /**
     * Return a list of tables in the database. Skips tables as configured in
     * the properties file.
     *
     * @return list of tables found in the database.
     * @throws SQLException
     *            - if a database access error occurs
     */
    public List<TableSpec> listTables() throws SQLException {
        ArrayList<TableSpec> tableList = new ArrayList<TableSpec>();
        HashSet<String> skipTables = getTablesToSkip();
        DatabaseMetaData dbMetadata = con.getMetaData();
        ResultSet rs = null;
        rs = dbMetadata.getTables(null, null, "%", new String[] {"TABLE"});
        while (rs.next()) {
            String tableName = rs.getString(3);
            if (!skipTables.contains(tableName.toLowerCase())) {
                TableSpec tableSpec = new TableSpec(tableName);
                tableSpec.tableCatalog = rs.getString(1);
                tableSpec.tableSchema = rs.getString(2);
                tableList.add(tableSpec);
            }
        }
        ExploreDB.close(rs);
        return tableList;
    }

    /**
     * When the user has chosen the tables, he registers them. That loads the column names.
     * @param tableList - list of tables to register.
     * @throws SQLException
     *            - if a database access error occurs
     */
    public void registerTables(List<TableSpec> tableList) throws SQLException {

        StringBuilder tablesListBuilder = new StringBuilder();

        DatabaseMetaData dbMetadata = con.getMetaData();
        for (TableSpec tableSpec : tableList) {
            tableSpec.setProperties(props);
            tableSpec.setJDBCSubProtocol(jdbcSubProtocol);
            tables.put(tableSpec.tableName, tableSpec);
            String segment = encodeSegment(tableSpec.tableName);
            tablesListBuilder.append(segment).append(" ");
            ResultSet rs = dbMetadata.getColumns(tableSpec.tableCatalog, tableSpec.tableSchema, tableSpec.tableName, "%");
            while (rs.next()) {
                tableSpec.addColumn(rs.getString(4), rs.getInt(5));
            }
        }
        // Assign to a member variable
        tablesPkColumns = discoverKeys(dbMetadata);
        //FIXME: Unwanted side-effect?
        props.setProperty("tables", tablesListBuilder.toString());
    }

    /**
     * Get a table.
     *
     * @param table - the name of the table
     * @return the table specification.
     * @throws SQLException
     *            - if a database access error occurs
     */
    public TableSpec getTable(String table) throws SQLException {
        return tables.get(table);
    }

    /**
     * Get a table.
     *
     * @param table - the name of the table
     */
    public Set<String> getColumns(String table) {
        TableSpec tabSpec = tables.get(table);
        Set<String> columns = tabSpec.columns.keySet();
        return columns;
    }

    /**
     * Returns a map of columns that are simple (i.e non-compound) foreign keys
     * pointing to some simple primary keys of other tables. The map's keys are
     * names of such columns. The tables they point to are the map's values.
     *
     * @param table - name of table
     * @return - map of columns
     * @throws SQLException
     *            - if a database access error occurs
     */
    public Map<String, String> getSimpleForeignKeys(String table) throws SQLException {
        TableSpec tabSpec = tables.get(table);
        Map<String, String> simpleForeignKeys = tabSpec.getSimpleForeignKeysToTables(tablesPkColumns);
        return simpleForeignKeys;
    }

    /**
     * Remove a foreign key -&gt; primary key reference.
     *
     * @param table - name of table
     */
    public void removeForeignKey(String table, String key, String referenceTable) {
        //TODO
    }

    /**
     * Generate a SQL query for a table.
     *
     */
    public void createQuery(TableSpec tabSpec, boolean addDataTypes) throws SQLException {
        String query = tabSpec.createQuery(tablesPkColumns, addDataTypes, datatypeMap);
        String segment = encodeSegment(tabSpec.tableName);
        props.setProperty(segment.concat(".query"), query);
    }

    /**
     * Generate a SQL query for each table in the list.
     *
     */
    public void createQuery(List<TableSpec> tableList, boolean addDataTypes) throws SQLException {
        for (TableSpec tableSpec : tableList) {
            createQuery(tableSpec, addDataTypes);
        }
    }

    /**
     * Loop through the discovered tables, and discover each one's primary and
     * foreign keys too. While at it, remember simple (i.e. non-compound)
     * primary keys of every table for later use below.
     *
     * @param dbMetadata - The metadata from the database
     * @return hashtable of discovered key pairs
     * @throws SQLException
     *            - if a database access error occurs
     */
    private HashMap<String, String> discoverKeys(DatabaseMetaData dbMetadata) throws SQLException {
        ResultSet rs = null;
        HashMap<String, String> tPkColumns = new HashMap<String, String>();

        for (Entry<String, TableSpec> entry : tables.entrySet()) {

            String tableName = entry.getKey();
            TableSpec tableSpec = entry.getValue();

            HashMap<Short, String> primKeys = new HashMap<Short, String>();
            rs = dbMetadata.getPrimaryKeys(null, null, tableName);
            while (rs.next()) {
                primKeys.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME").toLowerCase());
            }
            ExploreDB.close(rs);

            tableSpec.primaryKeyColumns = ExploreDB.listValuesSortedByKeys(primKeys);
            if (primKeys.size() == 1) {
                tPkColumns.put(tableName, primKeys.values().iterator().next());
            }

            rs = dbMetadata.getImportedKeys(null, null, tableName);
            while (rs.next()) {
                tableSpec.addFkColumn(rs.getString("PKTABLE_NAME"),
                        rs.getString("FK_NAME"), rs.getString("FKCOLUMN_NAME"),
                        rs.getString("PKCOLUMN_NAME"), rs.getShort("KEY_SEQ"));
            }
            // No close of rs?
        }
        return tPkColumns;
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
     * Make a segment of an RDF predicate. That means, no spaces in the name.
     *
     * @param table - name of table
     * @return syntactically valid segment
     */
    static String encodeSegment(String table) {
        return StringEncoder.encodeToIRI(table.toLowerCase());
    }

}
