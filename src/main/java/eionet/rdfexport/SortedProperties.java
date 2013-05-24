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

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * Properties that has keys sorted. It is used for storing properties to file in sorted order.
 *
 * @author Juhan Voolaid
 */
public class SortedProperties extends Properties {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Comparator for natural order.
     */
    private static final Comparator<Object> comparator = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return ((String) o1).compareTo((String) o2);
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Enumeration<Object> keys() {
        Vector<Object> keyList = new Vector<Object>(super.keySet());
        Collections.sort(keyList, comparator);
        return keyList.elements();
    }
}
