package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import org.junit.Test;

/**
 * Uses the Reflection API to get private members
 *
 * @see http://onjava.com/pub/a/onjava/2003/11/12/reflection.html
 * @see http://download.oracle.com/javase/tutorial/reflect/class/index.html
 * @see http://tutorials.jenkov.com/java-reflection/private-fields-and-methods.html
 */
public class GenerateRDFTest {

    private void callParseName(String testString, String testDatatype, String expectedName, String expectedDatatype,
            String expectedLangcode) throws Exception {

        Properties props = new Properties();
        props.setProperty("tables", "coubiogeoreg events");
        props.setProperty("vocabulary", "http://voc");
        GenerateRDF classToTest = new GenerateRDF(System.out, null, props);
        Field f;
        final Method method = classToTest.getClass().getDeclaredMethod("parseName", new Class[] {String.class, String.class});
        method.setAccessible(true);
        Object ret = method.invoke(classToTest, testString, testDatatype);
        f = ret.getClass().getDeclaredField("name");
        f.setAccessible(true);
        assertEquals(expectedName, (String) f.get(ret));

        f = ret.getClass().getDeclaredField("datatype");
        f.setAccessible(true);
        assertEquals(expectedDatatype, (String) f.get(ret));

        f = ret.getClass().getDeclaredField("langcode");
        f.setAccessible(true);
        assertEquals(expectedLangcode, (String) f.get(ret));
    }

    @Test
    public void test_parseName() throws Exception {
        callParseName("hasRef->export", "", "hasRef", "->export", "");
        callParseName("hasRef->", "", "hasRef", "->", "");
        callParseName("price^^xsd:decimal", "", "price", "xsd:decimal", "");
        callParseName("title@de", "", "title", "", "de");
        callParseName("rdfs:label@de", "", "rdfs:label", "", "de");
        callParseName("rdfs:label", "", "rdfs:label", "", "");
        callParseName("title", "xsd:string", "title", "xsd:string", "");
    }

    private void callInjectIdentifier(String testQuery, String testIdentifier, String expectedQuery) throws Exception {
        Properties props = new Properties();
        props.setProperty("tables", "coubiogeoreg events");
        props.setProperty("vocabulary", "http://voc");
        GenerateRDF classToTest = new GenerateRDF(System.out, null, props);
        String f;
        final Method method = classToTest.getClass().getDeclaredMethod("injectHaving", new Class[] {String.class, String.class});
        method.setAccessible(true);
        Object ret = method.invoke(classToTest, testQuery, testIdentifier);
        f = (String) ret;
        assertEquals(expectedQuery, f);
    }

    @Test
    public void test_injectHaving() throws Exception {
        // Test injection of identifier
        callInjectIdentifier("SELECT X AS id, * FROM Y", "819", "SELECT X AS id, * FROM Y HAVING id='819'");
        callInjectIdentifier("SELECT X AS id, * FROM Y ORDER BY postcode", "819",
                "SELECT X AS id, * FROM Y HAVING id='819' ORDER BY postcode");
        // Test injection of identifier with LIMIT
        callInjectIdentifier("SELECT X AS id, * FROM Y ORDER BY postcode LIMIT 10 OFFSET 2", "819",
                "SELECT X AS id, * FROM Y HAVING id='819' ORDER BY postcode LIMIT 10 OFFSET 2");
        callInjectIdentifier("SELECT X AS id, * FROM Y LIMIT 10 OFFSET 2", "819",
                "SELECT X AS id, * FROM Y HAVING id='819' LIMIT 10 OFFSET 2");
        // Test injection of identifier with HAVING
        callInjectIdentifier("SELECT X AS id, count(*) FROM Y GROUP BY id HAVING Z=1", "819",
                "SELECT X AS id, count(*) FROM Y GROUP BY id HAVING id='819' AND Z=1");
        callInjectIdentifier("SELECT X AS id, count(*) FROM Y GROUP BY id HAVING Z=1 ORDER BY ID", "819",
                "SELECT X AS id, count(*) FROM Y GROUP BY id HAVING id='819' AND Z=1 ORDER BY ID");
    }

    private void callInjectWhere(String testQuery, String testIdentifier, String testKey, String expectedQuery) throws Exception {
        Properties props = new Properties();
        props.setProperty("tables", "coubiogeoreg events");
        props.setProperty("vocabulary", "http://voc");
        GenerateRDF classToTest = new GenerateRDF(System.out, null, props);
        String f;
        final Method method =
                classToTest.getClass().getDeclaredMethod("injectWhere", new Class[] {String.class, String.class, String.class});
        method.setAccessible(true);
        Object ret = method.invoke(classToTest, testQuery, testKey, testIdentifier);
        f = (String) ret;
        assertEquals(expectedQuery, f);
    }

    @Test
    public void test_injectWhere() throws Exception {
        // Test injection of identifier
        callInjectWhere("SELECT X AS id, * FROM Y", "819", "X", "SELECT X AS id, * FROM Y WHERE X='819'");
        callInjectWhere("SELECT X AS id, * FROM Y HAVING id='819'", "819", "X",
                "SELECT X AS id, * FROM Y WHERE X='819' HAVING id='819'");
        callInjectWhere("SELECT X AS id, * FROM Y ORDER BY postcode", "819", "X",
                "SELECT X AS id, * FROM Y WHERE X='819' ORDER BY postcode");
        callInjectWhere("SELECT X AS id, * FROM Y HAVING id='819' ORDER BY postcode", "819", "X",
                "SELECT X AS id, * FROM Y WHERE X='819' HAVING id='819' ORDER BY postcode");
        // Test injection of identifier with LIMIT
        callInjectWhere("SELECT X AS id, * FROM Y ORDER BY postcode LIMIT 10 OFFSET 2", "819", "X",
                "SELECT X AS id, * FROM Y WHERE X='819' ORDER BY postcode LIMIT 10 OFFSET 2");
        callInjectWhere("SELECT X AS id, * FROM Y HAVING id='819' ORDER BY postcode LIMIT 10 OFFSET 2", "819", "X",
                "SELECT X AS id, * FROM Y WHERE X='819' HAVING id='819' ORDER BY postcode LIMIT 10 OFFSET 2");
        callInjectWhere("SELECT X AS id, * FROM Y LIMIT 10 OFFSET 2", "819", "X",
                "SELECT X AS id, * FROM Y WHERE X='819' LIMIT 10 OFFSET 2");
        // Test injection of identifier with HAVING
        callInjectWhere("SELECT X AS id, count(*) FROM Y GROUP BY id HAVING Z=1", "819", "X",
                "SELECT X AS id, count(*) FROM Y WHERE X='819' GROUP BY id HAVING Z=1");
        callInjectWhere("SELECT X AS id, count(*) FROM Y GROUP BY id HAVING id='819' AND Z=1", "819", "X",
                "SELECT X AS id, count(*) FROM Y WHERE X='819' GROUP BY id HAVING id='819' AND Z=1");
        callInjectWhere("SELECT X AS id, count(*) FROM Y GROUP BY id HAVING Z=1 ORDER BY ID", "819", "X",
                "SELECT X AS id, count(*) FROM Y WHERE X='819' GROUP BY id HAVING Z=1 ORDER BY ID");
    }

    @Test
    public void test_encodeURIComponent() {
        String testString = ";/?:@&=+$,aA-_.!~*'()[]<>#%\"{}\n\t ";
        String expected = "%3B%2F%3F%3A%40%26%3D%2B%24%2CaA-_.!~*'()%5B%5D%3C%3E%23%25%22%7B%7D%0A%09%20";
        String actual = StringHelper.encodeURIComponent(testString, "UTF-8");
        assertEquals(expected, actual);
    }

    private boolean callSwitch(Object id, Object currentId) {
        return (currentId != null && !currentId.equals(id));
    }

    @Test
    public void test_idSwitch() {
        assertEquals(false, callSwitch(null, null));
        assertEquals(true, callSwitch(null, (Object) "/.."));
        assertEquals(false, callSwitch((Object) "x", (Object) "x"));
        assertEquals(true, callSwitch((Object) "A", (Object) "x"));
        assertEquals(false, callSwitch((Object) "id", null));
    }

}
