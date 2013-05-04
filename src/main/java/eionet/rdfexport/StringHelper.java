package eionet.rdfexport;

//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;

/**
 * Class to help escape strings for XML and URI components.
 *
 * @see http://www.java2s.com/Tutorial/Java/0120__Development/EscapeHTML.htm
 * @see http://www.ietf.org/rfc/rfc3986.txt
 */
final class StringHelper {
    /**
     * Characters that aren't allowed in IRIs. Special consideration for plus (+): It is historically used to encode space. If we
     * leave it unencoded, then it could be mistakenly decoded back to a space.
     */
    private static final char[] BAD_IRI_CHARS = {' ', '{', '}', '<', '>', '"', '|', '\\', '^', '`', '+'};
    /** Replacements for characters that aren't allowed in IRIs. */
    private static final String[] BAD_IRI_CHARS_ESCAPES = {"%20", "%7B", "%7D", "%3C", "%3E",
                                                        "%22", "%7C", "%5C", "%5E", "%60", "%2B"};

    /** * Characters that aren't allowed in XML.  */
    private static final char[] BAD_XML_CHARS = {'\'', '"', '&', '<', '>'};
    /** Replacements for characters that aren't allowed in XML. */
    private static final String[] BAD_XML_CHARS_ESCAPES = {"&#39;", "&quot;", "&amp;", "&lt;", "&gt;"};

    private static final char[] BAD_CMP_CHARS = {';', '/', '?', ':', '@', '&', '=',
        '+', '$', ',', '[', ']', '<', '>', '#', '%', '\"', '{', '}', '\n', '\t', ' '};

    private static final String[] BAD_CMP_CHARS_ESCAPES = {"%3B", "%2F", "%3F", "%3A", "%40", "%26", "%3D",
        "%2B", "%24", "%2C", "%5B", "%5D", "%3C", "%3E", "%23", "%25", "%22", "%7B", "%7D", "%0A", "%09", "%20"};
    /**
     * Constructor. Since all methods are static we don't want instantiations of the class.
     */
    private StringHelper() {
        throw new UnsupportedOperationException();
    }

    /**
     * Escape characters that have special meaning in XML.
     *
     * @param s
     *            - The string to escape.
     * @return escaped string.
     */
    public static String escapeXml(String s) {
        return escapeString(s, BAD_XML_CHARS, BAD_XML_CHARS_ESCAPES);
    }

    /**
     * Escapes IRI's reserved characters in the given URL string.
     *
     * @param url
     *            is a string.
     * @return escaped URI
     */
    public static String encodeToIRI(String url) {
        return escapeString(url, BAD_IRI_CHARS, BAD_IRI_CHARS_ESCAPES);
    }

    /**
     * Escape characters that have special meaning.
     *
     * @param s
     *            - The string to escape.
     * @param badChars
     *            - A list of the characters that are not allowed.
     * @param escapeStrings
     *            - A list of the strings to escape to.
     * @return escaped string.
     */
    private static String escapeString(String s, char[] badChars, String[] escapeStrings) {
        if (s == null) {
            return s;
        }
        int length = s.length();
        int newLength = length;
        // first check for characters that might
        // be dangerous and calculate a length
        // of the string that has escapes.
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            for (int badInx = 0; badInx < badChars.length; badInx++) {
                if (c == badChars[badInx]) {
                    newLength += escapeStrings[badInx].length() - 1;
                    break;
                }
            }
        }
        if (length == newLength) {
            // nothing to escape in the string
            return s;
        }
        StringBuffer sb = new StringBuffer(newLength);
        boolean found;
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            found = false;
            for (int badInx = 0; badInx < badChars.length; badInx++) {
                if (c == badChars[badInx]) {
                    sb.append(escapeStrings[badInx]);
                    found = true;
                    break;
                }
            }
            if (!found) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * %-escapes the given string for a legal URI component. See http://www.ietf.org/rfc/rfc3986.txt section 2.4 for more.
     *
     * Does java.net.URLEncoder.encode(String, String) and then on the resulting string does the following corrections: - the "+"
     * signs are converted into "%20". - "%21", "%27", "%28", "%29" and "%7E" are unescaped back (i.e. "!", "'", "(", ")" and "~").
     * See the JavaDoc of java.net.URLEncoder and the above RFC specification for why this is done.
     *
     * @param s
     *            The string to %-escape.
     * @param enc
     *            The encoding scheme to use.
     * @return The escaped string.
     */
    public static String encodeURIComponent(String s, String enc) {
        return escapeString(s, BAD_CMP_CHARS, BAD_CMP_CHARS_ESCAPES);
    }
//    public static String encodeURIComponent(String s, String enc) {
//        try {
//            return URLEncoder.encode(s, enc).replaceAll("\\+", "%20").replaceAll("\\%21", "!").replaceAll("\\%27", "'")
//                    .replaceAll("\\%28", "(").replaceAll("\\%29", ")").replaceAll("\\%7E", "~");
//        } catch (UnsupportedEncodingException e) {
//            // This exception should never occur.
//            return s;
//        }
//    }

}
