package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;
import org.junit.Before;
import java.io.ByteArrayOutputStream;

/**
 * Test the module.
 *
 * @see http://onjava.com/pub/a/onjava/2003/11/12/reflection.html
 * @see http://download.oracle.com/javase/tutorial/reflect/class/index.html
 * @see http://tutorials.jenkov.com/java-reflection/private-fields-and-methods.html
 */
public class GenerateRDFTest {

    private GenerateRDF classToTest;
    private ByteArrayOutputStream testOutput;

    @Before
    public void setUp() throws Exception {
        testOutput = new ByteArrayOutputStream();
        Properties props = new Properties();
        props.setProperty("tables", "coubiogeoreg     events  ");
        props.setProperty("vocabulary", "http://voc");
        props.setProperty("xmlns.rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        props.setProperty("datatype.int", "xsd:integer");
        props.setProperty("objectproperty.org", "orgs");
        classToTest = new GenerateRDF(testOutput, null, props);
    }

    private void callParseName(String testString, String testDatatype, String expectedName, String expectedDatatype,
            String expectedLangcode) throws Exception {

        RDFField f = classToTest.parseName(testString, testDatatype);
        assertEquals(expectedName, f.name);
        assertEquals(expectedDatatype,  f.datatype);
        assertEquals(expectedLangcode,  f.langcode);
    }

    @Test
    public void testParseName() throws Exception {
        callParseName("hasRef->export", "", "hasRef", "->export", "");
        callParseName("hasRef->", "", "hasRef", "->", "");
        callParseName("price^^xsd:decimal", "", "price", "xsd:decimal", "");
        callParseName("title@de", "", "title", "", "de");
        callParseName("rdfs:label@de", "", "rdfs:label", "", "de");
        callParseName("rdfs:label", "", "rdfs:label", "", "");
        callParseName("title", "xsd:string", "title", "xsd:string", "");
    }

    private void callInjectIdentifier(String testQuery, String testIdentifier, String expectedQuery) throws Exception {
        String f;
        f = classToTest.injectHaving(testQuery, testIdentifier);
        assertEquals(expectedQuery, f);
    }

    @Test
    public void testInjectHaving() throws Exception {
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
        String f;
        f = classToTest.injectWhere(testQuery, testKey, testIdentifier);
        assertEquals(expectedQuery, f);
    }

    @Test
    public void testInjectWhere() throws Exception {
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
    public void testEncodeURIComponent() {
        String testString = ";/?:@&=+$,aA-_.!~*'()[]<>#%\"{}\n\t ";
        String expected = "%3B%2F%3F%3A%40%26%3D%2B%24%2CaA-_.!~*'()%5B%5D%3C%3E%23%25%22%7B%7D%0A%09%20";
        String actual = StringHelper.encodeURIComponent(testString, "UTF-8");
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAllTables() throws Exception {
        String[] t = classToTest.getAllTables();
        assertEquals("coubiogeoreg", t[0]);
        assertEquals("events", t[1]);
    }


    private boolean callSwitch(Object id, Object currentId) {
        return (currentId != null && !currentId.equals(id));
    }

    @Test
    public void testIdSwitch() {
        assertEquals(false, callSwitch(null, null));
        assertEquals(true, callSwitch(null, (Object) "/.."));
        assertEquals(false, callSwitch((Object) "x", (Object) "x"));
        assertEquals(true, callSwitch((Object) "A", (Object) "x"));
        assertEquals(false, callSwitch((Object) "id", null));
    }

    @Test
    public void writeFooter() throws Exception {
        classToTest.writeRdfFooter();
        assertEquals("</rdf:RDF>\n", testOutput.toString());
    }

    @Test
    public void testDocumentInformation() throws Exception {
        classToTest.exportDocumentInformation();
        assertEquals("", testOutput.toString());
    }

    // Test writeProperty()

    @Test
    public void writeNull() throws Exception {
        RDFField f = new RDFField();
        f.name = "rdfs:label";
        f.datatype = "";
        f.langcode = "";
        classToTest.writeProperty(f, null);
        assertEquals("", testOutput.toString());
    }

    @Test
    public void writeLiteral1() throws Exception {
        RDFField f = new RDFField();
        f.name = "rdfs:label";
        f.datatype = "";
        f.langcode = "";
        classToTest.writeProperty(f, "This is a label");
        assertEquals(" <rdfs:label>This is a label</rdfs:label>\n", testOutput.toString());
    }

    @Test
    public void writeLiteral2() throws Exception {
        RDFField f = new RDFField("rdfs:label", "", "");
        classToTest.writeProperty(f, "This is a label");
        assertEquals(" <rdfs:label>This is a label</rdfs:label>\n", testOutput.toString());
    }

    @Test
    public void writeReference1() throws Exception {
        RDFField f = new RDFField("foaf:page", "->", "");
        classToTest.writeProperty(f, "http://mypage.org/index.html");
        assertEquals(" <foaf:page rdf:resource=\"http://mypage.org/index.html\"/>\n", testOutput.toString());
    }

    @Test
    public void writeReference2() throws Exception {
        RDFField f = new RDFField("foaf:page", "->http://mypage.org/index.html", "");
        classToTest.writeProperty(f, "");
        assertEquals(" <foaf:page rdf:resource=\"http://mypage.org/index.html/\"/>\n", testOutput.toString());
    }

    @Test
    public void writeReference3() throws Exception {
        RDFField f = new RDFField("hasSpecies", "->http://eunis.eea.europa.eu/species", "");
        classToTest.writeProperty(f, "1366");
        //System.out.println(testOutput.toString());
        assertEquals(" <hasSpecies rdf:resource=\"http://eunis.eea.europa.eu/species/1366\"/>\n", testOutput.toString());
    }

    @Test
    public void writeReference4() throws Exception {
        RDFField f = new RDFField("hasSpecies", "->species", "");
        classToTest.writeProperty(f, "1366");
        //System.out.println(testOutput.toString());
        assertEquals(" <hasSpecies rdf:resource=\"#species/1366\"/>\n", testOutput.toString());
    }

    @Test
    public void writeInt() throws Exception {
        RDFField f = new RDFField("hasNumber", "xsd:int", "");
        classToTest.writeProperty(f, "1366");
        //System.out.println(testOutput.toString());
        assertEquals(" <hasNumber rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1366</hasNumber>\n", testOutput.toString());
    }

    @Test
    public void writeOwnType() throws Exception {
        RDFField f = new RDFField("hasDistance", "http://buzz#lightyear", "");
        classToTest.writeProperty(f, "20");
        assertEquals(" <hasDistance rdf:datatype=\"http://buzz#lightyear\">20</hasDistance>\n", testOutput.toString());
    }

    @Test
    public void writeLitWithLang() throws Exception {
        RDFField f = new RDFField("hello", "", "de");
        classToTest.writeProperty(f, "Welt");
        //System.out.println(testOutput.toString());
        assertEquals(" <hello xml:lang=\"de\">Welt</hello>\n", testOutput.toString());
    }

    @Test
    public void writeIntWithLang() throws Exception {
        RDFField f = new RDFField("hello", "xsd:int", "de");
        classToTest.writeProperty(f, "20");
        assertEquals(" <hello rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">20</hello>\n", testOutput.toString());
    }

}
