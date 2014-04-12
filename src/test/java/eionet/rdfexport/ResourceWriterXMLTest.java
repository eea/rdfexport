package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the module.
 *
 * @see http://onjava.com/pub/a/onjava/2003/11/12/reflection.html
 * @see http://download.oracle.com/javase/tutorial/reflect/class/index.html
 * @see http://tutorials.jenkov.com/java-reflection/private-fields-and-methods.html
 */
public class ResourceWriterXMLTest {

    private ResourceWriterXML classToTest;
    private ByteArrayOutputStream testOutput;
    private OutputStreamWriter testWriter;

    @Before
    public void setUp() throws Exception {
        testOutput = new ByteArrayOutputStream();
        testWriter = new OutputStreamWriter(testOutput, "UTF-8");

        classToTest = new ResourceWriterXML(testWriter);
        classToTest.setVocabulary("http://voc");
        classToTest.addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }

    // Test writeProperty()

    /**
     * A null value shall not create output.
     */
    @Test
    public void writeNull() throws Exception {
        RDFField f = new RDFField();
        f.name = "rdfs:label";
        f.datatype = "";
        f.langcode = "";
        classToTest.writeProperty(f, null);
        testWriter.close();
        assertEquals("", testOutput.toString());
    }

    /**
     * An empty string value shall create output.
     */
    @Test
    public void writeEmpty() throws Exception {
        RDFField f = new RDFField();
        f.name = "rdfs:label";
        f.datatype = "";
        f.langcode = "";
        classToTest.writeProperty(f, "");
        testWriter.close();
        assertEquals(" <rdfs:label></rdfs:label>\n", testOutput.toString());
    }

    @Test
    public void writeLiteral1() throws Exception {
        RDFField f = new RDFField();
        f.name = "rdfs:label";
        f.datatype = "";
        f.langcode = "";
        classToTest.writeProperty(f, "This is a label");
        testWriter.close();
        assertEquals(" <rdfs:label>This is a label</rdfs:label>\n", testOutput.toString());
    }

    @Test
    public void writeLiteral2() throws Exception {
        RDFField f = new RDFField("rdfs:label", "", "");
        classToTest.writeProperty(f, "This is a label");
        testWriter.close();
        assertEquals(" <rdfs:label>This is a label</rdfs:label>\n", testOutput.toString());
    }

    /**
     * Write a reference where the URL to link to is fully in the value. It is then assumed it is already URI-encoded.
     */
    @Test
    public void writeReference1() throws Exception {
        RDFField f = new RDFField("foaf:page", "->", "");
        classToTest.writeProperty(f, "http://mypage.org/index.html");
        testWriter.close();
        assertEquals(" <foaf:page rdf:resource=\"http://mypage.org/index.html\"/>\n", testOutput.toString());
    }

    /**
     * Write a reference to a complex foaf:page. Typical usage scenario.
     */
    @Test
    public void writeReferenceComplex() throws Exception {
        RDFField f = new RDFField("foaf:page", "->", "");
        classToTest.writeProperty(f, "http://mypage.org/green spider/index.html#here");
        testWriter.close();
        assertEquals(" <foaf:page rdf:resource=\"http://mypage.org/green%20spider/index.html#here\"/>\n", testOutput.toString());
    }

    /**
     * Write a reference to a foaf:page with a Query String. Typical usage scenario.
     */
    @Test
    public void writeReferenceQS() throws Exception {
        RDFField f = new RDFField("foaf:page", "->", "");
        classToTest.writeProperty(f, "http://mypage.org/greenspider/page.php?type=species&id=9288#x");
        testWriter.close();
        assertEquals(" <foaf:page rdf:resource=\"http://mypage.org/greenspider/page.php?type=species&amp;id=9288#x\"/>\n", testOutput.toString());
    }

    /**
     * Write a reference to a simple foaf:page. Typical usage scenario.
     */
    @Test
    public void writeReference2() throws Exception {
        RDFField f = new RDFField("foaf:page", "->http://mypage.org/index.html", "");
        classToTest.writeProperty(f, "");
        testWriter.close();
        assertEquals(" <foaf:page rdf:resource=\"http://mypage.org/index.html/\"/>\n", testOutput.toString());
    }

    /**
     * Write a reference where the value is relative. It is assumed the value is not already URI-encoded.
     */
    @Test
    public void writeReference3() throws Exception {
        RDFField f = new RDFField("hasSpecies", "->http://eunis.eea.europa.eu/species", "");
        classToTest.writeProperty(f, "canis lupus/linnaeus");
        //System.out.println(testOutput.toString());
        testWriter.close();
        assertEquals(" <hasSpecies rdf:resource=\"http://eunis.eea.europa.eu/species/canis%20lupus/linnaeus\"/>\n", testOutput.toString());
    }

    /**
     * Write a reference where the value is relative. It is assumed the value is not already URI-encoded.
     */
    @Test
    public void writeReferencePrtr() throws Exception {
        RDFField f = new RDFField("prtr:Pollutant", "->http://prtr.ec.europa.eu/pollutant", "");
        classToTest.writeProperty(f, "ICHLOROETHANE-1,2 (DCE)");
        //System.out.println(testOutput.toString());
        testWriter.close();
        assertEquals(" <prtr:Pollutant rdf:resource=\"http://prtr.ec.europa.eu/pollutant/ICHLOROETHANE-1,2%20(DCE)\"/>\n", testOutput.toString());
    }

    @Test
    public void writeReference4() throws Exception {
        RDFField f = new RDFField("hasSpecies", "->species", "");
        classToTest.writeProperty(f, "1366");
        //System.out.println(testOutput.toString());
        testWriter.close();
        assertEquals(" <hasSpecies rdf:resource=\"#species/1366\"/>\n", testOutput.toString());
    }

    @Test
    public void writeInt() throws Exception {
        RDFField f = new RDFField("hasNumber", "xsd:int", "");
        classToTest.writeProperty(f, "1366");
        //System.out.println(testOutput.toString());
        testWriter.close();
        assertEquals(" <hasNumber rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1366</hasNumber>\n", testOutput.toString());
    }

    @Test
    public void writeOwnType() throws Exception {
        RDFField f = new RDFField("hasDistance", "http://buzz#lightyear", "");
        classToTest.writeProperty(f, "20");
        testWriter.close();
        assertEquals(" <hasDistance rdf:datatype=\"http://buzz#lightyear\">20</hasDistance>\n", testOutput.toString());
    }

    @Test
    public void writeLitWithLang() throws Exception {
        RDFField f = new RDFField("hello", "", "de");
        classToTest.writeProperty(f, "Welt");
        //System.out.println(testOutput.toString());
        testWriter.close();
        assertEquals(" <hello xml:lang=\"de\">Welt</hello>\n", testOutput.toString());
    }

    @Test
    public void writeIntWithLang() throws Exception {
        RDFField f = new RDFField("hello", "xsd:int", "de");
        classToTest.writeProperty(f, "20");
        testWriter.close();
        assertEquals(" <hello rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">20</hello>\n", testOutput.toString());
    }

    /**
     * An emplty value shall not create output if emptystringisnull is set to true.
     */
    @Test
    public void writeNullWhenEmpty() throws Exception {
        ResourceWriterXML testClass = new ResourceWriterXML(testWriter);
        testClass.setVocabulary("http://voc");
        testClass.setEmptyStringIsNull(true);

        RDFField f = new RDFField();
        f.name = "rdfs:label";
        f.datatype = "";
        f.langcode = "";
        testClass.writeProperty(f, "");
        testWriter.close();
        assertEquals("", testOutput.toString());
    }

}
