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
 *        Søren Roug
 */
package eionet.rdfexport;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Interface for writing a resource in RDF/XML or Turtle or N3.
 *
 * @author Søren Roug
 */
abstract class ResourceWriter {

    /** The namespaces to add to the rdf:RDF element. */
    protected HashMap<String, String> namespaces;

    /** The output stream to send output to. */
    private OutputStreamWriter outputStream;

    /** Base of XML file. */
    protected String baseurl;

    /** The URL of the null namespace. */
    protected String nullNamespace;

    /** Treat empty strings as NULL. */
    protected boolean emptyStringIsNull = false;

    /**
     * Constructor.
     *
     * @param stream - the stream to write the output to
     */
    public ResourceWriter(OutputStreamWriter stream) {
        outputStream = stream;
        namespaces = new HashMap<String, String>();
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }

    /**
     * Set the vocabulary in case it needs to be different from the properties file.
     *
     * @param url
     *            - namespace url.
     */
    public void setVocabulary(final String url) {
        nullNamespace = url;
    }

    /**
     * Set the base URL.
     *
     * @param url
     *            - the base url.
     */
    public void setBaseURL(final String url) {
        baseurl = url;
    }

    /**
     * Set a flag that tells how to deal with empty strings.
     *
     * @param emptyStringIsNull
     *            - flag. If true then don't output properties for empty strings.
     */
    public void setEmptyStringIsNull(final boolean emptyStringIsNull) {
        this.emptyStringIsNull = emptyStringIsNull;
    }

    /**
     * Add namespace to table.
     *
     * @param name
     *            - namespace token.
     * @param url
     *            - namespace url.
     */
    public void addNamespace(String name, String url) {
        namespaces.put(name, url);
    }


    /**
     * Generate the RDF header element. You can in principle get the encoding
     * from the output stream, but it returns it as a string that is not
     * understandable by XML parsers.
     *
     * @throws IOException
     *             - if the output is not open.
     */
    abstract void writeRdfHeader() throws IOException;

    /**
     * Generate the RDF footer element.
     *
     * @throws IOException
     *             - if the output is not open.
     */
    abstract void writeRdfFooter() throws IOException;

    /**
     * Write the start of a resource - the line with rdf:about.
     * @param rdfClass
     *            - the class to assign
     * @param segment
     *            - the namespace of the table
     * @param id
     *            - the unqualified identifier of the resource
     * @throws IOException
     *             - if the output is not open.
     */
    abstract void writeStartResource(String rdfClass, String segment, String id) throws IOException;

    /**
     * Write the end of a resource.
     * @param rdfClass
     *            - the class to assign
     * @throws IOException
     *             - if the output is not open.
     */
    abstract void writeEndResource(String rdfClass) throws IOException;

    /**
     * Write a property. If the property.datatype is "->" then it is a resource
     * reference.
     *
     * @param property
     *            triple consisting of name, datatype and langcode
     * @param value
     *            from database.
     * @throws SQLException
     *             - if the SQL database is not available
     * @throws IOException
     *             - if the output is not open.
     */
    abstract void writeProperty(RDFField property, Object value) throws SQLException, IOException;

    /**
     * Called from the other methods to do the output.
     *
     * @param v
     *            - value to print.
     * @throws IOException
     *             - if the output is not open.
     */
    protected void output(String v) throws IOException {
        outputStream.write(v);
    }

    /**
     * Called from the other methods to flush the output.
     *
     * @throws IOException
     *             - if the output is not open.
     */
    protected void flush() throws IOException {
        outputStream.flush();
    }

}
