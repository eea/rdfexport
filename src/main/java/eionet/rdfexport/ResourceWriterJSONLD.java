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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 *
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
    
    private final String JSONLD_CONTEXT = "@context";
    private final String JSONLD_BASE = "@base";
    private final String JSONLD_ID = "@id";
    private final String JSONLD_LANGUAGE = "@language";
    private final String JSONLD_TYPE = "@type";
    private final String JSONLD_VALUE = "@value";
    private final String JSONLD_VOCAB = "@vocab";
    
    private JsonFactory f;
    private JsonGenerator json;
    
    public ResourceWriterJSONLD(OutputStreamWriter stream) throws IOException {
        outputStream = stream;
        namespaces = new HashMap<String, String>();
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        f = new JsonFactory();        
        json = f.createGenerator(outputStream);        
        //TODO find or create a better pretty printer
        json.useDefaultPrettyPrinter();
    }
    
    public void setVocabulary(final String url) {
        if (!url.equals(nullNamespace) && rdfHeaderWritten) {
            throw new RuntimeException("Can't set vocabulary after output has started!");
        }
        nullNamespace = url;
    }
        
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
    
    public void setBaseURL(String url) {
        baseurl = url;
    }
    
    public void setEmptyStringIsNull(boolean emptyStringIsNull) {
        this.emptyStringIsNull = emptyStringIsNull;
    }
    
    public void addNamespace(String name, String url) {
        namespaces.put(name, url);
    }
    
    public void writeJsonLDFooter() throws IOException {
        json.writeEndObject();
        json.close();        
    }
    
    void writeArray(RDFField property) throws IOException {
        json.writeArrayFieldStart(property.name);
    }
    
    public void writeEndArray() throws IOException {
        json.writeEndArray();
    }        
    
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
            }
            else {
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
        }
        else if (!"".equals(property.langcode)) {
            json.writeObjectFieldStart(property.name);
            json.writeStringField(JSONLD_LANGUAGE, property.langcode);
            json.writeStringField(JSONLD_VALUE, StringEncoder.encodeToXml(Datatypes.getFormattedValue(value)));
            json.writeEndObject();
        }     
        else {            
            json.writeStringField(property.name, StringEncoder.encodeToXml(Datatypes.getFormattedValue(value)));                
        }
    }
    
    public void writeEndResource() throws IOException {
        json.writeEndObject();
    }
    
    private void output(String s) throws IOException {
        outputStream.write(s);
    }
    
    private void flush() throws IOException {
        outputStream.flush();
    }
}
