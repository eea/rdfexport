package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

public class MySQLTest {

    private static final String JDBC_DRIVER = com.mysql.jdbc.Driver.class.getName();
    private static final String JDBC_URL = "jdbc:mysql:mxj://localhost:3336/RDFTest"
        + "?createDatabaseIfNotExist=true"
        + "&server.initialize-user=true"
        + "&useUnicode=true"
        + "&characterEncoding=UTF-8";
    private static final String USER = "testuser";
    private static final String PASSWORD = "testpassword";
    private static final String UTF8_ENCODING = "UTF-8";

    private GenerateRDF classToTest;
    private ByteArrayOutputStream testOutput;
    private OutputStreamWriter testWriter;
    private Properties props;
    private Connection dbConn;

    private void createSchema() throws Exception {
        Statement statement = dbConn.createStatement();
        // MySQL table names are case-sensitive on Linux
        statement.executeUpdate("drop table if exists PERSON");
        statement.executeUpdate("create table PERSON ("
            + "ID integer primary key,"
            + "NAME varchar(100),"
            + "LAST_NAME varchar(100),"
            + "BORN DATETIME,"
            + "ORG varchar(30)) default charset utf8");
        statement.close();
    }

    @BeforeClass
    public static void loadDriver() throws Exception {
        //Apparently that JDBC_DRIVER trick above loads the driver also
        //Class.forName("com.mysql.jdbc.Driver");
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
        props.setProperty("datatype.datetime", "xsd:dateTime");

        assertEquals("MySQL database expected", "mysql", ExploreDB.getDBProductName(dbConn));
        props.setProperty("sqldialect.access.skiptables",
             "VALIDATION_METADATA_DO_NOT_MODIFY" // DataDict reserved table
            + " MSYSACCESSOBJECTS MSYSACCESSXML MSYSACES MSYSOBJECTS MSYSQUERIES MSYSRELATIONSHIPS");
        props.setProperty("sqldialect.mysql.column.before", "'");
        props.setProperty("sqldialect.mysql.column.after", "'");
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
        InputStream is = MySQLTest.class.getClassLoader().getResourceAsStream("seed-person.xml");
        return new FlatXmlDataSetBuilder().build(is);
    }

    private void cleanlyInsert(IDataSet dataSet) throws Exception {
        IDatabaseTester databaseTester = new JdbcDatabaseTester(JDBC_DRIVER, JDBC_URL, USER, PASSWORD);
        databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        databaseTester.setDataSet(dataSet);
        databaseTester.onSetup();
    }

    private String loadFile(String fileName) throws Exception {
        InputStream is = MySQLTest.class.getClassLoader().getResourceAsStream(fileName);
        return IOUtils.toString(is, "UTF-8");
    }

    @Test
    public void simplePersonExport() throws Exception {
        props.setProperty("person.query", "SELECT ID, NAME, LAST_NAME, BORN, ORG AS INORG FROM PERSON");
        props.setProperty("objectproperty.INORG", "orgs");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("person");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString(UTF8_ENCODING);
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
        String actual = testOutput.toString(UTF8_ENCODING);
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
        String actual = testOutput.toString(UTF8_ENCODING);
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
        String actual = testOutput.toString(UTF8_ENCODING);
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

    /*
     * Test ExploreDB class.
     * MySQL on windows changes the table names to lower case, where as Linux is case-sensitive.
     */
    @Test
    public void explorePersonTable() throws Exception {
        ExploreDB edb = new ExploreDB(dbConn, props, false);
        edb.discoverTables(false);
        String foundTable = props.getProperty("tables").trim();
        assertEquals("discovered tables", "person", foundTable.toLowerCase());
        String expected = "SELECT concat('', id) AS id, concat('', id) AS 'rdfs:label',"
            + " `id` AS 'id->PERSON', `name` AS 'name', `last_name` AS 'last_name',"
            + " `born` AS 'born', `org` AS 'org' FROM PERSON";
        String actual = props.getProperty(foundTable + ".query");
        assertEquals(expected.toLowerCase(), actual.toLowerCase());
    }

    /*
     * Test ExploreDB class.
     * MySQL on windows changes the table names to lower case, where as Linux is case-sensitive.
     */
    @Test
    public void explorePersonTableWithTypes() throws Exception {
        ExploreDB edb = new ExploreDB(dbConn, props, false);
        edb.discoverTables(true);
        String foundTable = props.getProperty("tables").trim();
        assertEquals("discovered tables", "person", foundTable.toLowerCase());
        String expected = "SELECT concat('', id) AS id, concat('', id) AS 'rdfs:label',"
            + " `id` AS 'id->PERSON', `name` AS 'name@', `last_name` AS 'last_name@',"
            + " `born` AS 'born^^xsd:dateTime', `org` AS 'org@' FROM PERSON";
        String actual = props.getProperty(foundTable + ".query");
        assertEquals(expected.toLowerCase(), actual.toLowerCase());
    }

    /*
     * Test Execute class
     */
    /**
     * Test simple query. This can only by done with a disk-stored database, as the Execute class
     * opens a new connection to the database.
     */
    @Test
    public void executeSimple() throws Exception {
        String resourcefile = MySQLTest.class.getClassLoader().getResource("exportsimple.properties").getFile();
        File temp = File.createTempFile("outsimple", ".rdf");
        String outfile = temp.toString();
        String[] args = {"-f", resourcefile, "-o", outfile};
        Execute.main(args);
        String expected = loadFile("rdf-person.xml");
        String actual = IOUtils.toString(temp.toURI(), "UTF-8");
        temp.delete();
        assertEquals(expected, actual);
    }

}
