package eionet.rdfexport;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import org.junit.Test;

public class OptionParserTest {

    @Test
    public void plainArguments() {
        String[] args = {"-f", "argument1", "-o", "argumentO"};
        OptionParser op = new OptionParser(args, "o:f:");
        String optionO = op.getArgument("o");
        assertEquals("argumentO", optionO);
        assertNull(op.getArgument("x"));
    }

    @Test
    public void unusedArguments1() {
        String[] args = {"-x", "file1", "file2"};
        OptionParser op = new OptionParser(args, "xyz");
        assertEquals("", op.getArgument("x"));
        assertNull(op.getArgument("y"));
        assertNull(op.getArgument("z"));

        String[] unused = op.getUnusedArguments();
        assertEquals("file1", unused[0]);
        assertEquals("file2", unused[1]);
        assertEquals(2, unused.length);
    }

    @Test
    public void unknownPlainOption() {
        String[] args = {"-f", "-u", "-o", "argumentO"};
        OptionParser op = new OptionParser(args, "o:f");
        assertEquals("argumentO", op.getArgument("o"));
        // "u" is not on the list of options.
        assertNull(op.getArgument("u"));
    }

    @Test
    public void clusteredFlags() {
        String[] args = {"-xa", "-f", "F"};
        OptionParser op = new OptionParser(args, "f:ax");
        assertEquals("", op.getArgument("a"));
        assertEquals("", op.getArgument("x"));
        assertEquals("F", op.getArgument("f"));
    }

    @Test
    public void noArgumentsGiven() {
        String[] args = {};
        OptionParser op = new OptionParser(args, "f:ax");
        String[] unused = op.getUnusedArguments();
        assertEquals("No unused arguments", 0, unused.length);
    }

}
