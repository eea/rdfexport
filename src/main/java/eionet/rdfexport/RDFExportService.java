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
 *        Juhan Voolaid
 */
package eionet.rdfexport;

/**
 * Service interface of the main functionality.
 *
 * @author Juhan Voolaid
 */
public interface RDFExportService {

    /**
     * Export a table as RDF. A table can consist of several queries specified as property names table.query1, table.query2,
     * table.attributetable1 etc. The queries are sorted on name before being executed with the x.query first then x.attributetable
     * second.
     *
     * @param table
     *            - name of table in properties
     * @param identifier
     *            - primary key of the record we want or null for all records.
     * @throws Exception
     *            - on failure
     */
    void exportTable(String table, String identifier) throws Exception;

    /**
     * Exports all tables defined in the properties as RDF.
     *
     * @throws Exception
     *            - on failure
     */
    void exportAllTables() throws Exception;
}
