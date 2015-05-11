package eionet.rdfexport;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import static junit.framework.Assert.assertEquals;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author George Sofianos
 */
@Ignore
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
        statement.executeUpdate("INSERT INTO `T_OBLIGATION` (`PK_RA_ID`, `FK_SOURCE_ID`, `VALID_SINCE`, `TITLE`, `FORMAT_NAME`, `REPORT_FORMAT_URL`, `REPORT_FREQ_DETAIL`, `REPORTING_FORMAT`, `DATE_COMMENTS`, `LAST_UPDATE`, `TERMINATE`, `REPORT_FREQ`, `COMMENT`, `RESPONSIBLE_ROLE`, `NEXT_DEADLINE`, `FIRST_REPORTING`, `REPORT_FREQ_MONTHS`, `NEXT_REPORTING`, `VALID_TO`, `FK_DELIVERY_COUNTRY_IDS`, `NEXT_DEADLINE2`, `RM_NEXT_UPDATE`, `RM_VERIFIED`, `RM_VERIFIED_BY`, `LAST_HARVESTED`, `LOCATION_PTR`, `LOCATION_INFO`, `LEGAL_MORAL`, `FK_CLIENT_ID`, `DESCRIPTION`, `RESPONSIBLE_ROLE_SUF`, `NATIONAL_CONTACT`, `NATIONAL_CONTACT_URL`, `COORDINATOR_ROLE`, `COORDINATOR_ROLE_SUF`, `COORDINATOR`, `COORDINATOR_URL`, `PARAMETERS`, `VALIDATED_BY`, `EEA_PRIMARY`, `EEA_CORE`, `FLAGGED`, `AUTHORITY`, `OVERLAP_URL`, `DPSIR_D`, `DPSIR_P`, `DPSIR_S`, `DPSIR_I`, `DPSIR_R`, `DATA_USED_FOR`, `DATA_USED_FOR_URL`, `CONTINOUS_REPORTING`) VALUES (670, 650, '2011-12-17', '(B) Information on zones and agglomerations (Article 6)', 'See Air Quality Portal', 'http://www.eionet.europa.eu/aqportal', NULL, NULL, NULL, '2014-02-20', 'N', NULL, NULL, NULL, '2015-09-30', '2014-09-30', 12, NULL, '9999-12-31', ',3,5,7,8,9,10,11,12,13,14,15,17,16,21,20,23,22,24,27,29,28,31,30,34,35,33,36,40,114,', '2016-09-30', '2014-09-30', '2013-11-28', 'EEA', '2014-11-21 14:14:14', 'http://cdr.eionet.europa.eu', 'Reportnet Central Data Repository', 'L', 104, 'Article 6\\r\\nZones and agglomerations\\r\\n1. In accordance with the procedure referred to in Article 5 of this Decision, Member States shall make available the information set out in Part B of Annex II to this Decision on the delimitation and type of zones and agglomerations established in accordance with Article 3 of Directive 2004/107/EC and Article 4 of Directive 2008/50/EC and in which the assessment and management of air quality is to be carried out in the following calendar year.\\r\\nFor zones and agglomerations to which an exemption or a postponement applies pursuant to Article 22 of Directive 2008/50/EC, the information made available shall include an indication thereof.\\r\\n2. Member States shall make the information referred to in paragraph 1 available to the Commission no later than 31 December of each calendar year. Member States may indicate that there have been no changes to the information previously made available.\\r\\n3. Where changes are made to the delimitation and type of zones and agglomerations, the Member States shall inform the Commission thereof no later than 9 months after the end of the calendar year the changes were made.', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0, 0, 0, NULL, NULL, 'no', 'no', 'no', 'no', 'yes', 'See Air Quality Portal', 'http://www.eionet.europa.eu/aqportal', 'no');");
        statement.close();
    }

    @Before
    public void importDataSet() throws Exception {
        dbConn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        createSchema();
        //IDataSet dataSet = readDataSet();
        //cleanlyInsert(dataSet);
        dbConn.setAutoCommit(false); // Emulate command line use

        testOutput = new ByteArrayOutputStream();
        testWriter = new OutputStreamWriter(testOutput, "UTF-8");
        props = new Properties();
        props.load(MySQLJSONTest.class.getClassLoader().getResourceAsStream("rdfexport.properties"));

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
        String actual = testOutput.toString(UTF8_ENCODING);
        //System.out.println(actual);
        String expected = loadFile("jsonld-obligation.json");
        assertEquals(expected, actual);
    }
}
