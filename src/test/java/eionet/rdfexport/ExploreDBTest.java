package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/*
 * Test ExploreDB class
 */
public class ExploreDBTest {

    private static final String JDBC_DRIVER = org.h2.Driver.class.getName();
    private static final String JDBC_URL = "jdbc:h2:mem:test";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static final String UTF8_ENCODING = "UTF-8";

    private GenerateRDF classToTest;
    private ByteArrayOutputStream testOutput;
    private OutputStreamWriter testWriter;
    private Properties props;
    private Connection dbConn;

    private void createSchema() throws Exception {
        Statement statement = dbConn.createStatement();
        statement.executeUpdate("create table if not exists ORGANISATION ("
            + "ORG_ID int identity,"
            + "NAME varchar(100))");
        statement.executeUpdate("create table if not exists CUSTOMER ("
            + "CUSTOMER_ID int identity primary key,"
            + "NAME varchar(100),"
            + "LAST_NAME varchar(100),"
            + "ORG_ID int,"
            + "CREATED DATE"
            + ")");
        statement.executeUpdate("create table if not exists INVOICE ("
            + "INVOICE_ID int identity,"
            + "BILLING int,"
            + "DELIVERY int,"
            + "TOTAL decimal(6,2),"
            + "foreign key (BILLING) references CUSTOMER(CUSTOMER_ID),"
            + "foreign key (DELIVERY) references CUSTOMER)");
        statement.executeUpdate("create table if not exists INVOICEITEM ("
            + "INVOICE_ID int,"
            + "LINE_ID int,"
            + "DESCRIPTION varchar(100),"
            + "AMOUNT int,"
            + "PRICE decimal(6,2),"
            + "PRIMARY KEY (INVOICE_ID, LINE_ID))");
        statement.close();
    }

    @Before
    public void importDataSet() throws Exception {
        dbConn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        createSchema();
        IDataSet dataSet = readDataSet();
        cleanlyInsert(dataSet);

        testOutput = new ByteArrayOutputStream();
        testWriter = new OutputStreamWriter(testOutput, UTF8_ENCODING);
        props = new Properties();
        props.setProperty("tables", "person events");
        props.setProperty("vocabulary", "http://voc");
        props.setProperty("datatype.integer", "xsd:integer");
        props.setProperty("datatype.decimal", "xsd:decimal");
        props.setProperty("datatype.date", "xsd:date");

        assertEquals("H2 database expected", "h2", ExploreDB.getDBProductName(dbConn));
        props.setProperty("sqldialect.h2.skiptables",
              "CATALOGS COLLATIONS COLUMNS COLUMN_PRIVILEGES CONSTANTS CONSTRAINTS" // H2
            + " CROSS_REFERENCES DOMAINS FUNCTION_ALIASES FUNCTION_COLUMNS HELP "
            + " INDEXES IN_DOUBT LOCKS RIGHTS ROLES SCHEMATA SEQUENCES SESSIONS "
            + " SESSION_STATE SETTINGS TABLES TABLE_PRIVILEGES TABLE_TYPES TRIGGERS "
            + " TYPE_INFO USERS VIEWS");
        props.setProperty("sqldialect.access.skiptables",
             "VALIDATION_METADATA_DO_NOT_MODIFY" // DataDict reserved table
            + " MSYSACCESSOBJECTS MSYSACCESSXML MSYSACES MSYSOBJECTS MSYSQUERIES MSYSRELATIONSHIPS");
        props.setProperty("sqldialect.h2.column.before", "'");
        props.setProperty("sqldialect.h2.column.after", "'");
        props.setProperty("sqldialect.access.column.before", "[");
        props.setProperty("sqldialect.access.column.after", "]");
    }

    @After
    public void closeAll() throws Exception {
        testWriter.close();
        testOutput.close();
        dbConn.close();
        dbConn = null;
    }

    private IDataSet readDataSet() throws Exception {
        InputStream is = ExploreDBTest.class.getClassLoader().getResourceAsStream("seed-complex.xml");
        return new FlatXmlDataSetBuilder().build(is);
    }

    private void cleanlyInsert(IDataSet dataSet) throws Exception {
        IDatabaseTester databaseTester = new JdbcDatabaseTester(JDBC_DRIVER, JDBC_URL, USER, PASSWORD);
        databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        databaseTester.setDataSet(dataSet);
        databaseTester.onSetup();
    }

    private String loadFile(String fileName) throws Exception {
        InputStream is = ExploreDBTest.class.getClassLoader().getResourceAsStream(fileName);
        return IOUtils.toString(is, UTF8_ENCODING);
    }

    /*
     * Test ExploreDB class
     */
    @Test
    public void exploreInvoice() throws Exception {
        ExploreDB edb = new ExploreDB(dbConn, props, false);
        edb.discoverTables(false);
        assertEquals("discovered tables", "CUSTOMER INVOICE INVOICEITEM ORGANISATION ", props.getProperty("tables"));

        String expected = "SELECT '' || customer_id AS id, '' || customer_id AS 'rdfs:label',"
           + " `customer_id` AS 'customer_id',"
           + " `name` AS 'name', `last_name` AS 'last_name',"
           + " `org_id` AS 'org_id->ORGANISATION', `created` AS 'created' FROM CUSTOMER";
        assertEquals(expected, props.getProperty("CUSTOMER.query"));
    }

    /*
     * Force a timestamp to be exported as xsd:integer via the properties file.
     */
    @Test
    public void exploreInvoiceWithBadTypes() throws Exception {
        props.setProperty("datatype.date", "xsd:integer");
        ExploreDB edb = new ExploreDB(dbConn, props, false);
        edb.discoverTables(true);
        assertEquals("discovered tables", "CUSTOMER INVOICE INVOICEITEM ORGANISATION ", props.getProperty("tables"));

        String expected = "SELECT '' || customer_id AS id, '' || customer_id AS 'rdfs:label',"
           + " `customer_id` AS 'customer_id^^xsd:integer',"
           + " `name` AS 'name@', `last_name` AS 'last_name@',"
           + " `org_id` AS 'org_id->ORGANISATION', `created` AS 'created^^xsd:integer' FROM CUSTOMER";
        assertEquals(expected, props.getProperty("CUSTOMER.query"));
    }

    @Test
    public void exploreInvoiceWithTypes() throws Exception {
        String expected;
        ExploreDB edb = new ExploreDB(dbConn, props, false);

        edb.discoverTables(true);
        assertEquals("discovered tables", "CUSTOMER INVOICE INVOICEITEM ORGANISATION ", props.getProperty("tables"));

        expected = "SELECT '' || customer_id AS id, '' || customer_id AS 'rdfs:label',"
           + " `customer_id` AS 'customer_id^^xsd:integer',"
           + " `name` AS 'name@', `last_name` AS 'last_name@',"
           + " `org_id` AS 'org_id->ORGANISATION', `created` AS 'created^^xsd:date' FROM CUSTOMER";
        assertEquals(expected, props.getProperty("CUSTOMER.query"));

        expected = "SELECT '' || invoice_id AS id, "
         + "'' || invoice_id AS 'rdfs:label', "
         + "`invoice_id` AS 'invoice_id^^xsd:integer', "
         + "`billing` AS 'billing->CUSTOMER', "
         + "`delivery` AS 'delivery->CUSTOMER', "
         + "`total` AS 'total^^xsd:decimal' FROM INVOICE";
        assertEquals(expected, props.getProperty("INVOICE.query"));

        expected = "SELECT '' || invoice_id || line_id AS id, "
         + "'' || invoice_id || line_id AS 'rdfs:label', "
         + "`invoice_id` AS 'invoice_id->INVOICE', "
         + "`line_id` AS 'line_id^^xsd:integer', "
         + "`description` AS 'description@', "
         + "`amount` AS 'amount^^xsd:integer', "
         + "`price` AS 'price^^xsd:decimal' FROM INVOICEITEM";
        assertEquals(expected, props.getProperty("INVOICEITEM.query"));
    }

}