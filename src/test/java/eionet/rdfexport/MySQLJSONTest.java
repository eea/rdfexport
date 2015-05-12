package eionet.rdfexport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;

/**
 *
 * @author George Sofianos
 */

public class MySQLJSONTest {

    private static final String JDBC_DRIVER = com.mysql.jdbc.Driver.class.getName();
    private static final String JDBC_URL = "jdbc:mysql:mxj://localhost:3336/RDFTest"
        + "?createDatabaseIfNotExist=true"
        + "&server.initialize-user=true"
        + "&useUnicode=true"
        + "&characterEncoding=UTF-8";
    private static final String USER = "testuser";
    private static final String PASSWORD = "testpassword";
    private static final String UTF8_ENCODING = "UTF-8";

    private GenerateJSONLD classToTest;
    private ByteArrayOutputStream testOutput;
    private OutputStreamWriter testWriter;
    private Properties props;
    private Connection dbConn;

    private void createSchema() throws Exception {
       Statement statement = dbConn.createStatement();
        // MySQL table names are case-sensitive on Linux
        statement.executeUpdate("drop table if exists T_OBLIGATION");
        statement.executeUpdate("CREATE TABLE `T_OBLIGATION` ("
                + "`PK_RA_ID` INT(10) NOT NULL AUTO_INCREMENT,"
                + "`FK_SOURCE_ID` INT(10) UNSIGNED NOT NULL DEFAULT '0',"
                + "`VALID_SINCE` DATE NULL DEFAULT NULL,"
                + "`TITLE` VARCHAR(255) NOT NULL DEFAULT '',"
                + "`FORMAT_NAME` VARCHAR(100) NULL DEFAULT NULL,"
                + "`REPORT_FORMAT_URL` VARCHAR(255) NULL DEFAULT NULL,"
                + "`REPORT_FREQ_DETAIL` VARCHAR(50) NULL DEFAULT NULL,"
                + "`REPORTING_FORMAT` TEXT NULL,"
                + "`DATE_COMMENTS` VARCHAR(255) NULL DEFAULT NULL,"
                + "`LAST_UPDATE` DATE NULL DEFAULT NULL,"
                + "`TERMINATE` ENUM('Y','N') NOT NULL DEFAULT 'N',"
                + "`REPORT_FREQ` VARCHAR(25) NULL DEFAULT NULL,"
                + "`COMMENT` TEXT NULL,"
                + "`RESPONSIBLE_ROLE` VARCHAR(255) NULL DEFAULT NULL,"
                + "`NEXT_DEADLINE` DATE NULL DEFAULT NULL,"
                + "`FIRST_REPORTING` DATE NULL DEFAULT NULL,"
                + "`REPORT_FREQ_MONTHS` INT(3) NULL DEFAULT NULL,"
                + "`NEXT_REPORTING` VARCHAR(255) NULL DEFAULT NULL,"
                + "`VALID_TO` DATE NULL DEFAULT NULL,"
                + "`FK_DELIVERY_COUNTRY_IDS` VARCHAR(255) NULL DEFAULT NULL,"
                + "`NEXT_DEADLINE2` DATE NULL DEFAULT NULL,"
                + "`RM_NEXT_UPDATE` DATE NULL DEFAULT NULL,"
                + "`RM_VERIFIED` DATE NULL DEFAULT NULL,"
                + "`RM_VERIFIED_BY` VARCHAR(50) NULL DEFAULT NULL,"
                + "`LAST_HARVESTED` DATETIME NULL DEFAULT NULL,"
                + "`LOCATION_PTR` VARCHAR(255) NULL DEFAULT NULL,"
                + "`LOCATION_INFO` VARCHAR(255) NULL DEFAULT NULL,"
                + "`LEGAL_MORAL` ENUM('L','M','V') NULL DEFAULT 'L',"
                + "`FK_CLIENT_ID` INT(10) NULL DEFAULT NULL,"
                + "`DESCRIPTION` TEXT NULL,"
                + "`RESPONSIBLE_ROLE_SUF` INT(1) NOT NULL DEFAULT '1',"
                + "`NATIONAL_CONTACT` VARCHAR(255) NULL DEFAULT NULL,"
                + "`NATIONAL_CONTACT_URL` VARCHAR(255) NULL DEFAULT NULL,"
                + "`COORDINATOR_ROLE` VARCHAR(255) NULL DEFAULT NULL,"
                + "`COORDINATOR_ROLE_SUF` INT(1) NOT NULL DEFAULT '1',"
                + "`COORDINATOR` VARCHAR(255) NULL DEFAULT NULL,"
                + "`COORDINATOR_URL` VARCHAR(255) NULL DEFAULT NULL,"
                + "`PARAMETERS` TEXT NULL,"
                + "`VALIDATED_BY` TEXT NULL,"
                + "`EEA_PRIMARY` INT(1) NULL DEFAULT '0',"
                + "`EEA_CORE` INT(1) NULL DEFAULT '0',"
                + "`FLAGGED` INT(1) NULL DEFAULT '0',"
                + "`AUTHORITY` VARCHAR(255) NULL DEFAULT NULL,"
                + "`OVERLAP_URL` VARCHAR(255) NULL DEFAULT NULL,"
                + "`DPSIR_D` ENUM('yes','no') NULL DEFAULT NULL,"
                + "`DPSIR_P` ENUM('yes','no') NULL DEFAULT NULL,"
                + "`DPSIR_S` ENUM('yes','no') NULL DEFAULT NULL,"
                + "`DPSIR_I` ENUM('yes','no') NULL DEFAULT NULL,"
                + "`DPSIR_R` ENUM('yes','no') NULL DEFAULT NULL,"
                + "`DATA_USED_FOR` VARCHAR(255) NULL DEFAULT NULL,"
                + "`DATA_USED_FOR_URL` VARCHAR(255) NULL DEFAULT NULL,"
                + "`CONTINOUS_REPORTING` ENUM('yes','no') NULL DEFAULT NULL,"
                + "PRIMARY KEY (`PK_RA_ID`),"
                + "INDEX `FK_SOURCE_ID` (`FK_SOURCE_ID`)) default charset utf8");
        statement.executeUpdate("INSERT INTO `T_OBLIGATION` (`PK_RA_ID`, `FK_SOURCE_ID`, `VALID_SINCE`, `TITLE`, `FORMAT_NAME`, `REPORT_FORMAT_URL`, `REPORT_FREQ_DETAIL`, `REPORTING_FORMAT`, `DATE_COMMENTS`, `LAST_UPDATE`, `TERMINATE`, `REPORT_FREQ`, `COMMENT`, `RESPONSIBLE_ROLE`, `NEXT_DEADLINE`, `FIRST_REPORTING`, `REPORT_FREQ_MONTHS`, `NEXT_REPORTING`, `VALID_TO`, `FK_DELIVERY_COUNTRY_IDS`, `NEXT_DEADLINE2`, `RM_NEXT_UPDATE`, `RM_VERIFIED`, `RM_VERIFIED_BY`, `LAST_HARVESTED`, `LOCATION_PTR`, `LOCATION_INFO`, `LEGAL_MORAL`, `FK_CLIENT_ID`, `DESCRIPTION`, `RESPONSIBLE_ROLE_SUF`, `NATIONAL_CONTACT`, `NATIONAL_CONTACT_URL`, `COORDINATOR_ROLE`, `COORDINATOR_ROLE_SUF`, `COORDINATOR`, `COORDINATOR_URL`, `PARAMETERS`, `VALIDATED_BY`, `EEA_PRIMARY`, `EEA_CORE`, `FLAGGED`, `AUTHORITY`, `OVERLAP_URL`, `DPSIR_D`, `DPSIR_P`, `DPSIR_S`, `DPSIR_I`, `DPSIR_R`, `DATA_USED_FOR`, `DATA_USED_FOR_URL`, `CONTINOUS_REPORTING`) VALUES (670, 650, '2011-12-17', '(B) Information on zones and agglomerations (Article 6)', 'See Air Quality Portal', 'http://www.eionet.europa.eu/aqportal', NULL, NULL, NULL, '2014-02-20', 'N', NULL, NULL, NULL, '2015-09-30', '2014-09-30', 12, NULL, '9999-12-31', ',3,5,7,8,9,10,11,12,13,14,15,17,16,21,20,23,22,24,27,29,28,31,30,34,35,33,36,40,114,', '2016-09-30', '2014-09-30', '2013-11-28', 'EEA', '2014-11-21 14:14:14', 'http://cdr.eionet.europa.eu', 'Reportnet Central Data Repository', 'L', 104, 'Article 6\r\nZones and agglomerations\r\n1. In accordance with the procedure referred to in Article 5 of this Decision, Member States shall make available the information set out in Part B of Annex II to this Decision on the delimitation and type of zones and agglomerations established in accordance with Article 3 of Directive 2004/107/EC and Article 4 of Directive 2008/50/EC and in which the assessment and management of air quality is to be carried out in the following calendar year.\r\nFor zones and agglomerations to which an exemption or a postponement applies pursuant to Article 22 of Directive 2008/50/EC, the information made available shall include an indication thereof.\r\n2. Member States shall make the information referred to in paragraph 1 available to the Commission no later than 31 December of each calendar year. Member States may indicate that there have been no changes to the information previously made available.\r\n3. Where changes are made to the delimitation and type of zones and agglomerations, the Member States shall inform the Commission thereof no later than 9 months after the end of the calendar year the changes were made.', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0, 0, 0, NULL, NULL, 'no', 'no', 'no', 'no', 'yes', 'See Air Quality Portal', 'http://www.eionet.europa.eu/aqportal', 'no');");
        statement.close();
    }

    @Before
    public void importDataSet() throws Exception {
        dbConn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        createSchema();
        dbConn.setAutoCommit(false); // Emulate command line use

        testOutput = new ByteArrayOutputStream();
        testWriter = new OutputStreamWriter(testOutput, "UTF-8");
        props = new Properties();
        props.setProperty("datatype.int","xsd:integer");
        props.setProperty("datatype.datetime", "xsd:dateTime");
        props.setProperty("datatype.decimal", "xsd:double");
        props.setProperty("datatype.float", "xsd:double");
        props.setProperty("datatype.real", "xsd:double");
        props.setProperty("baseurl","http://rod.eionet.europa.eu/");
        props.setProperty("vocabulary","http://rod.eionet.europa.eu/schema.rdf#");
        props.setProperty("tables","obligations");
        props.setProperty("obligations.class", "Obligation");
        props.setProperty("obligations.query1", "SELECT PK_RA_ID AS id, CAST(PK_RA_ID AS BINARY) AS 'skos:notation', FK_SOURCE_ID AS instrument," +
        "VALID_SINCE AS 'dcterms:valid', TITLE AS 'dcterms:title', TITLE AS 'rdfs:label', REPORTING_FORMAT AS 'guidelines', COMMENT AS 'comment'," +
        "NULLIF(RESPONSIBLE_ROLE, '') AS 'responsibleRole', NEXT_DEADLINE AS 'nextdeadline', NEXT_DEADLINE2 AS 'nextdeadline2', LAST_UPDATE AS lastUpdate," +
        "IF(TERMINATE='Y','true','false') AS 'isTerminated^^xsd:boolean', VALID_SINCE AS validSince, RM_NEXT_UPDATE AS nextUpdate, RM_VERIFIED AS verified," +                   "RM_VERIFIED_BY AS verifiedBy, LAST_HARVESTED AS lastHarvested, FK_CLIENT_ID AS 'requester->clients', DESCRIPTION AS 'dcterms:abstract'," +
        "NULLIF(COORDINATOR, '') AS 'coordinator', NULLIF(COORDINATOR_URL, '') AS 'coordinatorUrl->', NULLIF(LOCATION_PTR, '') AS 'primaryRepository->'," +
        "NULLIF(REPORT_FREQ_DETAIL, '') AS reportingFrequencyDetail, REPORT_FREQ_MONTHS AS reportingFrequencyMonths, REPORT_FREQ AS reportingFrequency," +                       "VALIDATED_BY AS validatedBy, IF(EEA_PRIMARY=1,'true','false') AS 'isEEAPrimary^^xsd:boolean', IF(EEA_CORE=1,'true','false') AS 'isEEACore^^xsd:boolean'," +
        "IF(FLAGGED=1,'true','false') AS 'isFlagged^^xsd:boolean', IF(DPSIR_D='yes','http://dd.eionet.europa.eu/vocabulary/common/dpsir/D',NULL) AS 'dpsirCategory->'," +
        "IF(DPSIR_P='yes','http://dd.eionet.europa.eu/vocabulary/common/dpsir/P',NULL) AS 'dpsirCategory->'," +
        "IF(DPSIR_S='yes','http://dd.eionet.europa.eu/vocabulary/common/dpsir/S',NULL) AS 'dpsirCategory->'," +
        "IF(DPSIR_I='yes','http://dd.eionet.europa.eu/vocabulary/common/dpsir/I',NULL) AS 'dpsirCategory->'," +
        "IF(DPSIR_R='yes','http://dd.eionet.europa.eu/vocabulary/common/dpsir/R',NULL) AS 'dpsirCategory->'," +
        "NULLIF(DATA_USED_FOR_URL, '') AS 'dataUsedFor->', IF(CONTINOUS_REPORTING='yes','true','false') AS 'continuousReporting^^xsd:boolean' FROM T_OBLIGATION");
        props.setProperty("xmlns.rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        props.setProperty("xmlns.rdfs","http://www.w3.org/2000/01/rdf-schema#");
        props.setProperty("xmlns.geo","http://www.w3.org/2003/01/geo/wgs84_pos#");
        props.setProperty("xmlns.owl","http://www.w3.org/2002/07/owl#");
        props.setProperty("xmlns.foaf","http://xmlns.com/foaf/0.1/");
        props.setProperty("xmlns.sioc","http://rdfs.org/sioc/ns#");
        props.setProperty("xmlns.dcterms","http://purl.org/dc/terms/");
        props.setProperty("xmlns.skos","http://www.w3.org/2004/02/skos/core#");
        props.setProperty("xmlns.xsd","http://www.w3.org/2001/XMLSchema#");
        props.setProperty("xmlns.cc","http://creativecommons.org/ns#");

        assertEquals("MySQL database expected", "mysql", ExploreDB.getDBProductName(dbConn));
        props.setProperty("sqldialect.access.skiptables",
             "VALIDATION_METADATA_DO_NOT_MODIFY" // DataDict reserved table
            + " MSYSACCESSOBJECTS MSYSACCESSXML MSYSACES MSYSOBJECTS MSYSQUERIES MSYSRELATIONSHIPS");
        props.setProperty("sqldialect.mysql.column.before", "`");
        props.setProperty("sqldialect.mysql.column.after", "`");
        props.setProperty("sqldialect.mysql.alias.before", "'");
        props.setProperty("sqldialect.mysql.alias.after", "'");
        props.setProperty("sqldialect.mysql.concat", "concat");
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
    @AfterClass
    public static void cleanup() throws SQLException {
        Connection dbConn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        Statement statement = dbConn.createStatement();
        // MySQL table names are case-sensitive on Linux
        statement.executeUpdate("drop table if exists T_OBLIGATION");
    }

    private String loadFile(String fileName) throws Exception {
        InputStream is = MySQLJSONTest.class.getClassLoader().getResourceAsStream(fileName);
        return IOUtils.toString(is, "UTF-8");
    }

    /**
    * Test Generation of JSONLD file.
    *
    */
    @Test
    public void obligationJSONLDExport() throws Exception {
        classToTest = new GenerateJSONLD(testWriter, dbConn, props);
        classToTest.exportTable("obligations", "670");
        classToTest.exportDocumentInformation();
        classToTest.writeJsonLDFooter();
        String jsonldObject = testOutput.toString(UTF8_ENCODING);
        String actual;
        String expected;
        Boolean actualBool;
        Boolean expectedBool;

        Map<String,String> expectedContext = new LinkedHashMap<String, String>();
        expectedContext.put("sioc","http://rdfs.org/sioc/ns#");
        expectedContext.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
        expectedContext.put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
        expectedContext.put("foaf", "http://xmlns.com/foaf/0.1/");
        expectedContext.put("owl", "http://www.w3.org/2002/07/owl#");
        expectedContext.put("xsd", "http://www.w3.org/2001/XMLSchema#");
        expectedContext.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        expectedContext.put("skos", "http://www.w3.org/2004/02/skos/core#");
        expectedContext.put("cc", "http://creativecommons.org/ns#");
        expectedContext.put("dcterms", "http://purl.org/dc/terms/");
        expectedContext.put("@base", "http://rod.eionet.europa.eu/");
        expectedContext.put("@vocab", "http://rod.eionet.europa.eu/schema.rdf#");

        JsonElement jelem = new JsonParser().parse(jsonldObject);
        JsonObject jobj = jelem.getAsJsonObject();
        JsonObject context = jobj.getAsJsonObject("@context");
        for (Map.Entry<String,JsonElement> st : context.entrySet()) {
          actual = st.getValue().getAsString();
          expected = expectedContext.get(st.getKey());
          assertEquals(expected,actual);
        }

        //Test @id Field
        actual = jobj.get("@id").getAsString();
        expected = "obligations/670";
        assertEquals(expected,actual);

        //Test @type Field
        actual = jobj.get("@type").getAsString();
        expected = "Obligation";
        assertEquals(expected,actual);

        //Test if json object has some key/value pairs
        actualBool = jobj.has("instrument");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("dcterms:valid");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("dcterms:title");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("rdfs:label");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("nextdeadline");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("nextdeadline2");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("lastUpdate");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("isTerminated");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("validSince");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("nextUpdate");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);
        actualBool = jobj.has("isFlagged");
        expectedBool = true;
        assertEquals(expectedBool,actualBool);

        //Test if json has a valid date value
        JsonObject validSince = jobj.getAsJsonObject("validSince");
        actual = validSince.get("@type").getAsString();
        expected = "xsd:date";
        assertEquals(expected,actual);
        actual = validSince.get("@value").getAsString();
        expected = "2011-12-17";
        assertEquals(expected,actual);

        //Test if json has a valid datetime value
        JsonObject lastHarvested = jobj.getAsJsonObject("lastHarvested");
        actual = lastHarvested.get("@type").getAsString();
        expected = "xsd:dateTime";
        assertEquals(expected,actual);
        actual = lastHarvested.get("@value").getAsString();
        expected = "2014-11-21T14:14:14";
        assertEquals(expected,actual);
    }
}
