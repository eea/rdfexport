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
 *
 */
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;

/**
 * A struct to hold a complex type.
 * No need to do data encapsulation.
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
}

/**
 * Class to help escape strings for XML and URI components.
 * @see http://www.java2s.com/Tutorial/Java/0120__Development/EscapeHTML.htm
 * @see http://www.ietf.org/rfc/rfc3986.txt
 */
final class StringHelper {
    /**
     * Constructor. Since all methods are static we don't want instantiations of the class.
     */
    private StringHelper() {
        throw new UnsupportedOperationException();
    }
    /**
     * Escape characters that have special meaning in XML.
     *
     * @param s - The string to escape.
     * @return escaped string.
     */
    public static String escapeXml(String s) {
        int length = s.length();
        int newLength = length;
        // first check for characters that might
        // be dangerous and calculate a length
        // of the string that has escapes.
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"':
                    newLength += 5;
                    break;
                case '&':
                case '\'':
                    newLength += 4;
                    break;
                case '<':
                case '>':
                    newLength += 3;
                    break;
                default:
                    break;
            }
        }
        if (length == newLength) {
            // nothing to escape in the string
            return s;
        }
        StringBuffer sb = new StringBuffer(newLength);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * %-escapes the given string for a legal URI component.
     * See http://www.ietf.org/rfc/rfc3986.txt section 2.4 for more.
     *
     * Does java.net.URLEncoder.encode(String, String) and then on the resulting string does the following corrections:
     *   - the "+" signs are converted into "%20".
     *   - "%21", "%27", "%28", "%29" and "%7E" are unescaped back (i.e. "!", "'", "(", ")" and "~").
     * See the JavaDoc of java.net.URLEncoder and the above RFC specification for why this is done.
     *
     * @param s   The string to %-escape.
     * @param enc The encoding scheme to use.
     * @return    The escaped string.
     */
    public static String encodeURIComponent(String s, String enc){
        try {
            return URLEncoder.encode(s, enc)
            .replaceAll("\\+", "%20")
            .replaceAll("\\%21", "!")
            .replaceAll("\\%27", "'")
            .replaceAll("\\%28", "(")
            .replaceAll("\\%29", ")")
            .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            // This exception should never occur.
            return s;
        }
    }
}

/**
 * RDF generator. The queries are stored in a properties file. There
 * are two types of queries. A plain select and an attributes table.
 * For the plain select the class will use the first column as the
 * <em>identifier</em>, and create RDF properties for the other columns.
 *
 * For the attributes table the result must have one + X * four columns:
 *  1. id, 2. attribute name, 3. value, 4. datatype, 5. languagecode,
 *         6. attribute name, 7. value, 8. datatype, 9. languagecode, etc.
 */
public class GenerateRDF {
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
    private String[] tables;
    /** Hashtable of loaded properties. */
    private Properties props;
    /** The output stream to send output to. */
    private PrintStream outputStream;

    /**
     * Constructor.
     * @param writer - The output stream to send output to
     * @param dbCon - The database connection
     * @param propFilename - The file to load the configuration from
     * @throws IOException - if the properties file is missing
     * @throws SQLException - if the SQL database is not available
     * @throws ClassNotFoundException - if the SQL driver is unavailable
     * @throws InstantiationException - if the SQL driver can't be instantiatied
     * @throws IllegalAccessException - unknown
     */
    public GenerateRDF(PrintStream writer, Connection dbCon, String propFilename) throws IOException,
                                SQLException, ClassNotFoundException,
                                InstantiationException, IllegalAccessException {
        props = new Properties();

        outputStream = writer;
        props.load(new FileInputStream(propFilename));

        tables = props.getProperty("tables").split("\\s+");

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
        rdfFooter();
        if (con != null) {
            con.close();
            con = null;
        }
    }

    /**
     * Called from the other methods to do the output.
     *
     * @param v - value to print.
     */
    private void output(String v) {
        outputStream.print(v);
    }

    /**
     * Write a property. If the property.datatype is "->" then it is a resource reference.
     *
     * @param property triple consisting of name, datatype and langcode
     * @param value from database.
     */
    private void writeProperty(RDFField property, Object value) {
        String typelangAttr = "";

        if (value == null) {
            return;
        }
        output(" <");
        output(property.name);
        if (property.datatype.startsWith("->")) {
            // Handle pointers
            if (property.datatype.length() == 2) {
                output(" rdf:resource=\"");
                output(StringHelper.escapeXml(value.toString()));
                output("\"/>\n");
            } else {
                output(" rdf:resource=\"");
                output(property.datatype.substring(2));
                output("/");
                output(StringHelper.escapeXml(value.toString()));
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
        output(StringHelper.escapeXml(value.toString()));
        output("</");
        output(property.name);
        output(">\n");
    }

    /**
     * The user can choose one record to output. This is done by inserting
     *  a HAVING ID=... into the SELECT statement. (using HAVING is slow).
     *  If the ID is numeric, then Mysql will convert the type to match
     *
     *  @param query - SQL query to patch
     *  @param identifier to insert into query
     *  @return patched SQL query
     */
    private String injectHaving(String query, String identifier) {
        String[] keywords = {" order ", " limit ", " procedure ", " into ", " for ", " lock " };
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
            query = query.substring(0, insertBefore) + " HAVING id='" + identifier.replace("'", "''") + "'"
                    + query.substring(insertBefore);
        } else {
            query = query.substring(0, h + 8) + "id='" + identifier.replace("'", "''") + "' AND "
                    + query.substring(h + 8);
        }
        return query;
    }

    /**
     * The user can choose one record to output. This is done by inserting
     *  a WHERE <em>key</em>=... into the SELECT statement.
     *  If the ID is numeric, then Mysql will convert the type to match
     *
     *  @param query - SQL query to patch
     *  @param key - Name of column that can be used as key in index
     *  @param identifier to insert into query
     *  @return patched SQL query
     */
    private String injectWhere(String query, String key, String identifier) {
        // Handle WHERE for key hints
        String[] keywords = {" group ", " having ", " order ", " limit ", " procedure ", " into ", " for ", " lock " };
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
            query = query.substring(0, insertBefore) + " WHERE " + key + "='" + identifier.replace("'", "''") + "'"
                    + query.substring(insertBefore);
        } else {
            query = query.substring(0, h + 7) + key + "='" + identifier.replace("'", "''") + "' AND "
                    + query.substring(h + 7);
        }
        return query;
    }

    /**
     * Return all known tables in properties file.
     * @return list of strings.
     */
    public String[] getAllTables() {
        return Arrays.copyOf(tables, tables.length);
    }

    /**
     * Export a table as RDF. A table can consist of several queries.
     *
     * @param table - name of table in properties file
     * @throws SQLException if there is a database problem.
     */
    public void exportTable(String table) throws SQLException {
        exportTable(table, null);
    }

    /**
     * Export a table as RDF. A table can consist of several queries specified
     * as property names table.query1, table.query2, table.attributetable1 etc.
     * The queries are sorted on name before being executed with the x.query
     * first then x.attributetable second.
     *
     * @param table - name of table in properties file
     * @param identifier - primary key of the record we want or null for all records.
     * @throws SQLException if there is a database problem.
     */
    public void exportTable(String table, String identifier) throws SQLException {
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
     * Add namespace to table.
     *
     * @param name - namespace token.
     * @param url - namespace url.
     */
    private void addNamespace(String name, String url) {
        namespaces.put(name, url);
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
     * Set the vocabulary in case it needs to be different from the properties file.
     *
     * @param url - namespace url.
     */
    private void setVocabulary(String url) {
        if (!url.equals(nullNamespace) && rdfHeaderWritten) {
            throw new RuntimeException("Can't set vocabulary after output has started!");
        }
        nullNamespace = url;
    }

    /**
     * Generate the RDF header element.
     */
    private void rdfHeader() {
        if (rdfHeaderWritten) {
            throw new RuntimeException("Can't write header twice!");
        }
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
        output(">\n");
        rdfHeaderWritten = true;
    }

    /**
     * Generate the RDF footer element.
     */
    private void rdfFooter() {
        output("</rdf:RDF>\n");
    }

    /**
     * Run a query.
     *
     * @param segment - the namespace of the table
     * @param sql - the query to run.
     * @param rdfClass - the class to assign or rdf:Description
     * @throws SQLException - if the SQL database is not available
     */
    private void runQuery(String segment, String sql, String rdfClass) throws SQLException {
        Statement stmt = null;
        String currentId = null;
        Boolean firstTime = true;
        try {
            stmt = con.createStatement();

            if (stmt.execute(sql)) {
                // There's a ResultSet to be had
                ResultSet rs = stmt.getResultSet();

                ResultSetMetaData rsmd = rs.getMetaData();
                queryStruct(rsmd);

                int numcols = rsmd.getColumnCount();

                while (rs.next()) {
                    String id = rs.getObject(1).toString();
                    if (!id.equals(currentId)) {
                        if (!firstTime) {
                            output("</");
                            output(rdfClass);
                            output(">\n");
                        }
                        output("<");
                        output(rdfClass);
                        output(" rdf:about=\"");
                        output(segment);
                        output("/");
                        output(StringHelper.escapeXml(StringHelper.encodeURIComponent(id, "UTF-8")));
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
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Query attributes table. The result must have one + X * four columns.
     *  1. id, 2. attribute name, 3. value, 4. datatype, 5. languagecode,
     *         6. attribute name, 7. value, 8. datatype, 9. languagecode, etc.
     *
     * @param segment - the namespace of the table
     * @param sql - the query
     * @param rdfClass - the class to assign or rdf:Description
     * @throws SQLException - if the SQL database is not available
     */
    private void runAttributes(String segment, String sql, String rdfClass) throws SQLException {
        Statement stmt = null;
        String currentId = null;
        Boolean firstTime = true;
        try {
            stmt = con.createStatement();

            if (stmt.execute(sql)) {
                ResultSet rs = stmt.getResultSet();

                ResultSetMetaData rsmd = rs.getMetaData();
                int numcols = rsmd.getColumnCount();

                while (rs.next()) {
                    RDFField property = new RDFField();
                    if (rs.getObject(1) == null) {
                        continue;
                    }
                    String id = rs.getObject(1).toString();
                    if (!id.equals(currentId)) {
                        if (!firstTime) {
                            output("</");
                            output(rdfClass);
                            output(">\n");
                        }
                        output("<");
                        output(rdfClass);
                        output(" rdf:about=\"");
                        output(segment);
                        output("/");
                        output(StringHelper.escapeXml(StringHelper.encodeURIComponent(id, "UTF-8")));
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
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    /**
     * Get the metadata from the columns. Check what datatype the database delivers.
     * but override if the user has specified something else in the column label.
     *
     * @param rsmd - metadata extracted from database.
     * @throws SQLException - if the SQL database is not available
     */
    private void queryStruct(ResultSetMetaData rsmd) throws SQLException {
        String dbDatatype;
        String rdfDatatype = "";
        int numcols = rsmd.getColumnCount();

        this.names = new RDFField[numcols + 1];

        for (int i = 1; i <= numcols; i++) {
            dbDatatype = rsmd.getColumnTypeName(i).toLowerCase();
            rdfDatatype = datatypeMap.get((Object) dbDatatype);
            if (rdfDatatype == null) {
                rdfDatatype = "";
            }
            if (objectProperties.containsKey(rsmd.getColumnLabel(i))) {
                rdfDatatype = objectProperties.get(rsmd.getColumnLabel(i)).toString();
            }
            names[i] = parseName(rsmd.getColumnLabel(i), rdfDatatype);
        }
    }

    /**
     * Parses a column label. It can be parsed into three parts: name, datatype, language.
     *      hasRef-&gt; becomes "hasRef","-&gt;",""
     *      hasRef-&gt;expert becomes "hasRef","-&gt;expert",""
     *      price^^xsd:decimal becomes "price","xsd:decimal",""
     *      rdfs:label@fr becomes "rdfs:label","","fr"
     *
     * @param complexname - name containing column name plus datatype or language code.
     * @param datatype - suggested datatype from database.
     * @return RDFField - struct of three strings: Name, datatype and langcode.
     */
    private RDFField parseName(String complexname, String datatype) {
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
     * Main routine. Primarily to demonstrate the use.
     */
    public static void main(String[] args) {
        ArrayList<String> unusedArgs;
        String[] tables;
        String identifier = null;
        String rdfPropFilename = "rdfexport.properties";
        String dbPropFilename = "database.properties";

        unusedArgs = new ArrayList<String>(args.length);

        // Parse arguments. Just find an -i option
        // The -i takes an argument that is the record id we're interested in
        // variable "i" is in fact used.
        for (int a = 0; a < args.length; a++) {
            if (args[a].equals("-d")) {
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
            props.load(new FileInputStream(dbPropFilename));

            String driver = props.getProperty("driver");
            String dbUrl = props.getProperty("database");
            String userName = props.getProperty("user");
            String password = props.getProperty("password");

            Class.forName(driver).newInstance();
            Connection con = DriverManager.getConnection(dbUrl, userName, password);
            GenerateRDF r = new GenerateRDF(System.out, con, rdfPropFilename);

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
            r.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
//vim: set expandtab sw=4 :
