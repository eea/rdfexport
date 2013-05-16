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
        testWriter = new OutputStreamWriter(testOutput, UTF8_ENCODING);
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
        return IOUtils.toString(is, UTF8_ENCODING);
    }

    @Test
    public void simplePersonExport() throws Exception {
        props.setProperty("person.query", "SELECT ID, NAME, LAST_NAME, BORN, ORG AS INORG FROM PERSON ORDER BY ID");
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
        props.setProperty("person.query", "SELECT ID, NAME, LAST_NAME, BORN, ORG AS INORG FROM PERSON ORDER BY ID");
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
    public void personAtQuery() throws Exception {
        props.setProperty("person.query", "SELECT '@' AS ID, NAME, LAST_NAME, BORN, ORG FROM PERSON ORDER BY BORN");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("person");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = loadFile("rdf-person-atsign.xml");
        assertEquals(expected, actual);
    }

    @Test
    public void fullDocumentInformation() throws Exception {
        props.setProperty("baseurl", "http://base/url/");
        props.setProperty("class", "bibo:Document");
        props.setProperty("query", "SELECT NULL AS ID, 'Ηλέκτρα' AS \"dcterms:creator@\", 'http://license.eu' AS \"cc:licence->\"");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportDocumentInformation();
        classToTest.writeRdfFooter();
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns=\"http://voc\" xml:base=\"http://base/url/\">\n"
            + "\n"
            + "<bibo:Document rdf:about=\"\">\n"
            + " <dcterms:creator>Ηλέκτρα</dcterms:creator>\n"
            + " <cc:licence rdf:resource=\"http://license.eu\"/>\n"
            + "</bibo:Document>\n"
            + "</rdf:RDF>\n";
        assertEquals(expected, actual);
    }

    /*
     * If the 'class' property is spelled 'CLASS' then it has no effect.
     */
    @Test
    public void documentInformationWithCLASS() throws Exception {
        props.setProperty("baseurl", "http://base/url/");
        props.setProperty("CLASS", "bibo:Document");
        props.setProperty("query", "SELECT NULL AS ID, 'Ηλέκτρα' AS \"dcterms:creator@\", 'http://license.eu' AS \"cc:licence->\"");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportDocumentInformation();
        classToTest.writeRdfFooter();
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns=\"http://voc\" xml:base=\"http://base/url/\">\n"
            + "\n"
            + "<rdf:Description rdf:about=\"\">\n"
            + " <dcterms:creator>Ηλέκτρα</dcterms:creator>\n"
            + " <cc:licence rdf:resource=\"http://license.eu\"/>\n"
            + "</rdf:Description>\n"
            + "</rdf:RDF>\n";
        assertEquals(expected, actual);
    }

    /*
     * It is possible to use attribute instead of 'query'
     */
    @Test
    public void documentInformationWithAttribute() throws Exception {
        props.setProperty("baseurl", "http://base/url/");
        props.setProperty("query", "SELECT NULL AS ID, 'http://license.eu' AS \"cc:licence->\"");
        props.setProperty("attributetable", "SELECT NULL AS ID"
            + ",'dcterms:creator',NAME, '', ''"
            + "FROM PERSON ORDER BY ID");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportDocumentInformation();
        classToTest.writeRdfFooter();
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns=\"http://voc\" xml:base=\"http://base/url/\">\n"
            + "\n"
            + "<rdf:Description rdf:about=\"\">\n"
            + " <cc:licence rdf:resource=\"http://license.eu\"/>\n"
            + "</rdf:Description>\n"
            + "<rdf:Description rdf:about=\"\">\n"
            + " <dcterms:creator>Ηλέκτρα</dcterms:creator>\n"
            + " <dcterms:creator>Alice</dcterms:creator>\n"
            + " <dcterms:creator>Charlie</dcterms:creator>\n"
            + "</rdf:Description>\n"
            + "</rdf:RDF>\n";
        assertEquals(expected, actual);
    }
    @Test
    public void simpleAttr() throws Exception {
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
    public void simpleAttrWithAt() throws Exception {
        props.setProperty("orgs.attributetable1", "SELECT DISTINCT '@' AS id"
            + ",'rdf:type','http://ontology/Org','->',NULL "
            + ",'skos:notation',ORG,'','' FROM PERSON ORDER BY ORG");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("orgs");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString(UTF8_ENCODING);
        //System.out.println(actual);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns=\"http://voc\">\n"
            + "\n"
            + "<Orgs rdf:about=\"#orgs/1\">\n"
            + " <rdf:type rdf:resource=\"http://ontology/Org\"/>\n"
            + " <skos:notation>mafia +</skos:notation>\n"
            + "</Orgs>\n"
            + "<Orgs rdf:about=\"#orgs/2\">\n"
            + " <rdf:type rdf:resource=\"http://ontology/Org\"/>\n"
            + " <skos:notation>spectre</skos:notation>\n"
            + "</Orgs>\n"
            + "<Orgs rdf:about=\"#orgs/3\">\n"
            + " <rdf:type rdf:resource=\"http://ontology/Org\"/>\n"
            + " <skos:notation>yakuza</skos:notation>\n"
            + "</Orgs>\n"
            + "</rdf:RDF>\n";
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

    @Test
    public void prtrElektra() throws Exception {
        props.setProperty("greek.vocabulary", "http://prtr/");
        props.setProperty("greek.class", "prtr:TMX");
        props.setProperty("greek.query", "SELECT 'Elektra' AS ID, 'Ηλέκτρα' AS name");
        classToTest = new GenerateRDF(testWriter, dbConn, props);
        classToTest.exportTable("greek");
        classToTest.writeRdfFooter();
        String actual = testOutput.toString(UTF8_ENCODING);
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
        String actual = testOutput.toString(UTF8_ENCODING);
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
        assertEquals("SELECT '' || id AS id, '' || id AS 'rdfs:label', `id` AS 'id', `name` AS 'name', `last_name` AS 'last_name', `born` AS 'born', `org` AS 'org' FROM PERSON", props.getProperty("PERSON.query"));
        assertNull(props.getProperty("person.query"));
    }

    @Test
    public void explorePersonTableWithTypes() throws Exception {
        ExploreDB edb = new ExploreDB(dbConn, props, false);
        edb.discoverTables(true);
        assertEquals("discovered tables", "PERSON ", props.getProperty("tables"));
        assertEquals("SELECT '' || id AS id, '' || id AS 'rdfs:label', `id` AS 'id^^xsd:integer', `name` AS 'name@', `last_name` AS 'last_name@', `born` AS 'born^^xsd:dateTime', `org` AS 'org@' FROM PERSON", props.getProperty("PERSON.query"));
        assertNull(props.getProperty("person.query"));
    }

    /*
     * Force a timestamp to be exported as xsd:integer via the properties file.
     */
    @Test
    public void explorePersonTableWithBadTypes() throws Exception {
        props.setProperty("datatype.timestamp", "xsd:integer");
        ExploreDB edb = new ExploreDB(dbConn, props, false);
        edb.discoverTables(true);
        assertEquals("discovered tables", "PERSON ", props.getProperty("tables"));
        assertEquals("SELECT '' || id AS id, '' || id AS 'rdfs:label', `id` AS 'id^^xsd:integer', `name` AS 'name@', `last_name` AS 'last_name@', `born` AS 'born^^xsd:integer', `org` AS 'org@' FROM PERSON", props.getProperty("PERSON.query"));
        assertNull(props.getProperty("person.query"));
    }
}
