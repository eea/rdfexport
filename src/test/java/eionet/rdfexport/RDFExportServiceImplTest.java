package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.junit.Test;
import org.junit.Before;
import java.io.ByteArrayOutputStream;

/**
 * The RDFExportServiceImpl is really too simple to test.
 */
public class RDFExportServiceImplTest {

    @Test
    public void instantiation() {
        ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
        RDFExportServiceImpl s = new RDFExportServiceImpl(testOutput, null, null);
        assertNotNull("Expected constructor to work", s);
    }

}
