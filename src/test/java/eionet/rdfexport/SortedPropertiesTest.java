package eionet.rdfexport;

import java.util.Enumeration;
import static junit.framework.Assert.assertEquals;
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
}
