package eionet.rdfexport;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
//import org.apache.commons.lang.StringUtils;

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
    private static final String[] BAD_IRI_CHARS = {" ", "{", "}", "<", ">", "\"", "|", "\\", "^", "`", "+"};
    /** Replacements for characters that aren't allowed in IRIs. */
    private static final String[] BAD_IRI_CHARS_ESCAPES = {"%20", "%7B", "%7D", "%3C", "%3E", "%22", "%7C", "%5C", "%5E", "%60", "%2B"};

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
        int length = s.length();
        int newLength = length;
        // first check for characters that might
        // be dangerous and calculate a length
        // of the string that has escapes.
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"':
                    newLength += 5;
                    break;
                case '&':
                case '\'':
                    newLength += 4;
                    break;
                case '<':
                case '>':
                    newLength += 3;
                    break;
                default:
                    break;
            }
        }
        if (length == newLength) {
            // nothing to escape in the string
            return s;
        }
        StringBuffer sb = new StringBuffer(newLength);
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                default:
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
        try {
            return URLEncoder.encode(s, enc).replaceAll("\\+", "%20").replaceAll("\\%21", "!").replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(").replaceAll("\\%29", ")").replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            // This exception should never occur.
            return s;
        }
    }

//    /**
//     * Escapes IRI's reserved characters in the given URL string.
//     *
//     * @param url
//     *            is a string.
//     * @return escaped URI
//     */
//    public static String encodeToIRI(String url) {
//
//        return (url == null) ? null : StringUtils.replaceEach(url, BAD_IRI_CHARS, BAD_IRI_CHARS_ESCAPES);
//    }

}
