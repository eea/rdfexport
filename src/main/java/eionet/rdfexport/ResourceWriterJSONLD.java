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
 *        George Sofianos
 */

package eionet.rdfexport;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Class for writing a resource in JSON-LD. This class does not read properties files.
 * @author George Sofianos
 */
public class ResourceWriterJSONLD {

    /** The namespaces to add to the @context element. */
    protected HashMap<String, String> namespaces;

    /** The output stream to send output to. */
    private OutputStreamWriter outputStream;

    /** Base of file. */
    protected String baseurl;

    /** The URL of the null namespace. */
    protected String nullNamespace;

    /** Treat empty strings as NULL. */
    protected boolean emptyStringIsNull = false;

    /** If output has started, then you can't change the nullNamespace. */
    private Boolean rdfHeaderWritten = false;

    private static final String JSONLD_CONTEXT = "@context";
    private static final String JSONLD_BASE = "@base";
    private static final String JSONLD_ID = "@id";
    private static final String JSONLD_LANGUAGE = "@language";
    private static final String JSONLD_TYPE = "@type";
    private static final String JSONLD_VALUE = "@value";
    private static final String JSONLD_VOCAB = "@vocab";

    private JsonFactory f;
    private JsonGenerator json;

    /**
     * Constructor
     * @param stream - the stream to write the output to
     * @throws IOException - if the output is not open.
     */
    public ResourceWriterJSONLD(OutputStreamWriter stream) throws IOException {
        outputStream = stream;
        namespaces = new HashMap<String, String>();
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        f = new JsonFactory();
        json = f.createGenerator(outputStream);
        //TODO find or create a better pretty printer
        json.useDefaultPrettyPrinter();
    }

    /**
     * Set the JSON-LD vocabulary in case it needs to be different from the properties file.
     * @param url - namespace url.
     */
    public void setVocabulary(final String url) {
        if (!url.equals(nullNamespace) && rdfHeaderWritten) {
            throw new RuntimeException("Can't set vocabulary after output has started!");
        }
        nullNamespace = url;
    }

    /**
     * Generate the JSON-LD header element.
     * @throws IOException - if the output is not open.
     */
    void writeJsonLDHeader() throws IOException {
        if (rdfHeaderWritten) {
            return;
        }
        //start jsonld object
        json.writeStartObject();
        //write context field
        json.writeObjectFieldStart(JSONLD_CONTEXT);

        for (Object key : namespaces.keySet()) {
            String url = namespaces.get(key.toString());
            json.writeStringField(key.toString(), url);
        }
        if (baseurl != null) {
            json.writeStringField(JSONLD_BASE, baseurl);
        }
        if (nullNamespace != null) {
            json.writeStringField(JSONLD_VOCAB, nullNamespace);
        }
        json.writeEndObject();
    }

    /**
     * Set the base URL.
     * @param url - the base URL
     */
    public void setBaseURL(String url) {
        baseurl = url;
    }

    /**
     * Set a flag that tells how to deal with empty strings.
     * @param emptyStringIsNull - flag. If true then don't output properties for empty strings.
     */
    public void setEmptyStringIsNull(boolean emptyStringIsNull) {
        this.emptyStringIsNull = emptyStringIsNull;
    }

    /**
     * Add namespace to table.
     * @param name - namespace token
     * @param url - namespace url
     */
    public void addNamespace(String name, String url) {
        namespaces.put(name, url);
    }

    /**
     * Generate the JSON-LD footer element.
     * @throws IOException - if the output is not open.
     */
    public void writeJsonLDFooter() throws IOException {
        json.writeEndObject();
        json.close();
    }

    /**
     * Generates a JSON array
     * @param property - triple consisting of name, datatype and langcode
     * @throws IOException - if the output is not open.
     */
    void writeArray(RDFField property) throws IOException {
        json.writeArrayFieldStart(property.name);
    }

    /**
     * Ends a JSON array
     * @throws IOException - if the output is not open.
     */
    public void writeEndArray() throws IOException {
        json.writeEndArray();
    }

    /**
     * Write the start of a resource - the lines with @id and @type
     * @param rdfClass - the class to assign
     * @param segment - the namespace of the table
     * @param id - the unqualified identifier of the resource
     * @throws IOException - if the output is not open.
     */
    public void writeStartResource(String rdfClass, String segment, String id) throws IOException {
        if (rdfHeaderWritten) {
            return;
        }
        String value = "";
        if (baseurl == null) {
            value += "#";
        }
        if (segment != null) {
            value += segment;
        }
        if (id != null) {
            value += "/".concat(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(id)));
        }
        json.writeStringField(JSONLD_ID, value);
        json.writeStringField(JSONLD_TYPE, rdfClass);
        rdfHeaderWritten = true;
    }

    /**
     *
     * @param property - triple consisting of name, datatype and langcode
     * @param value - from database.
     * @param isArray - if it has a parent array
     * @throws SQLException - if the SQL database is not available
     * @throws IOException - if the output is not open.
     */
    void writeProperty(RDFField property, Object value, Boolean isArray) throws SQLException, IOException {
        if (value == null || (emptyStringIsNull && "".equals(value))) {
            return;
        }
        //json.writeStartObject();
        if (property.datatype.startsWith("->")) {
            if (property.datatype.length() == 2) {
                if (!isArray) json.writeObjectFieldStart(property.name);
                else json.writeStartObject();
                json.writeStringField(JSONLD_ID, StringEncoder.encodeToXml(StringEncoder.encodeToIRI(Datatypes.getFormattedValue(value))));
                json.writeEndObject();
            } else {
                // Handle the case of ->countries or ->http://...
                // If the ref-segment contains a colon then it can't be a fragment
                // http://www.w3.org/TR/REC-xml-names/#NT-NCName
                String refSegment = property.datatype.substring(2);
                if (!isArray) json.writeObjectFieldStart(property.name);
                else json.writeStartObject();
                String tmp = "";
                if (baseurl == null && refSegment.indexOf(":") == -1) {
                    tmp = "#";
                }
                json.writeStringField(JSONLD_ID, tmp + StringEncoder.encodeToIRI(refSegment) + "/" + StringEncoder.encodeToXml(StringEncoder.encodeToIRI(Datatypes.getFormattedValue(value))));
                json.writeEndObject();
            }
        } else if (!"".equals(property.datatype)) {
            if (!isArray) json.writeObjectFieldStart(property.name);
            else json.writeStartObject();
            json.writeStringField(JSONLD_TYPE, property.datatype);
            json.writeStringField(JSONLD_VALUE, StringEncoder.encodeToXml(Datatypes.getFormattedValue(value)));
            json.writeEndObject();
        } else if (!"".equals(property.langcode)) {
            json.writeObjectFieldStart(property.name);
            json.writeStringField(JSONLD_LANGUAGE, property.langcode);
            json.writeStringField(JSONLD_VALUE, StringEncoder.encodeToXml(Datatypes.getFormattedValue(value)));
            json.writeEndObject();
        } else {
            json.writeStringField(property.name, StringEncoder.encodeToXml(Datatypes.getFormattedValue(value)));
        }
    }

    /**
     * Writes the end of a resource.
     * @throws IOException - if the output is not open.
     */
    public void writeEndResource() throws IOException {
        json.writeEndObject();
    }

    /**
     * Writes a JSON object start
     * @throws IOException - if the output is not open.
     */
    public void writeStartObject() throws IOException {
        json.writeStartObject();
    }
}
