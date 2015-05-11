package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.junit.Ignore;
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
    public void sqlDate30Apr() throws Exception {
        String expectedDate = "2013-04-30";
        Date input = this.stringDatetimeToSqlDate(String.format("%sT22:00:00", expectedDate));
        
        assertEquals(expectedDate, Datatypes.getFormattedValue(input));
    }

    @Test
    public void sqlDate1May() throws Exception {
        String expectedDate = "2013-05-01";
        Date input = this.stringDatetimeToSqlDate(String.format("%sT00:05:00", expectedDate));
        
        assertEquals(expectedDate, Datatypes.getFormattedValue(input));
    }

    @Test
    public void sqlDateTime() throws Exception {
        String[] expectedDatetimes = new String[] {"2013-05-01T00:00:01", "2013-05-01T12:00:01", "2013-05-01T01:00:01", "2013-05-01T19:00:01"};
        
        for (String expectedDatetime : expectedDatetimes) {
            Timestamp input = this.stringDatetimeToSqlDatetime(expectedDatetime);
            assertEquals(expectedDatetime, Datatypes.getFormattedValue(input));
        }
    }
    
    private Date stringDatetimeToSqlDate(String datetime) throws ParseException {
        return new Date(this.stringDatetimeToTimestamp(datetime));
    }
    
    private Timestamp stringDatetimeToSqlDatetime(String datetime) throws ParseException {
        return new Timestamp(this.stringDatetimeToTimestamp(datetime));
    }
    
    /**
     * Mock function that converts a datetime value in string format into a timestamp
     * value, in the same way that the MySql jdbc driver does.
     * 
     * @param datetime The datetime value in yyyy-MM-dd'T'HH:mm:ss format
     * @return The corresponding unix timestamp
     * @throws ParseException 
     */
    private long stringDatetimeToTimestamp(String datetime) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        long millis = format.parse(datetime).getTime();
        
        return millis;
    }
}
