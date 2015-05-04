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

import java.io.OutputStream;
import java.sql.Connection;
import java.util.Properties;

/**
 *
 * @author George Sofianos
 */
public class RDFExportServiceJSONLD implements RDFExportService {
    
    /** Output to write JSONLD. */
    private OutputStream outputStream;
    /** Database connection. */
    private Connection connection;
    /** JSONLD properties. */
    private Properties properties;
    
    public RDFExportServiceJSONLD(OutputStream outputStream, Connection connection, Properties properties) {
        this.outputStream = outputStream;
        this.connection = connection;
        this.properties = properties;
    }

    @Override
    public void exportTable(String table, String identifier) throws Exception {
        GenerateJSONLD generateJSONLD = new GenerateJSONLD(outputStream, connection, properties);
        generateJSONLD.exportTable(table, identifier);
        generateJSONLD.exportDocumentInformation();
        generateJSONLD.writeJsonLDFooter();
    }

    @Override
    public void exportAllTables() throws Exception {
        GenerateJSONLD generateJSONLD = new GenerateJSONLD(outputStream, connection, properties);
        for (String table : generateJSONLD.getAllTables()) {
            generateJSONLD.exportTable(table);
        }
        generateJSONLD.exportDocumentInformation();
        generateJSONLD.writeJsonLDFooter();
    }
    
}
