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

import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Encapsulation of SQL data types.
 */
public final class Datatypes {

    /** Format of xsd:date value. */
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    /** Format of xsd:dateTime value. */
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";

    /** Date format. */
    private static SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    /** Date-time format. */
    private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATE_TIME_FORMAT);

    /** Known java types. */
    private static HashMap<String, Integer> knownTypes = new HashMap<String, Integer>();

    /** Java types to XSD mapping. */
    private static HashMap<Integer, String> defaultMapping = new HashMap<Integer, String>();

    static {
        knownTypes.put("bigint", Types.BIGINT);
        knownTypes.put("binary", Types.BINARY);
        knownTypes.put("bit", Types.BIT);
        knownTypes.put("blob", Types.BLOB);
        knownTypes.put("boolean", Types.BOOLEAN);
        knownTypes.put("char", Types.CHAR);
        knownTypes.put("clob", Types.CLOB);
        knownTypes.put("date", Types.DATE);
        knownTypes.put("decimal", Types.DECIMAL);
        knownTypes.put("double", Types.DOUBLE);
        knownTypes.put("float", Types.FLOAT);
        knownTypes.put("integer", Types.INTEGER);
        knownTypes.put("longnvarchar", Types.LONGNVARCHAR);
        knownTypes.put("longvarbinary", Types.LONGVARBINARY);
        knownTypes.put("longvarchar", Types.LONGVARCHAR);
        knownTypes.put("nchar", Types.NCHAR);
        knownTypes.put("nclob", Types.NCLOB);
        knownTypes.put("numeric", Types.NUMERIC);
        knownTypes.put("nvarchar", Types.NVARCHAR);
        knownTypes.put("real", Types.REAL);
        knownTypes.put("smallint", Types.SMALLINT);
        knownTypes.put("time", Types.TIME);
        knownTypes.put("timestamp", Types.TIMESTAMP);
        knownTypes.put("tinyint", Types.TINYINT);
        knownTypes.put("varbinary", Types.VARBINARY);
        knownTypes.put("varchar", Types.VARCHAR);

        defaultMapping.put(Types.BIGINT, "xsd:integer");
        defaultMapping.put(Types.BINARY, "");
        defaultMapping.put(Types.BIT, "xsd:int");
        defaultMapping.put(Types.BLOB, "");
        defaultMapping.put(Types.BOOLEAN, "xsd:boolean");
        defaultMapping.put(Types.CHAR, "");
        defaultMapping.put(Types.CLOB, "");
        defaultMapping.put(Types.DATE, "xsd:date");
        defaultMapping.put(Types.DECIMAL, "xsd:decimal");
        defaultMapping.put(Types.DOUBLE, "xsd:double");
        defaultMapping.put(Types.FLOAT, "xsd:float");
        defaultMapping.put(Types.INTEGER, "xsd:integer");
        defaultMapping.put(Types.LONGNVARCHAR, "");
        defaultMapping.put(Types.LONGVARBINARY, "");
        defaultMapping.put(Types.LONGVARCHAR, "");
        defaultMapping.put(Types.NCHAR, "");
        defaultMapping.put(Types.NCLOB, "");
        defaultMapping.put(Types.NUMERIC, "xsd:decimal");
        defaultMapping.put(Types.NVARCHAR, "");
        defaultMapping.put(Types.REAL, "xsd:decimal");
        defaultMapping.put(Types.SMALLINT, "xsd:int");
        defaultMapping.put(Types.TIME, "xsd:time");
        defaultMapping.put(Types.TIMESTAMP, "xsd:datetime");
        defaultMapping.put(Types.TINYINT, "xsd:int");
        defaultMapping.put(Types.VARBINARY, "");
        defaultMapping.put(Types.VARCHAR, "");
    }

    /**
     * Constructor. Since all methods are static we don't want instantiations of the class.
     */
    private Datatypes() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the SQL type from a string name.
     *
     * @param key - The type name
     * @return the SQL type as an Integer
     */
    public static Integer getSQLType(String key) {
        return knownTypes.get(key);
    }

    /**
     * Returns the RDF type from a SQL type.
     *
     * @param sqlType - The type name
     * @return the SQL type as a String
     */
    public static String getRDFType(Integer sqlType) {
        return defaultMapping.get(sqlType);
    }

    /**
     * Update the rdf type map.
     *
     * @param sqlType - The type name
     * @param rdfType - The updated RDF type
     */
    public static void setRDFType(Integer sqlType, String rdfType) {
        defaultMapping.put(sqlType, rdfType);
    }

    /**
     * Returns RDF formatted string representation of the value object. The value comes from a database.
     *
     * @param value - value to format
     * @throws SQLException
     *             - if the SQL database is not available
     *
     * @return the formatted string
     */
    public static String getFormattedValue(Object value) throws SQLException {
        if (value instanceof java.sql.Date) {
            Date sqlDate = (java.sql.Date) value;
            return dateFormat.format(new Date(sqlDate.getTime()));
        }

        if (value instanceof Timestamp) {
            Timestamp sqlDate = (Timestamp) value;
            return dateTimeFormat.format(new Date(sqlDate.getTime()));
        }

        if (value instanceof Clob) {
            Clob tValue = (Clob) value;
            return tValue.getSubString(1, (int) tValue.length());
        }

        if (value instanceof Blob) {
            // There is no guarantee that we'll get text data from a BLOB.
            Blob tValue = (Blob) value;
            return new String(tValue.getBytes(1, (int) tValue.length()), Charset.forName("UTF-8"));
        }

        if (value instanceof byte[]) {
            return new String((byte[]) value, Charset.forName("UTF-8"));
        }

        return value.toString();

    }
}
