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
 *        Søren Roug, EEA
 *        Juhan Voolaid
 */
package eionet.rdfexport;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

/**
 * RDF generator. The queries are stored in a properties file. There are two
 * types of queries. A plain select and an attributes table. For the plain
 * select the class will use the first column as the <em>identifier</em>, and
 * create RDF properties for the other columns.
 *
 * For the attributes table the result must have one + X * four columns:
 * 1. id, 2. attribute name, 3. value, 4. datatype, 5. languagecode,
 * 6. attribute name, 7. value, 8. datatype, 9. languagecode, etc.
 */
public class GenerateRDF {

    /** Tell the DB driver how much to fetch at a time. */
    private static final int FETCH_SIZE = 1000;
    /** Connection to database. */
    private Connection con;
    /** Names, types and langcodes of columns. */
    private RDFField[] names;
    /** The URL of the null namespace. */
    private String nullNamespace;
    /** If output has started, then you can't change the nullNamespace. */
    private Boolean rdfHeaderWritten = false;
    /** The properties that are object properties. They point to another object. */
    private HashMap<String, String> objectProperties;
    /** The datatype mappings. */
    //private HashMap<Integer, String> datatypeMap;
    /** All the tables in the properties file. */
    private String[] tables = new String[0];
    /** Hashtable of loaded properties. */
    private Properties props;

    /** The object doing the serialisation. */
    private ResourceWriter resourceWriter;

    /**
     * Constructor.
     *
     * @param writer
     *            - The output stream to send output to
     * @param dbCon
     *            - The database connection
     * @param properties
     *            - The properties
     * @throws IOException
     *             - if the properties file is missing
     * @throws SQLException
     *             - if the SQL database is not available
     */
    public GenerateRDF(OutputStream writer, Connection dbCon,
                Properties properties) throws IOException, SQLException {
        this(new OutputStreamWriter(writer, "UTF-8"), dbCon, properties);
    }

    /**
     * Constructor.
     *
     * @param writer
     *            - The output stream to send output to
     * @param dbCon
     *            - The database connection
     * @param properties
     *            - The properties
     * @throws IOException
     *             - if the properties file is missing
     * @throws SQLException
     *             - if the SQL database is not available
     */
    public GenerateRDF(OutputStreamWriter writer, Connection dbCon,
                Properties properties) throws IOException, SQLException {
        if (!"UTF8".equals(writer.getEncoding())) {
            throw new RuntimeException("Only UTF-8 is supported!");
        }
        props = properties;

        resourceWriter = new ResourceWriterXML(writer);
        // Generate exception if there is no vocabulary property
        resourceWriter.setVocabulary(props.getProperty("vocabulary"));
        resourceWriter.setBaseURL(props.getProperty("baseurl"));
        boolean emptyStringIsNull = Boolean.parseBoolean(props.getProperty("emptystringisnull", "false"));
        resourceWriter.setEmptyStringIsNull(emptyStringIsNull);


        String tablesProperty = props.getProperty("tables");
        if (tablesProperty != null && !tablesProperty.isEmpty()) {
            tables = tablesProperty.split("\\s+");
        }

        con = dbCon;

        objectProperties = new HashMap<String, String>();
        //datatypeMap = new HashMap<Integer, String>();
        // Get the namespaces from the properties file.
        // Get the objectproperties from the properties file.
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("xmlns.")) {
                resourceWriter.addNamespace(key.substring(6), props.getProperty(key));
            } else if (key.startsWith("objectproperty.")) {
                String value = props.getProperty(key);
                addObjectProperty(key.substring(15), "->".concat(value));
            } else if (key.startsWith("datatype.")) {
                Integer type = Datatypes.getSQLType(key.substring(9));
                Datatypes.setRDFType(type, props.getProperty(key));
            }
        }
    }

    /**
     * The user can choose one record to output. This is done by inserting a
     * HAVING ID=... into the SELECT statement. (using HAVING is slow). If
     * the ID is numeric, then Mysql will convert the type to match.
     *
     * @param query
     *            - SQL query to patch
     * @param identifier
     *            to insert into query
     * @return patched SQL query
     */
    String injectHaving(String query, String identifier) {
        String[] keywords = {" order ", " limit ", " procedure ", " into ", " for ", " lock "};
        String lquery = query.toLowerCase().replace("\n", " ");
        int insertBefore = lquery.length();
        for (String k : keywords) {
            int i = lquery.indexOf(k);
            if (i >= 0 && i < insertBefore) {
                insertBefore = i;
            }
        }
        int h = lquery.indexOf(" having ");
        if (h == -1) {
            query =
                    query.substring(0, insertBefore) + " HAVING id='" + identifier.replace("'", "''") + "'"
                            + query.substring(insertBefore);
        } else {
            query = query.substring(0, h + 8) + "id='" + identifier.replace("'", "''") + "' AND " + query.substring(h + 8);
        }
        return query;
    }

    /**
     * The user can choose one record to output. This is done by inserting a
     * WHERE <em>key</em>=... into the SELECT statement. If the ID is numeric,
     * then Mysql will convert the type to match.
     *
     * @param query
     *            - SQL query to patch
     * @param key
     *            - Name of column that can be used as key in index
     * @param identifier
     *            to insert into query
     * @return patched SQL query
     */
    String injectWhere(String query, String key, String identifier) {
        // Handle WHERE for key hints
        String[] keywords = {" group ", " having ", " order ", " limit ", " procedure ", " into ", " for ", " lock "};
        String lquery = query.toLowerCase().replace("\n", " ");
        int insertBefore = lquery.length();
        for (String k : keywords) {
            int i = lquery.indexOf(k);
            if (i >= 0 && i < insertBefore) {
                insertBefore = i;
            }
        }
        int h = lquery.indexOf(" where ");
        if (h == -1) {
            query =
                    query.substring(0, insertBefore) + " WHERE " + key + "='" + identifier.replace("'", "''") + "'"
                            + query.substring(insertBefore);
        } else {
            query = query.substring(0, h + 7) + key + "='" + identifier.replace("'", "''") + "' AND " + query.substring(h + 7);
        }
        return query;
    }

    /**
     * Return all known tables in properties file.
     *
     * @return list of strings.
     */
    public String[] getAllTables() {
        return tables == null ? null : Arrays.copyOf(tables, tables.length);
    }

    /**
     * Export a table as RDF. A table can consist of several queries.
     *
     * @param table
     *            - name of table in properties file
     * @throws SQLException
     *             if there is a database problem.
     * @throws IOException
     *             - if the output is not open.
     */
    public void exportTable(String table) throws SQLException, IOException {
        exportTable(table, null);
    }

    /**
     * Export a table as RDF. A table can consist of several queries specified
     * as property names table.query1, table.query2, table.attributetable1 etc.
     * The queries are sorted on name before being executed with the x.query
     * first then x.attributetable second.
     *
     * @param table
     *            - name of table in properties file
     * @param identifier
     *            - primary key of the record we want or null for all records.
     * @throws SQLException
     *             if there is a database problem.
     * @throws IOException
     *             - if the output is not open.
     */
    public void exportTable(String table, String identifier) throws SQLException, IOException {
        String voc = props.getProperty(table.concat(".vocabulary"));
        if (voc != null) {
            resourceWriter.setVocabulary(voc);
        } else {
            resourceWriter.setVocabulary(props.getProperty("vocabulary"));
        }
        if (!rdfHeaderWritten) {
            resourceWriter.writeRdfHeader();
            rdfHeaderWritten = true;
        }
        Boolean firstQuery = true;
        String rdfClass = table.substring(0, 1).toUpperCase() + table.substring(1).toLowerCase();
        rdfClass = props.getProperty(table.concat(".class"), rdfClass);
        TreeSet<String> sortedProps = new TreeSet<String>(props.stringPropertyNames());
        String tableQueryKey = table.concat(".query");

        for (String key : sortedProps) {
            if (key.startsWith(tableQueryKey)) {
                String query = props.getProperty(key);
                if (identifier != null) {
                    String tableKeyKey = table.concat(".key").concat(key.substring(tableQueryKey.length()));
                    String whereKey = props.getProperty(tableKeyKey);
                    if (whereKey != null) {
                        query = injectWhere(query, whereKey, identifier);
                    } else {
                        query = injectHaving(query, identifier);
                    }
                }

                runQuery(table, query, firstQuery ? rdfClass : "rdf:Description");
                firstQuery = false;
            }
        }

        String tableAttributesKey = table.concat(".attributetable");

        for (String key : sortedProps) {
            if (key.startsWith(tableAttributesKey)) {
                String query = props.getProperty(key);
                if (identifier != null) {
                    String tableKeyKey = table.concat(".attributekey").concat(key.substring(tableAttributesKey.length()));
                    String whereKey = props.getProperty(tableKeyKey);
                    if (whereKey != null) {
                        query = injectWhere(query, whereKey, identifier);
                    } else {
                        query = injectHaving(query, identifier);
                    }
                }
                runAttributes(table, query, firstQuery ? rdfClass : "rdf:Description");
                firstQuery = false;
            }
        }
    }

    /**
     * Looks for 'class' and 'query' properties from the rdf properties file
     * like this.
     *
     * <pre>
     *  class = bibo:Document
     *  query = SELECT NULL AS 'id', \
     *    'GEMET RDF file' AS 'rdfs:label', \
     *    'Søren Roug' AS 'dcterms:creator', \
     * 'http://creativecommons.org/licenses/by/2.5/dk/' AS 'dcterms:licence->'
     *
     * </pre>
     * When found, {@code <bibo:Document rdf:about="">} section with given
     * properties will be exported.
     * @throws IOException
     *             - if the output is not open.
     * @throws SQLException
     *             if there is a database problem.
     */
    public void exportDocumentInformation() throws IOException, SQLException {
        String rdfClass = props.getProperty("class", "rdf:Description");

        String queryTable = props.getProperty("query");
        if (queryTable != null) {
            if (!rdfHeaderWritten) {
                resourceWriter.writeRdfHeader();
                rdfHeaderWritten = true;
            }
            runQuery("", queryTable, rdfClass);
            rdfClass = "rdf:Description"; // Any further declaration must be anonymous
        }
        String attributesTable = props.getProperty("attributetable");
        if (attributesTable != null) {
            if (!rdfHeaderWritten) {
                resourceWriter.writeRdfHeader();
                rdfHeaderWritten = true;
            }
            runAttributes("", attributesTable, rdfClass);
            rdfClass = "rdf:Description"; // Any further declaration must be anonymous
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
     * Run a query. First value is the key. The others are the attributes. The
     * column names are the attribute names. If first value is null, then the
     * attributes are assigned to the namespace of the table.
     *
     * @param segment
     *            - the namespace of the table
     * @param sql
     *            - the query to run.
     * @param rdfClass
     *            - the class to assign or rdf:Description
     * @throws SQLException
     *             - if the SQL database is not available
     * @throws IOException
     *             - if the output is not open.
     */
    private void runQuery(String segment, String sql, String rdfClass) throws SQLException, IOException {

        ResultSet rs = null;
        Statement stmt = null;
        String currentId = "/..";
        Integer currentRow = 0;
        Boolean firstTime = true;

        try {
            stmt = con.createStatement();
            stmt.setFetchSize(FETCH_SIZE);
            if (stmt.execute(sql)) {

                rs = stmt.getResultSet();

                ResultSetMetaData rsmd = rs.getMetaData();
                queryStruct(rsmd);

                int numcols = rsmd.getColumnCount();

                while (rs.next()) {

                    currentRow += 1;

                    String id = rs.getString(1);
                    if (id != null && id.equals("@")) {
                        id = currentRow.toString();
                    }

                    if (currentId != null && !currentId.equals(id)) {
                        if (!firstTime) {
                            resourceWriter.writeEndResource(rdfClass);
                        }
                        resourceWriter.writeStartResource(rdfClass, segment, id);
                        currentId = id;
                        firstTime = false;
                    }

                    for (int i = 2; i <= numcols; i++) {
                        resourceWriter.writeProperty(names[i], rs.getObject(i));
                    }
                }
                if (!firstTime) {
                    resourceWriter.writeEndResource(rdfClass);
                }
            }
        } finally {
            closeIgnoringExceptions(rs);
            closeIgnoringExceptions(stmt);
        }
    }

    /**
     * Query attributes table. The result must have one + X * four columns.
     * 1. id, 2. attribute name, 3. value, 4. datatype,
     * 5. languagecode, 6. attribute name, 7. value, 8. datatype,
     * 9. languagecode, etc. If id is null, then the attributes are assigned
     * to the namespace of the table.
     *
     * @param segment
     *            - the namespace of the table
     * @param sql
     *            - the query
     * @param rdfClass
     *            - the class to assign or rdf:Description
     * @throws SQLException
     *             - if the SQL database is not available
     * @throws IOException
     *             - if the output is not open.
     */
    private void runAttributes(String segment, String sql, String rdfClass) throws SQLException, IOException {

        ResultSet rs = null;
        Statement stmt = null;
        String currentId = "/..";
        Integer currentRow = 0;
        Boolean firstTime = true;

        try {
            stmt = con.createStatement();
            stmt.setFetchSize(FETCH_SIZE);

            if (stmt.execute(sql)) {
                rs = stmt.getResultSet();

                ResultSetMetaData rsmd = rs.getMetaData();
                int numcols = rsmd.getColumnCount();

                while (rs.next()) {

                    currentRow += 1;

                    RDFField property = new RDFField();
                    String id = rs.getString(1);
                    if (id != null && id.equals("@")) {
                        id = currentRow.toString();
                    }

                    if (currentId != null && !currentId.equals(id)) {
                        if (!firstTime) {
                            resourceWriter.writeEndResource(rdfClass);
                        }
                        resourceWriter.writeStartResource(rdfClass, segment, id);
                        currentId = id;
                        firstTime = false;
                    }

                    for (int b = 2; b < numcols; b += 4) {
                        property.name = rs.getObject(b + 0).toString();
                        if (rs.getObject(b + 2) == null) {
                            if (objectProperties.containsKey(property.name)) {
                                property.datatype = objectProperties.get(property.name);
                            } else {
                                property.datatype = "";
                            }
                        } else {
                            property.datatype = rs.getObject(b + 2).toString();
                        }
                        if (rs.getObject(b + 3) != null) {
                            property.langcode = rs.getObject(b + 3).toString();
                        } else {
                            property.langcode = "";
                        }
                        resourceWriter.writeProperty(property, rs.getObject(b + 1));
                    }
                }
                if (!firstTime) {
                    resourceWriter.writeEndResource(rdfClass);
                }
            }
        } finally {
            GenerateRDF.closeIgnoringExceptions(rs);
            GenerateRDF.closeIgnoringExceptions(stmt);
        }
    }

    /**
     * Close resultset.
     * @param rs - result set
     */
    private static void closeIgnoringExceptions(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                // Deliberately ignore.
            }
        }
    }

    /**
     * Close statement.
     * @param stmt - statement
     */
    private static void closeIgnoringExceptions(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                // Deliberately ignore.
            }
        }
    }

    /**
     * Get the metadata from the columns. Check what datatype the database
     * delivers. but override if the user has specified something else in the
     * column label.
     *
     * @param rsmd
     *            - metadata extracted from database.
     * @throws SQLException
     *             - if the SQL database is not available
     */
    private void queryStruct(ResultSetMetaData rsmd) throws SQLException {
        Integer dbDatatype;
        String rdfDatatype = "";
        int numcols = rsmd.getColumnCount();

        this.names = new RDFField[numcols + 1];

        for (int i = 1; i <= numcols; i++) {
            dbDatatype = rsmd.getColumnType(i);
            //rdfDatatype = datatypeMap.get(dbDatatype);
            rdfDatatype = Datatypes.getRDFType(dbDatatype);
            if (rdfDatatype == null) {
                rdfDatatype = "";
            }
            String columnLabel = rsmd.getColumnLabel(i);
            if (objectProperties.containsKey(columnLabel)) {
                rdfDatatype = objectProperties.get(columnLabel).toString();
            }
            names[i] = parseName(columnLabel, rdfDatatype);
        }
    }

    /**
     * Parses a column label. It can be parsed into three parts: name,
     * datatype, language.
     * <ul>
     * <li>hasRef-&gt; becomes "hasRef","-&gt;",""</li>
     * <li>hasRef-&gt;expert becomes "hasRef","-&gt;expert",""</li>
     * <li>price^^xsd:decimal becomes "price","xsd:decimal",""</li>
     * <li>rdfs:label@fr becomes "rdfs:label","","fr"</li>
     * </ul>
     *
     * @param complexname
     *            - name containing column name plus datatype or language code.
     * @param datatype
     *            - suggested datatype from database.
     * @return RDFField - struct of three strings: Name, datatype and langcode.
     */
    RDFField parseName(String complexname, String datatype) {
        RDFField result = new RDFField();
        String name = complexname;
        String language = "";

        int foundReference = complexname.indexOf("->");
        if (foundReference >= 0) {
            name = complexname.substring(0, foundReference);
            datatype = complexname.substring(foundReference);
        } else {
            int foundDatatype = complexname.indexOf("^^");
            if (foundDatatype >= 0) {
                name = complexname.substring(0, foundDatatype);
                datatype = complexname.substring(foundDatatype + 2);
            } else {
                int foundLanguage = complexname.indexOf("@");
                if (foundLanguage >= 0) {
                    name = complexname.substring(0, foundLanguage);
                    language = complexname.substring(foundLanguage + 1);
                    datatype = "";
                }
            }
        }
        result.name = name;
        result.datatype = datatype;
        result.langcode = language;
        return result;
    }

    /**
     * Generate the RDF footer element.
     *
     * @throws IOException
     *             - if the output is not open.
     */
    public void writeRdfFooter() throws IOException {
        resourceWriter.writeRdfFooter();
    }
}
// vim: set expandtab sw=4 :
