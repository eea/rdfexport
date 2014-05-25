package eionet.rdfexport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Properties;
import static junit.framework.Assert.assertEquals;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class SortedPropertiesTest {

    @Test
    public void simpleTest() {
        String[] expectedValues = {"datatype.int", "objectproperty.org", "tables", "vocabulary", "xmlns.rdf"};

        SortedProperties props = new SortedProperties();
        props.setProperty("tables", "1");
        props.setProperty("vocabulary", "2");
        props.setProperty("xmlns.rdf", "5");
        props.setProperty("datatype.int", "3");
        props.setProperty("objectproperty.org", "4");

        int i = 0;
        for (Enumeration<Object> key = props.keys(); key.hasMoreElements();) {
            assertEquals(expectedValues[i], key.nextElement());
            i++;
        }
    }

    @Test
    public void newlineTest() throws Exception {
        String expected = "query=SELECT ID \\\n    ,surname \\\n    ,givenName FROM TABLE\n";

        SortedProperties props = new SortedProperties();
        props.setProperty("query", "SELECT ID \n    ,surname \n    ,givenName FROM TABLE");
        File temp = File.createTempFile("newlinetest", ".properties");
        props.store(new FileOutputStream(temp), "");
        String actual = IOUtils.toString(temp.toURI(), "8859_1");
        assertEquals(expected, actual);

        String pExpected =  "SELECT ID ,surname ,givenName FROM TABLE";
        Properties plainProps = new Properties();
        plainProps.load(new FileInputStream(temp));
        actual = plainProps.getProperty("query");
        assertEquals(pExpected, actual);

        temp.delete();
        //System.out.println(actual);
    }

}
