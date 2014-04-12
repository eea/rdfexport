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
import java.sql.SQLException;

/**
 * Interface for writing a resource in RDF/XML or Turtle or N3.
 *
 * @author Søren Roug
 */
public interface ResourceWriter {

    /**
     * Set the vocabulary in case it needs to be different from the properties file.
     *
     * @param url
     *            - namespace url.
     */
    void setVocabulary(final String url);

    /**
     * Set the base URL.
     *
     * @param url
     *            - the base url.
     */
    void setBaseURL(final String url);

    /**
     * Set a flag that tells how to deal with empty strings.
     *
     * @param emptyStringIsNull
     *            - flag. If true then don't output properties for empty strings.
     */
    void setEmptyStringIsNull(boolean emptyStringIsNull);

    /**
     * Add namespace to table.
     *
     * @param name
     *            - namespace token.
     * @param url
     *            - namespace url.
     */
    void addNamespace(String name, String url);

    /**
     * Generate the RDF header element. You can in principle get the encoding
     * from the output stream, but it returns it as a string that is not
     * understandable by XML parsers.
     *
     * @throws IOException
     *             - if the output is not open.
     */
    void writeRdfHeader() throws IOException;

    /**
     * Generate the RDF footer element.
     *
     * @throws IOException
     *             - if the output is not open.
     */
    void writeRdfFooter() throws IOException;

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
    void writeStartResource(String rdfClass, String segment, String id) throws IOException;

    /**
     * Write the end of a resource.
     * @param rdfClass
     *            - the class to assign
     * @throws IOException
     *             - if the output is not open.
     */
    void writeEndResource(String rdfClass) throws IOException;

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
    void writeProperty(RDFField property, Object value) throws SQLException, IOException;

}
