package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.io.ByteArrayOutputStream;
//import java.io.FileOutputStream;
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

public class DatabaseTest {

    private static final String JDBC_DRIVER = org.h2.Driver.class.getName();
    private static final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private GenerateRDF classToTest;
    private ByteArrayOutputStream testOutput;
    private OutputStreamWriter testWriter;
    private Properties props;
    private Connection dbConn;

    private void createSchema() throws Exception {
        Statement statement = dbConn.createStatement();
        statement.executeUpdate("create table if not exists PERSON ("
            + "ID int identity primary key,"
            + "NAME varchar(100),"
            + "LAST_NAME varchar(100),"
            + "BORN DATETIME,"
            + "ORG varchar(30))");
        statement.close();
    }

    @Before
    public void importDataSet() throws Exception {
        dbConn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        createSchema();
        IDataSet dataSet = readDataSet();
        cleanlyInsert(dataSet);

        testOutput = new ByteArrayOutputStream();
        testWriter = new OutputStreamWriter(testOutput, "UTF-8");
        props = new Properties();
        props.setProperty("tables", "person events");
        props.setProperty("vocabulary", "http://voc");
        props.setProperty("datatype.integer", "xsd:integer");
        props.setProperty("datatype.decimal", "xsd:decimal");
        props.setProperty("datatype.timestamp", "xsd:dateTime");

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
        InputStream is = DatabaseTest.class.getClassLoader().getResourceAsStream("seed-person.xml");
        return new FlatXmlDataSetBuilder().build(is);
    }

    private void cleanlyInsert(IDataSet dataSet) throws Exception {
        IDatabaseTester databaseTester = new JdbcDatabaseTester(JDBC_DRIVER, JDBC_URL, USER, PASSWORD);
        databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        databaseTester.setDataSet(dataSet);
        databaseTester.onSetup();
    }

    private String loadFile(String fileName) throws Exception {
        InputStream is = DatabaseTest.class.getClassLoader().getResourceAsStream(fileName);
        return IOUtils.toString(is, "UTF-8");
    }

    @Test
    public void simplePersonExport() throws Exception {
        props.setProperty("person.query", "SELECT ID, NAME, LAST_NAME, BORN, ORG AS INORG FROM PERSON");
        props.setProperty("objectproperty.INORG", "orgs");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("person");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString();
        //System.out.println(actual);
        String expected = loadFile("rdf-person.xml");
        assertEquals(expected, actual);
    }

    @Test
    public void basePersonExport() throws Exception {
        props.setProperty("person.query", "SELECT ID, NAME, LAST_NAME, BORN, ORG AS INORG FROM PERSON");
        props.setProperty("objectproperty.INORG", "orgs");
        props.setProperty("baseurl", "http://base/url/");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("person");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString();
        String expected = loadFile("rdf-person-base.xml");
        assertEquals(expected, actual);
    }

    @Test
    public void simpleAttrExport() throws Exception {
        props.setProperty("notations.attributetable3", "SELECT 'NE' AS id"
            + ",'rdf:type','http://ontology/Notation','->',NULL "
            + ",'rdfs:label','Not estimated','','' "
            + ",'skos:notation','NE','','' "
            + ",'skos:prefLabel','Not estimated','','' ");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("notations");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString();
        String expected = loadFile("rdf-notations.xml");
        assertEquals(expected, actual);
    }

    @Test
    public void prtrPollutant() throws Exception {
        props.setProperty("pollutant.vocabulary", "http://prtr/");
        props.setProperty("pollutant.class", "prtr:Pollutant");
        props.setProperty("pollutant.query", "SELECT 'ICHLOROETHANE-1,2 (DCE)' AS ID, 'ICHLOROETHANE-1,2 (DCE)' AS CODE");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("pollutant");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString();
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns=\"http://prtr/\">\n"
            + "\n"
            + "<prtr:Pollutant rdf:about=\"#pollutant/ICHLOROETHANE-1,2%20(DCE)\">\n"
            + " <CODE>ICHLOROETHANE-1,2 (DCE)</CODE>\n"
            + "</prtr:Pollutant>\n"
            + "</rdf:RDF>\n";
        assertEquals(expected, actual);
    }

    @Test
    public void prtrElektra() throws Exception {
        props.setProperty("greek.vocabulary", "http://prtr/");
        props.setProperty("greek.class", "prtr:TMX");
        props.setProperty("greek.query", "SELECT 'Elektra' AS ID, 'Ηλέκτρα' AS name");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("greek");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString();
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns=\"http://prtr/\">\n"
            + "\n"
            + "<prtr:TMX rdf:about=\"#greek/Elektra\">\n"
            + " <NAME>Ηλέκτρα</NAME>\n"
            + "</prtr:TMX>\n"
            + "</rdf:RDF>\n";
        assertEquals(expected, actual);
    }

    /*
     * We have explicitly disallowed other encodings than UTF-8
     */
    @Test(expected = RuntimeException.class)
    public void prtrElektraISO() throws Exception {
        props.setProperty("greek.vocabulary", "http://prtr/");
        props.setProperty("greek.class", "TMX");
        props.setProperty("greek.query", "SELECT 'Elektra' AS ID, 'Ηλέκτρα' AS name");
        //FileOutputStream fOut = new FileOutputStream("greek-rdf.xml");
        testWriter = new OutputStreamWriter(testOutput, "ISO-8859-7");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("greek");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString();
        String expected = "<?xml version=\"1.0\" encoding=\"ISO8859_7\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns=\"http://prtr/\">\n"
            + "\n"
            + "<prtr:TMX rdf:about=\"#greek/Elektra\">\n"
            + " <NAME>Ηλέκτρα</NAME>\n"
            + "</prtr:TMX>\n"
            + "</rdf:RDF>\n";
        assertEquals(expected, actual);
    }

    /*
     * Test ExploreDB class
     */
    @Test
    public void explorePersonTable() throws Exception {
        ExploreDB edb = new ExploreDB(dbConn, props, false);
        edb.discoverTables(false);
        assertEquals("discovered tables", "PERSON ", props.getProperty("tables"));
        assertEquals("SELECT '' || id AS id, '' || id AS 'rdfs:label', `id` AS 'id->PERSON', `name` AS 'name', `last_name` AS 'last_name', `born` AS 'born', `org` AS 'org' FROM PERSON", props.getProperty("PERSON.query"));
        assertNull(props.getProperty("person.query"));
    }

    @Test
    public void explorePersonTableWithTypes() throws Exception {
        ExploreDB edb = new ExploreDB(dbConn, props, false);
        edb.discoverTables(true);
        assertEquals("discovered tables", "PERSON ", props.getProperty("tables"));
        assertEquals("SELECT '' || id AS id, '' || id AS 'rdfs:label', `id` AS 'id->PERSON', `name` AS 'name@', `last_name` AS 'last_name@', `born` AS 'born^^xsd:dateTime', `org` AS 'org@' FROM PERSON", props.getProperty("PERSON.query"));
        assertNull(props.getProperty("person.query"));
    }

}
