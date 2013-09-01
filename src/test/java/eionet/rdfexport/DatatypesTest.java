package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.Timestamp;
import org.junit.Test;

public class DatatypesTest {

    @Test
    public void noEscapeNeeded() {
        String input = "This is a simple string";
        String expct = "This is a simple string";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }

    @Test
    public void escapeNeeded() {
        String input = "Fruit & vegetables";
        String expct = "Fruit & vegetables";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }

    @Test
    public void asCharArray() throws UnsupportedEncodingException {
        String strInput = "Fruit & vegetables";
        byte[] input = strInput.getBytes("UTF8");
        String expct = "Fruit & vegetables";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }

    @Test
    public void sqlDate() {
        Date input = new Date(1367359200000L); // 1 May 2013
        String expct = "2013-05-01";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }

    @Test
    public void sqlDateTime() {
        Timestamp input = new Timestamp(1367359201000L); // 1 May 2013 One second past noon.
        String expct = "2013-05-01T12:00:01";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }
}
