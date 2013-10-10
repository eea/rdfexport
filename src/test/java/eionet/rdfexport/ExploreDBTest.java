package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test ExploreDB class.
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
        statement.executeUpdate("create table if not exists \"INVOICE ITEM\" ("
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
        props.setProperty("sqldialect.h2.column.before", "\"");
        props.setProperty("sqldialect.h2.column.after", "\"");
        props.setProperty("sqldialect.h2.alias.before", "\"");
        props.setProperty("sqldialect.h2.alias.after", "\"");
        props.setProperty("sqldialect.h2.concat", "and");

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
        ExploreDB edb = new ExploreDB(dbConn, props);
        edb.discoverTables(false);
        assertEquals("discovered tables", "customer invoice invoice%20item organisation ", props.getProperty("tables"));

        String expected = "SELECT '' || customer_id AS id, '' || customer_id AS \"rdfs:label\","
           + " \"CUSTOMER_ID\" AS \"customer_id\","
           + " \"NAME\" AS \"name\", \"LAST_NAME\" AS \"last_name\","
           + " \"ORG_ID\" AS \"org_id->organisation\", \"CREATED\" AS \"created\" FROM \"PUBLIC\".\"CUSTOMER\"";
        assertEquals(expected, props.getProperty("customer.query"));
    }

    /**
     * Force a timestamp to be exported as xsd:integer via the properties file.
     */
    @Test
    public void exploreInvoiceWithBadTypes() throws Exception {
        props.setProperty("datatype.date", "xsd:integer");
        ExploreDB edb = new ExploreDB(dbConn, props);
        edb.discoverTables(true);
        assertEquals("discovered tables", "customer invoice invoice%20item organisation ", props.getProperty("tables"));

        String expected = "SELECT '' || customer_id AS id, '' || customer_id AS \"rdfs:label\","
           + " \"CUSTOMER_ID\" AS \"customer_id^^xsd:integer\","
           + " \"NAME\" AS \"name@\", \"LAST_NAME\" AS \"last_name@\","
           + " \"ORG_ID\" AS \"org_id->organisation\", \"CREATED\" AS \"created^^xsd:integer\" FROM \"PUBLIC\".\"CUSTOMER\"";
        assertEquals(expected, props.getProperty("customer.query"));
    }

    @Test
    public void exploreInvoiceWithTypes() throws Exception {
        String expected;
        ExploreDB edb = new ExploreDB(dbConn, props);
        edb.discoverTables(true);
        assertEquals("discovered tables", "customer invoice invoice%20item organisation ", props.getProperty("tables"));

        expected = "SELECT '' || customer_id AS id, '' || customer_id AS \"rdfs:label\","
           + " \"CUSTOMER_ID\" AS \"customer_id^^xsd:integer\","
           + " \"NAME\" AS \"name@\", \"LAST_NAME\" AS \"last_name@\","
           + " \"ORG_ID\" AS \"org_id->organisation\", \"CREATED\" AS \"created^^xsd:date\" FROM \"PUBLIC\".\"CUSTOMER\"";
        assertEquals(expected, props.getProperty("customer.query"));

        expected = "SELECT '' || invoice_id AS id, "
         + "'' || invoice_id AS \"rdfs:label\", "
         + "\"INVOICE_ID\" AS \"invoice_id^^xsd:integer\", "
         + "\"BILLING\" AS \"billing->customer\", "
         + "\"DELIVERY\" AS \"delivery->customer\", "
         + "\"TOTAL\" AS \"total^^xsd:decimal\" FROM \"PUBLIC\".\"INVOICE\"";
        assertEquals(expected, props.getProperty("invoice.query"));

        expected = "SELECT '' || invoice_id || line_id AS id, "
         + "'' || invoice_id || line_id AS \"rdfs:label\", "
         + "\"INVOICE_ID\" AS \"invoice_id->invoice\", "
         + "\"LINE_ID\" AS \"line_id^^xsd:integer\", "
         + "\"DESCRIPTION\" AS \"description@\", "
         + "\"AMOUNT\" AS \"amount^^xsd:integer\", "
         + "\"PRICE\" AS \"price^^xsd:decimal\" FROM \"PUBLIC\".\"INVOICE ITEM\"";
        assertEquals(expected, props.getProperty("invoice%20item.query"));
    }

    @Test
    public void listTables() throws Exception {
        ExploreDB dbExplorer = new ExploreDB(dbConn, props);
        List<TableSpec> tableToAskExport = dbExplorer.listTables();
        List<String> listOfTableNames = new ArrayList<String>();
        for (TableSpec tableSpec : tableToAskExport) {
        //    System.out.println(tableSpec.tableName);
            listOfTableNames.add(tableSpec.tableName);
        }
        assertTrue(listOfTableNames.contains("CUSTOMER"));
        assertTrue(listOfTableNames.contains("INVOICE ITEM"));
        assertEquals("Size of list expected to be 4", 4, tableToAskExport.size());
    }

    @Test
    public void registerTablesNoChanges() throws Exception {
        ExploreDB dbExplorer = new ExploreDB(dbConn, props);
        List<TableSpec> tableToAskExport = dbExplorer.listTables();
        dbExplorer.registerTables(tableToAskExport);
        for (TableSpec tableSpec : tableToAskExport) {
            Map<String, String> simpleForeignKeys = dbExplorer.getSimpleForeignKeys(tableSpec.tableName);
        }
    }

    @Ignore @Test
    public void registerOneTable() throws Exception {
        ExploreDB dbExplorer = new ExploreDB(dbConn, props);
        //List<TableSpec> tableToAskExport = dbExplorer.listTables();
        ArrayList<TableSpec> tablesToRegister = new ArrayList<TableSpec>();
        tablesToRegister.add(new TableSpec("CUSTOMER"));
        tablesToRegister.add(new TableSpec("ORGANISATION"));
        tablesToRegister.add(new TableSpec("INVOICE"));
        dbExplorer.registerTables(tablesToRegister);
        for (TableSpec tableSpec : tablesToRegister) {
            Map<String, String> simpleForeignKeys = dbExplorer.getSimpleForeignKeys(tableSpec.tableName);
            assertNotNull(simpleForeignKeys);
            assertEquals(1, simpleForeignKeys.size());
            System.out.println(simpleForeignKeys);
        }
    }

    @Ignore @Test
    public void getInvoiceTable() throws Exception {
        ExploreDB dbExplorer = new ExploreDB(dbConn, props);
        List<TableSpec> tableToAskExport = dbExplorer.listTables();
        TableSpec ts = dbExplorer.getTable("INVOICE");
        assertNotNull(ts);
    }
}
