package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.sql.Timestamp;
import org.junit.Test;

public class DatatypesTest {

    @Test
    public void noEscapeNeeded() throws Exception {
        String input = "This is a simple string";
        String expct = "This is a simple string";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }

    @Test
    public void escapeNeeded() throws Exception {
        String input = "Fruit & vegetables";
        String expct = "Fruit & vegetables";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }

    @Test
    public void asCharArray() throws Exception {
        String strInput = "Fruit & vegetables";
        byte[] input = strInput.getBytes("UTF8");
        String expct = "Fruit & vegetables";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }

    @Test
    public void sqlDate() throws Exception {
        Date input = new Date(1367359200000L); // 1 May 2013
        String expct = "2013-05-01";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }

    @Test
    public void sqlDateTime() throws Exception {
        long testTS = 1367359201000L;
        long HOUR = 60L * 60L * 1000L;

        Timestamp input = new Timestamp(testTS); // 1 May 2013 One second past midnight.
        String expct = "2013-05-01T00:00:01";
        assertEquals(expct, Datatypes.getFormattedValue(input));

        input = new Timestamp(testTS + HOUR * 12); // 1 May 2013 One second past 12 (noon).
        expct = "2013-05-01T12:00:01";
        assertEquals(expct, Datatypes.getFormattedValue(input));

        input = new Timestamp(testTS + HOUR); // 1 May 2013 One hour and one second past midnight.
        expct = "2013-05-01T01:00:01";
        assertEquals(expct, Datatypes.getFormattedValue(input));

        input = new Timestamp(testTS + HOUR * 19); // 1 May 2013 One second past 19.
        expct = "2013-05-01T19:00:01";
        assertEquals(expct, Datatypes.getFormattedValue(input));
    }
}
