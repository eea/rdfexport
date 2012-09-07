package eionet.rdfexport;

import java.io.OutputStream;
import java.sql.Connection;
import java.util.Properties;

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
 * The Original Code is Content Registry 3
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Juhan Voolaid
 */

/**
 * Service implementation of the main functionality.
 *
 * @author Juhan Voolaid
 */
public class RDFExportServiceImpl implements RDFExportService {

    /** Output to write RDF. */
    private OutputStream outputStream;
    /** Database connection. */
    private Connection connection;
    /** RDF properties. */
    private Properties properties;

    /**
     * Class constructor.
     *
     * @param outputStream
     * @param connection
     * @param properties
     */
    public RDFExportServiceImpl(OutputStream outputStream, Connection connection, Properties properties) {
        this.outputStream = outputStream;
        this.connection = connection;
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportTable(String table, String identifier) throws Exception {
        GenerateRDF generateRDF = new GenerateRDF(outputStream, connection, properties);
        generateRDF.exportTable(table, identifier);
        generateRDF.writeRdfFooter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportAllTables() throws Exception {
        GenerateRDF generateRDF = new GenerateRDF(outputStream, connection, properties);
        for (String table : generateRDF.getAllTables()) {
            generateRDF.exportTable(table);
        }
        generateRDF.exportDocumentInformation();
        generateRDF.writeRdfFooter();
    }
}
