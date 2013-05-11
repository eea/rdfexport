package eionet.rdfexport;

//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;

/**
 * Class to help escape strings for XML and URI components.
 *
 * @see http://www.java2s.com/Tutorial/Java/0120__Development/EscapeHTML.htm
 * @see http://www.ietf.org/rfc/rfc3986.txt
 */
public final class StringEncoder {
    /**
     * Characters that aren't allowed in IRIs. Special consideration for plus (+): It is historically used to encode space. If we
     * leave it unencoded, then it could be mistakenly decoded back to a space.
     */
    private static final char[] BAD_IRI_CHARS = {' ', '{', '}', '<', '>', '"', '|', '\\', '^', '`', '+'};
    /** Replacements for characters that aren't allowed in IRIs. */
    private static final String[] BAD_IRI_CHARS_ESCAPES = {"%20", "%7B", "%7D", "%3C", "%3E",
                                                        "%22", "%7C", "%5C", "%5E", "%60", "%2B"};

    /** Characters that aren't allowed in XML.  */
    private static final char[] BAD_XML_CHARS = {'\'', '"', '&', '<', '>'};
    /** Replacements for characters that aren't allowed in XML. */
    private static final String[] BAD_XML_CHARS_ESCAPES = {"&#39;", "&quot;", "&amp;", "&lt;", "&gt;"};

    /** Characters that aren't allowed in the local part of a URI.  */
    private static final char[] BAD_CMP_CHARS = {';', '/', '?', ':', '@', '&', '=',
        '+', '$', ',', '[', ']', '<', '>',
        '#', '%', '\"', '{', '}', '\n', '\t', ' '};

    /** Replacements for characters that aren't allowed in the local part of a URI. */
    private static final String[] BAD_CMP_CHARS_ESCAPES = {"%3B", "%2F", "%3F", "%3A", "%40", "%26", "%3D",
        "%2B", "%24", "%2C", "%5B", "%5D", "%3C", "%3E",
        "%23", "%25", "%22", "%7B", "%7D", "%0A", "%09", "%20"};
    /**
     * Constructor. Since all methods are static we don't want instantiations of the class.
     */
    private StringEncoder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Escape characters that have special meaning in XML.
     *
     * @param s
     *            - The string to escape.
     * @return escaped string.
     */
    public static String encodeToXml(String s) {
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
     * Percent-escapes the given string for a legal URI component.
     * See http://www.ietf.org/rfc/rfc3986.txt section 2.4 for more.
     *
     * @param s
     *            The string to %-escape.
     * @return The escaped string.
     */
    public static String encodeURIComponent(String s) {
        return escapeString(s, BAD_CMP_CHARS, BAD_CMP_CHARS_ESCAPES);
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
        int orgLength = s.length();
        int newLength = calculateNewLength(s, badChars, escapeStrings);
        if (orgLength == newLength) {
            // nothing to escape in the string
            return s;
        }
        StringBuffer sb = new StringBuffer(newLength);
        boolean found;
        for (int i = 0; i < orgLength; i++) {
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
     * Calculate how long an escaped string would be. Check for characters that might
     * be dangerous and calculate a length of the string that has escapes.
     *
     * @param s
     *            - The string to escape.
     * @param badChars
     *            - A list of the characters that are not allowed.
     * @param escapeStrings
     *            - A list of the strings to escape to.
     * @return length of new string
     */
    private static int calculateNewLength(String s, char[] badChars, String[] escapeStrings) {
        int orgLength = s.length();
        int newLength = orgLength;
        for (int i = 0; i < orgLength; i++) {
            char c = s.charAt(i);
            for (int badInx = 0; badInx < badChars.length; badInx++) {
                if (c == badChars[badInx]) {
                    newLength += escapeStrings[badInx].length() - 1;
                    break;
                }
            }
        }
        return newLength;
    }

}
