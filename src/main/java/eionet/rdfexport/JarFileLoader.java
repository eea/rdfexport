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
 * The Original Code is RDFExport 1.3
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency.
 *
 * Contributor(s):
 *  Søren Roug, EEA
 *
 */
package eionet.rdfexport;

import java.net.URI;
import java.net.URL;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

public final class JarFileLoader {

    /**
     * Constructor. This is a utility class.
     */
    private JarFileLoader() {
    }

    /**
     * Add on JAR file to the Class loader.
     *
     * @param s - the file name of the jar file.
     */
    public static void addPath(String s) throws Exception {
        File f = new File(s);
        URI u = f.toURI();
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> urlClass = URLClassLoader.class;
        Method method = urlClass.getDeclaredMethod("addURL", new Class[]{URL.class});
        method.setAccessible(true);
        method.invoke(urlClassLoader, new Object[]{u.toURL()});
    }

    /**
     * Parse a class path string with colon or semicolon delimiters.
     *
     * @param pathLine - The unsplit references to jar files.
     */
    public static void addPaths(String pathLine) {
        if (pathLine == null || "".equals(pathLine)) {
            return;
        }
        try {
            String[] paths = pathLine.split("[:;]");
            for (String path : paths) {
                if (!"".equals(path)) {
                    addPath(path);
                }
            }
        } catch (MalformedURLException e) {
            System.out.println("File not found");
        } catch (Exception e) {
            System.out.println("File not found");
        }
    }
}
