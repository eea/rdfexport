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
 * Class for writing a resource in Turtle. Not finished!
 *
 * @author Søren Roug
 */
public class ResourceWriterTTL extends ResourceWriter {

    /** If output has started, then you can't change the nullNamespace. */
    private Boolean rdfHeaderWritten = false;

    /**
     * Constructor.
     *
     * @param stream - the stream to write the output to
     */
    public ResourceWriterTTL(OutputStreamWriter stream) {
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
        for (Object key : namespaces.keySet()) {
            String url = namespaces.get(key).toString();
            output("@prefix ");
            output(key.toString());
            output(": <");
            output(url);
            output(">;\n");
        }
        output("@prefix : <");
        output(nullNamespace);
        output(">\n");
        if (baseurl != null) {
            output("@base: <");
            output(baseurl);
            output(">\n");
        }
        rdfHeaderWritten = true;
    }

    @Override
    public void writeRdfFooter() throws IOException {
        writeRdfHeader();
        output("\n");
        flush();
    }

    @Override
    public void writeStartResource(String rdfClass, String segment, String id) throws IOException {
        output("<");
        if (baseurl == null) {
            output("#");
        }
        output(segment);
        if (id != null) {
            output("/");
            output(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(id.toString())));
        }
        output("> a ");
        if (rdfClass.startsWith("http")) {
            output("<");
            output(rdfClass);
            output(">");
        } else {
            if (!rdfClass.contains(":")) {
                output(":");
            }
            output(rdfClass);
        }
    }

    @Override
    public void writeEndResource(String rdfClass) throws IOException {
        output(".\n");
    }

    @Override
    public void writeProperty(RDFField property, Object value) throws SQLException, IOException {
        String typelangAttr = "";
        if (value == null || (emptyStringIsNull && "".equals(value))) {
            return;
        }
        output(";\n    ");
        if (!property.name.contains(":")) {
            output(":");
        }
        output(property.name);
        output(" ");
        if (property.datatype.startsWith("->")) {
            // Handle pointers
            if (property.datatype.length() == 2) {
                // Handle the case where the value contains the pointer.
                output("<");
                output(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(Datatypes.getFormattedValue(value))));
                output(">");
            } else {
                // Handle the case of ->countries or ->http://...
                // If the ref-segment contains a colon then it can't be a fragment
                // http://www.w3.org/TR/REC-xml-names/#NT-NCName
                String refSegment = property.datatype.substring(2);
                output("<");
                if (baseurl == null && refSegment.indexOf(":") == -1) {
                    output("#");
                }
                output(StringEncoder.encodeToIRI(refSegment));
                output("/");
                output(StringEncoder.encodeToXml(StringEncoder.encodeToIRI(Datatypes.getFormattedValue(value))));
                output(">");
            }
            return;
        } else if (!"".equals(property.datatype)) {
            if (property.datatype.startsWith("xsd:")) {
                typelangAttr = "^^" + property.datatype;
            } else {
                typelangAttr = "^^<" + property.datatype + ">";
            }
            typelangAttr = "^^" + property.datatype;
        } else if (!"".equals(property.langcode)) {
            typelangAttr = "@" + property.langcode;
        }
        output("\"");
        output(Datatypes.getFormattedValue(value));
        output("\"");
        output(typelangAttr);
    }

}
