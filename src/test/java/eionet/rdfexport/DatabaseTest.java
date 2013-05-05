package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.operation.DatabaseOperation;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class DatabaseTest {

    //final Logger logger = LoggerFactory.getLogger(DatabaseTest.class);

    private static final String JDBC_DRIVER = org.h2.Driver.class.getName();
    private static final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static final String UTF8 = null;

    private GenerateRDF classToTest;
    private ByteArrayOutputStream testOutput;
    private Properties props;
    private Connection dbConn;

    @BeforeClass
    public static void createSchema() throws Exception {
        String schemaPath = DatabaseTest.class.getClassLoader().getResource("schema.sql").getFile();
        RunScript.execute(JDBC_URL, USER, PASSWORD, schemaPath, UTF8, false);
    }

    @Before
    public void importDataSet() throws Exception {
        IDataSet dataSet = readDataSet();
        cleanlyInsert(dataSet);

        testOutput = new ByteArrayOutputStream();
        props = new Properties();
        props.setProperty("tables", "person events");
        props.setProperty("vocabulary", "http://voc");
        props.setProperty("datatype.integer", "xsd:integer");
        props.setProperty("datatype.decimal", "xsd:decimal");
        props.setProperty("datatype.timestamp","xsd:dateTime");
        props.setProperty("objectproperty.INORG", "orgs");
        props.setProperty("person.query", "SELECT ID, NAME, LAST_NAME, BORN, ORG AS INORG FROM person");
        props.setProperty("notations.attributetable3","SELECT 'NE' AS id"
         + ",'rdf:type','http://ontology/Notation','->',NULL "
         + ",'rdfs:label','Not estimated','','' "
         + ",'skos:notation','NE','','' "
         + ",'skos:prefLabel','Not estimated','','' ");

        dbConn = dataSource().getConnection();
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

    private DataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(JDBC_URL);
        dataSource.setUser(USER);
        dataSource.setPassword(PASSWORD);
        return dataSource;
    }

    private String loadFile(String fileName) throws Exception {
        InputStream is = DatabaseTest.class.getClassLoader().getResourceAsStream(fileName);
        return IOUtils.toString(is, "UTF-8");
    }

    @Test
    public void simplePersonExport() throws Exception {
        classToTest = new GenerateRDF(testOutput, dbConn, props);
        classToTest.exportTable("person");
        String actual = testOutput.toString();
        //System.out.println(actual);
        String expected = loadFile("rdf-person.xml");
        assertEquals(expected, actual);
    }

    @Test
    public void basePersonExport() throws Exception {
        props.setProperty("baseurl", "http://base/url/");
        classToTest = new GenerateRDF(testOutput, dbConn, props);
        classToTest.exportTable("person");
        String actual = testOutput.toString();
        String expected = loadFile("rdf-person-base.xml");
        assertEquals(expected, actual);
    }

    @Test
    public void simpleAttrExport() throws Exception {
        classToTest = new GenerateRDF(testOutput, dbConn, props);
        classToTest.exportTable("notations");
        String actual = testOutput.toString();
        String expected = loadFile("rdf-notations.xml");
        assertEquals(expected, actual);
    }

    @Test
    public void prtrPollutant() throws Exception {
        props.setProperty("pollutant.vocabulary", "http://prtr/");
        props.setProperty("pollutant.class", "prtr:Pollutant");
        props.setProperty("pollutant.query", "SELECT 'ICHLOROETHANE-1,2 (DCE)' AS ID, 'ICHLOROETHANE-1,2 (DCE)' AS CODE");
        classToTest = new GenerateRDF(testOutput, dbConn, props);
        classToTest.exportTable("pollutant");
        String actual = testOutput.toString();
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns=\"http://prtr/\">\n"
            + "\n"
            + "<prtr:Pollutant rdf:about=\"#pollutant/ICHLOROETHANE-1,2%20(DCE)\">\n"
            + " <CODE>ICHLOROETHANE-1,2 (DCE)</CODE>\n"
            + "</prtr:Pollutant>\n";
        assertEquals(expected, actual);
    }
}
