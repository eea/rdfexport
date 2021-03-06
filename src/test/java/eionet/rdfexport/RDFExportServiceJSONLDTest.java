package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.junit.Test;
import org.junit.Before;
import java.io.ByteArrayOutputStream;

/**
 * The RDFExportServiceJSONLD is really too simple to test.
 */
public class RDFExportServiceJSONLDTest {

    @Test
    public void instantiation() {
        ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
        RDFExportService s = new RDFExportServiceJSONLD(testOutput, null, null);
        assertNotNull("Expected constructor to work", s);
    }

}
