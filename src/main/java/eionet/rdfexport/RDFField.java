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

/**
 * A struct to hold a complex type. No need to do data encapsulation.
 */
class RDFField {
    /** Name of column. */
    String name;
    /** Datatype of column. */
    String datatype;
    /** Language code of column. */
    String langcode;

    /**
     * Constructor.
     */
    RDFField() {
        name = "";
        datatype = "";
        langcode = "";
    }

    /**
     * Constructor.
     * @param n - name
     * @param dt - datatype
     * @param l - language code
     */
    RDFField(String n, String dt, String l) {
        name = n;
        datatype = dt;
        langcode = l;
    }
}
