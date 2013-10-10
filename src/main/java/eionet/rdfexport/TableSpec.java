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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    /** Name of schema the table is in. */
    String tableCatalog;

    /** Name of schema the table is in. */
    String tableSchema;

    /** Name of table. */
    String tableName;

    /** The map of columns found on this table. Key = column name, value = column data type as in java.sql.Types. */
    LinkedHashMap<String, Integer> columns;

    /** Columns constituting the primary key, in that same order. */
    List<String> primaryKeyColumns;

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

    /** Flag to determine if the table should be output. */
    //private boolean chosen;

    /**
     * Constructor.
     *
     * @param tableName
     *         - Name of table
     */
    TableSpec(String tableName) {
        this.tableName = tableName;
    }

    void setProperties(final Properties props) {
        this.properties = props;
    }

    void setJDBCSubProtocol(final String protocol) {
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
    void addFkColumn(String pkTable, String fkName, String fkCol, String pkCol, int pos) {

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

        fkColumns.fkToPkColumns.put(fkCol, pkCol);
        fkColumns.positions.put(pos, fkCol.toLowerCase());
    }

    /**
     * Returns a map of columns that are simple (i.e. non-compound) foreign keys
     * pointing to some simple primary keys of other tables. The map's keys are
     * names of such columns. The tables they point to are the map's values.
     *
     * As an input, this method takes a map representing simple primary keys of
     * all tables in this database. If a table does not have simple key, it is
     * not listed here. The map's keys are table names, and the values are the
     * names of single columns that constitute the particular table's primary key.
     *
     * @param tablesPkColumns Map representing simple primary keys of all tables in this database
     * @return Map of foreign keys
     */
    Map<String, String> getSimpleForeignKeysToTables(Map<String, String> tablesPkColumns) {

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
     * Generate a SQL query for a table. The table is specified in the tableName member variable.
     *
     * @param tablesPkColumns
     *            - simple primary keys of all tables (key = table, value = table's primary key column).
     * @param addDataTypes
     *            - if true, the table labels in the SQL query will have data types, like customer_id^^xsd:int
     * @param datatypeMap - the datatype mappings.
     * @return the SQL query
     */
    public String createQuery(Map<String, String> tablesPkColumns,
            boolean addDataTypes, HashMap<Integer, String> datatypeMap) {

        Map<String, String> simpleForeignKeys = getSimpleForeignKeysToTables(tablesPkColumns);

        String aliasEscapeStart = properties.getProperty("sqldialect." + jdbcSubProtocol + ".alias.before");
        String aliasEscapeEnd = properties.getProperty("sqldialect." + jdbcSubProtocol + ".alias.after");

        StringBuilder query = new StringBuilder("SELECT ");
        if (primaryKeyColumns == null || primaryKeyColumns.isEmpty()) {
            query.append("'@' AS id");
        } else {
            // Concatenate primary key columns.
            String pksConcatenated = concatColumns(primaryKeyColumns);
            query.append(pksConcatenated).append(" AS id, ").append(pksConcatenated).append(" AS ")
                .append(aliasEscapeStart).append("rdfs:label").append(aliasEscapeEnd);
        }

        String colEscapeStart = properties.getProperty("sqldialect." + jdbcSubProtocol + ".column.before");
        String colEscapeEnd = properties.getProperty("sqldialect." + jdbcSubProtocol + ".column.after");

        ArrayList<String> fkReferences = new ArrayList<String>();
        for (Map.Entry<String, Integer> columnEntry : columns.entrySet()) {

            String col = columnEntry.getKey();
            String type = getXsdDataType(columnEntry.getValue(), datatypeMap);
            String label = makeLabel(col);
            String pkTable = simpleForeignKeys.get(col);
            if (pkTable != null) {
                label += "->" + ExploreDB.encodeSegment(pkTable);
                fkReferences.add(col + "->" + pkTable);
            } else {
                pkTable = getFirstMatchingKey(tablesPkColumns, col);
                if (pkTable != null && !pkTable.equals(tableName)) {
                    label += "->" + ExploreDB.encodeSegment(pkTable);
                    fkReferences.add(col + "->" + pkTable);
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
        query.append(" FROM ");
        if (tableSchema != null) {
            query.append(colEscapeStart).append(tableSchema).append(colEscapeEnd).append(".");
        }
        query.append(colEscapeStart).append(tableName).append(colEscapeEnd);

        return query.toString();
    }

    /**
     * Returns an XML Schema data type for the given SQL type (as in java.sql.Types).
     *
     * @param sqlType The given SQL type.
     * @param datatypeMap - the datatype mappings.
     * @return the XSD type for RDF.
     */
    private String getXsdDataType(int sqlType, HashMap<Integer, String> datatypeMap) {
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
     * Returns a string representing the SQL concatenation of the columns given.
     * The dialect differs depending on the DB-vendor represented by the JDBC sub-protocol.
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
