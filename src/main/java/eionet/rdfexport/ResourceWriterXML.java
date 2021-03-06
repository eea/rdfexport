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
 * Class for writing a resource in RDF/XML. This class does not read properties files.
 *
 * @author Søren Roug
 */
public class ResourceWriterXML extends ResourceWriter {

    /** If output has started, then you can't change the nullNamespace. */
    private Boolean rdfHeaderWritten = false;

    /**
     * Constructor.
     *
     * @param stream - the stream to write the output to
     */
    public ResourceWriterXML(OutputStreamWriter stream) {
        super(stream);
    }

    @Override
    public void setVocabulary(final String url) {
        if (!url.equals(nullNamespace) && rdfHeaderWritten) {
            throw new RuntimeException("Can't set vocabulary after output has started!");
        }
        nullNamespace = url;
    }

    @Override
    public void writeRdfHeader() throws IOException {
        if (rdfHeaderWritten) {
            return;
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
        if (nullNamespace != null) {
            output(" xmlns=\"");
            output(nullNamespace);
            output("\"");
        }
        if (baseurl != null) {
            output(" xml:base=\"");
            output(baseurl);
            output("\"");
        }
        output(">\n\n");
        rdfHeaderWritten = true;
    }

    @Override
    public void writeRdfFooter() throws IOException {
        writeRdfHeader();
        output("</rdf:RDF>\n");
        flush();
    }

    @Override
    public void writeStartResource(String rdfClass, String segment, String id) throws IOException {
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
    }

    @Override
    public void writeEndResource(String rdfClass) throws IOException {
        output("</");
        output(rdfClass);
        output(">\n");
    }

    @Override
    public void writeProperty(RDFField property, Object value) throws SQLException, IOException {
        String typelangAttr = "";
        if (value == null || (emptyStringIsNull && "".equals(value))) {
            return;
        }
        output(" <");
        output(property.name);
        if (property.datatype.startsWith("->")) {
            // Handle pointers
            if (property.datatype.length() == 2) {
                // Handle the case where the value contains the pointer.
                output(" rdf:resource=\"");
                output(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(Datatypes.getFormattedValue(value))));
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
                output(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(Datatypes.getFormattedValue(value))));
                output("\"/>\n");
            }
            return;
        } else if (!"".equals(property.datatype)) {
            if (property.datatype.startsWith("xsd:")) {
                property.datatype = "http://www.w3.org/2001/XMLSchema#" + property.datatype.substring(4);
            }
            typelangAttr = " rdf:datatype=\"" + property.datatype + "\"";
        } else if (!"".equals(property.langcode)) {
            typelangAttr = " xml:lang=\"" + property.langcode + "\"";
        }
        output(typelangAttr);
        output(">");
        output(StringEncoder.encodeToXml(Datatypes.getFormattedValue(value)));
        output("</");
        output(property.name);
        output(">\n");
    }
}
