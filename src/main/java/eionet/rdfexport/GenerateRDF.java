package eionet.rdfexport;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

/**
 * A struct to hold a complex type. No need to do data encapsulation.
 */
class RDFField {
    /** Name of column. */
    String name;
    /** Datatype of column. */
    String datatype;
    /** Language code of column. */
    String langcode;

    /**
     * Constructor.
     */
    RDFField() {
        name = "";
        datatype = "";
        langcode = "";
    }

    /**
     * Constructor.
     * @param n - name
     * @param dt - datatype
     * @param l - language code
     */
    RDFField(String n, String dt, String l) {
        name = n;
        datatype = dt;
        langcode = l;
    }
}

/**
 * RDF generator. The queries are stored in a properties file. There are two types of queries. A plain select and an attributes
 * table. For the plain select the class will use the first column as the <em>identifier</em>, and create RDF properties for the
 * other columns.
 *
 * For the attributes table the result must have one + X * four columns: 1. id, 2. attribute name, 3. value, 4. datatype, 5.
 * languagecode, 6. attribute name, 7. value, 8. datatype, 9. languagecode, etc.
 */
public class GenerateRDF {

    /** Format of xsd:date value. */
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    /** Format of xsd:dateTime value. */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";

    /** Base of XML file. */
    private String baseurl;
    /** Connection to database. */
    private Connection con;
    /** Names, types and langcodes of columns. */
    private RDFField[] names;
    /** The URL of the null namespace. */
    private String nullNamespace;
    /** If output has started, then you can't change the nullNamespace. */
    private Boolean rdfHeaderWritten = false;
    /** The namespaces to add to the rdf:RDF element. */
    private HashMap<String, String> namespaces;
    /** The properties that are object properties. They point to another object. */
    private HashMap<String, String> objectProperties;
    /** The datatype mappings. */
    private HashMap<String, String> datatypeMap;
    /** All the tables in the properties file. */
    private String[] tables = new String[0];
    /** Hashtable of loaded properties. */
    private Properties props;
    /** The output stream to send output to. */
    private OutputStreamWriter outputStream;
    /** Date format. */
    private SimpleDateFormat dateFormat;
    /** Date-time format. */
    private SimpleDateFormat dateTimeFormat;

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
    public GenerateRDF(OutputStream writer, Connection dbCon, Properties properties) throws IOException, SQLException {
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
    public GenerateRDF(OutputStreamWriter writer, Connection dbCon, Properties properties) throws IOException, SQLException {
        outputStream = writer;
        if (!"UTF8".equals(outputStream.getEncoding())) {
            throw new RuntimeException("Only UTF-8 is supported!");
        }
        props = properties;

        String tablesProperty = props.getProperty("tables");
        if (tablesProperty != null && !tablesProperty.isEmpty()) {
            tables = tablesProperty.split("\\s+");
        }

        con = dbCon;
        // Generate exception if there is no vocabulary property
        setVocabulary(props.getProperty("vocabulary"));
        baseurl = props.getProperty("baseurl");

        namespaces = new HashMap<String, String>();
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

        objectProperties = new HashMap<String, String>();
        datatypeMap = new HashMap<String, String>();
        // Get the namespaces from the properties file.
        // Get the objectproperties from the properties file.
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("xmlns.")) {
                addNamespace(key.substring(6), props.getProperty(key));
            } else if (key.startsWith("objectproperty.")) {
                String value = props.getProperty(key);
                addObjectProperty(key.substring(15), "->".concat(value));
            } else if (key.startsWith("datatype.")) {
                datatypeMap.put(key.substring(9), props.getProperty(key));
            }
        }

        dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateTimeFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
    }

    /**
     * Called from the other methods to do the output.
     *
     * @param v
     *            - value to print.
     * @throws IOException
     *             - if the output is not open.
     */
    private void output(String v) throws IOException {
        outputStream.write(v);
    }

//  public void close() throws IOException {
//      outputStream.flush();
//      outputStream.close();
//  }

    /**
     * Write a property. If the property.datatype is "->" then it is a resource reference.
     *
     * @param property
     *            triple consisting of name, datatype and langcode
     * @param value
     *            from database.
     * @throws IOException
     *             - if the output is not open.
     */
    void writeProperty(RDFField property, Object value) throws IOException {
        String typelangAttr = "";

        if (value == null) {
            return;
        }
        output(" <");
        output(property.name);
        if (property.datatype.startsWith("->")) {
            // Handle pointers
            if (property.datatype.length() == 2) {
                // Handle the case where the value contains the pointer.
                output(" rdf:resource=\"");
                output(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(value.toString())));
                output("\"/>\n");
            } else {
                // Handle the case of ->countries or ->http://...
                // If the ref-segment contains a colon then it can't be a fragment
                // http://www.w3.org/TR/REC-xml-names/#NT-NCName
                String refSegment = property.datatype.substring(2);
                output(" rdf:resource=\"");
                if (baseurl == null && refSegment.indexOf(":") == -1) {
                    output("#");
                }
                output(StringEncoder.encodeToIRI(refSegment));
                output("/");
                output(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(value.toString())));
                output("\"/>\n");
            }
            return;
        } else if (!"".equals(property.datatype)) {
            if (property.datatype.startsWith("xsd:")) {
                property.datatype = "http://www.w3.org/2001/XMLSchema#" + property.datatype.substring(4);
            }
            typelangAttr = " rdf:datatype=\"" + property.datatype + '"';
        } else if (!"".equals(property.langcode)) {
            typelangAttr = " xml:lang=\"" + property.langcode + '"';
        }
        output(typelangAttr);
        output(">");
        output(getFormattedValue(value));
        output("</");
        output(property.name);
        output(">\n");
    }

    /**
     * The user can choose one record to output. This is done by inserting a HAVING ID=... into the SELECT statement. (using HAVING
     * is slow). If the ID is numeric, then Mysql will convert the type to match
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
     * The user can choose one record to output. This is done by inserting a WHERE <em>key</em>=... into the SELECT statement. If
     * the ID is numeric, then Mysql will convert the type to match
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
     * Export a table as RDF. A table can consist of several queries specified as property names table.query1, table.query2,
     * table.attributetable1 etc. The queries are sorted on name before being executed with the x.query first then x.attributetable
     * second.
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
            setVocabulary(voc);
        } else {
            setVocabulary(props.getProperty("vocabulary"));
        }
        if (!rdfHeaderWritten) {
            rdfHeader();
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
     * Looks for 'class' and 'query' properties from the rdf properties file like this:
     *
     * <pre>
     *  class = bibo:Document
     *  query = SELECT NULL AS 'id', \
     *    'GEMET RDF file' AS 'rdfs:label', \
     *    'Søren Roug' AS 'dcterms:creator', \
     * 'http://creativecommons.org/licenses/by/2.5/dk/' AS 'dcterms:licence->'
     *
     * </pre>
     * When found, {@code <bibo:Document rdf:about="">} section with given properties will be exported.
     * @throws IOException
     *             - if the output is not open.
     * @throws SQLException
     *             if there is a database problem.
     */
    public void exportDocumentInformation() throws IOException, SQLException {
        TreeSet<String> sortedProps = new TreeSet<String>(props.stringPropertyNames());
        String rdfClass = null;
        String query = null;
        for (String prop : sortedProps) {
            if (prop.trim().equalsIgnoreCase("class")) {
                rdfClass = props.getProperty(prop);
            }
            if (prop.trim().equalsIgnoreCase("query")) {
                query = props.getProperty(prop);
            }
        }
        if (query != null && query.length() > 0 && rdfClass != null && rdfClass.length() > 0) {
            runQuery("", query, rdfClass);
        }
    }

    /**
     * Generate the RDF footer element.
     *
     * @throws IOException
     *             - if the output is not open.
     */
    public void writeRdfFooter() throws IOException {
        output("</rdf:RDF>\n");
        outputStream.flush();
    }

    /**
     * Returns formatted string representation of the value object.
     *
     * @param value - value to format
     * @return the formatted string
     */
    private String getFormattedValue(Object value) {
        if (value instanceof java.sql.Date) {
            java.sql.Date sqlDate = (java.sql.Date) value;
            return dateFormat.format(new Date(sqlDate.getTime()));
        }

        if (value instanceof java.sql.Timestamp) {
            java.sql.Timestamp sqlDate = (Timestamp) value;
            return dateTimeFormat.format(new Date(sqlDate.getTime()));
        }

        if (value instanceof byte[]) {
            return StringEncoder.encodeToXml(new String((byte[]) value));
        }

        return StringEncoder.encodeToXml(value.toString());

    }

    /**
     * Add namespace to table.
     *
     * @param name
     *            - namespace token.
     * @param url
     *            - namespace url.
     */
    private void addNamespace(String name, String url) {
        namespaces.put(name, url);
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
     * Set the vocabulary in case it needs to be different from the properties file.
     *
     * @param url
     *            - namespace url.
     */
    private void setVocabulary(String url) {
        if (!url.equals(nullNamespace) && rdfHeaderWritten) {
            throw new RuntimeException("Can't set vocabulary after output has started!");
        }
        nullNamespace = url;
    }

    /**
     * Generate the RDF header element. You can in principle get the encoding
     * from the output stream, but it returns it as a string that is not
     * understandable by XML parsers.
     *
     * @throws IOException
     *             - if the output is not open.
     */
    private void rdfHeader() throws IOException {
        if (rdfHeaderWritten) {
            throw new RuntimeException("Can't write header twice!");
        }
        //output("<?xml version=\"1.0\" encoding=\"" + outputStream.getEncoding() + "\"?>\n");
        output("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        output("<rdf:RDF");
        for (Object key : namespaces.keySet()) {
            String url = namespaces.get(key).toString();
            output(" xmlns:");
            output(key.toString());
            output("=\"");
            output(url);
            output("\"\n");
        }
        output(" xmlns=\"");
        output(nullNamespace);
        output("\"");
        if (baseurl != null) {
            output(" xml:base=\"");
            output(baseurl);
            output("\"");
        }
        output(">\n\n");
        rdfHeaderWritten = true;
    }

    /**
     * Run a query. First value is the key. The others are the attributes. The column names are the attribute names. If first value
     * is null, then the attributes are assigned to the namespace of the table.
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

        Object currentId = "/..";
        Integer currentRow = 0;
        Boolean firstTime = true;
        try {
            stmt = con.createStatement();
            if (stmt.execute(sql)) {

                rs = stmt.getResultSet();

                ResultSetMetaData rsmd = rs.getMetaData();
                queryStruct(rsmd);

                int numcols = rsmd.getColumnCount();

                while (rs.next()) {

                    currentRow += 1;

                    Object id = rs.getObject(1);
                    if (id != null && id.equals("@")) {
                        id = currentRow;
                    }

                    if (currentId != null && !currentId.equals(id)) {
                        if (!firstTime) {
                            output("</");
                            output(rdfClass);
                            output(">\n");
                        }
                        output("<");
                        output(rdfClass);
                        output(" rdf:about=\"");
                        if (baseurl == null) {
                            output("#");
                        }
                        output(segment);
                        if (id != null) {
                            output("/");
                            output(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(id.toString())));
                        }
                        output("\">\n");
                        currentId = id;
                        firstTime = false;
                    }

                    for (int i = 2; i <= numcols; i++) {
                        writeProperty(names[i], rs.getObject(i));
                    }
                }
                if (!firstTime) {
                    output("</");
                    output(rdfClass);
                    output(">\n");
                }
            }
        } finally {
            close(rs);
            close(stmt);
        }
    }

    /**
     * Close resultset.
     * @param rs - result set
     */
    private static void close(ResultSet rs) {
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
    private static void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                // Deliberately ignore.
            }
        }
    }

    /**
     * Query attributes table. The result must have one + X * four columns. 1. id, 2. attribute name, 3. value, 4. datatype, 5.
     * languagecode, 6. attribute name, 7. value, 8. datatype, 9. languagecode, etc. If id is null, then the attributes are assigned
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
        Object currentId = "/..";
        Boolean firstTime = true;
        try {
            stmt = con.createStatement();

            if (stmt.execute(sql)) {
                rs = stmt.getResultSet();

                ResultSetMetaData rsmd = rs.getMetaData();
                int numcols = rsmd.getColumnCount();

                while (rs.next()) {
                    RDFField property = new RDFField();
                    if (rs.getObject(1) == null) {
                        continue;
                    }
                    Object id = rs.getObject(1);
                    if (currentId != null && !currentId.equals(id)) {
                        if (!firstTime) {
                            output("</");
                            output(rdfClass);
                            output(">\n");
                        }
                        output("<");
                        output(rdfClass);
                        output(" rdf:about=\"");
                        if (baseurl == null) {
                            output("#");
                        }
                        output(segment);
                        if (id != null) {
                            output("/");
                            output(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(id.toString())));
                        }
                        output("\">\n");
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
                        writeProperty(property, rs.getObject(b + 1));
                    }
                }
                if (!firstTime) {
                    output("</");
                    output(rdfClass);
                    output(">\n");
                }
            }
        } finally {
            GenerateRDF.close(rs);
            GenerateRDF.close(stmt);
        }
    }

    /**
     * Get the metadata from the columns. Check what datatype the database delivers. but override if the user has specified
     * something else in the column label.
     *
     * @param rsmd
     *            - metadata extracted from database.
     * @throws SQLException
     *             - if the SQL database is not available
     */
    private void queryStruct(ResultSetMetaData rsmd) throws SQLException {
        String dbDatatype;
        String rdfDatatype = "";
        int numcols = rsmd.getColumnCount();

        this.names = new RDFField[numcols + 1];

        for (int i = 1; i <= numcols; i++) {
            dbDatatype = rsmd.getColumnTypeName(i).toLowerCase();
            rdfDatatype = datatypeMap.get(dbDatatype);
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
     * Parses a column label. It can be parsed into three parts: name, datatype, language. hasRef-&gt; becomes "hasRef","-&gt;",""
     * hasRef-&gt;expert becomes "hasRef","-&gt;expert","" price^^xsd:decimal becomes "price","xsd:decimal","" rdfs:label@fr becomes
     * "rdfs:label","","fr"
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
}
// vim: set expandtab sw=4 :
