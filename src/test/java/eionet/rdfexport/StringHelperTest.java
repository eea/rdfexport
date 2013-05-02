package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;

public class StringHelperTest {

    @Test
    public void noEscapeNeeded() {
        String input = "This is a simple string";
        String expct = "This is a simple string";
        assertEquals(expct, StringHelper.escapeXml(input));
    }

    @Test
    public void noEscapeOfNatl() {
        String input = "€";
        String expct = "€";
        assertEquals(expct, StringHelper.escapeXml(input));
    }

    @Test
    public void escapeAmp() {
        String input = "Fruit & vegetables";
        String expct = "Fruit &amp; vegetables";
        assertEquals(expct, StringHelper.escapeXml(input));
    }

    @Test
    public void escapeApos() {
        String input = "<div class='Apostrophs'>";
        String expct = "&lt;div class=&#39;Apostrophs&#39;&gt;";
        assertEquals(expct, StringHelper.escapeXml(input));
    }

    @Test
    public void escapeQuot() {
        String input = "<div class=\"Quotes\">";
        String expct = "&lt;div class=&quot;Quotes&quot;&gt;";
        assertEquals(expct, StringHelper.escapeXml(input));
    }

    /* ENCODE URI */
    @Test
    public void encodeURI() {
        String input = "http://site/sp ace";
        String expct = "http%3A%2F%2Fsite%2Fsp%20ace";
        assertEquals(expct, StringHelper.encodeURIComponent(input, "UTF-8"));
    }

    @Test
    public void encodeUsuals() {
        String input = "#hash+amp&";
        String expct = "%23hash%2Bamp%26";
        assertEquals(expct, StringHelper.encodeURIComponent(input, "UTF-8"));
    }

    @Test
    public void noEncodeOfSpecials() {
        String input = "(char)quote'~excl!!";
        String expct = "(char)quote'~excl!!";
        assertEquals(expct, StringHelper.encodeURIComponent(input, "UTF-8"));
    }
}
