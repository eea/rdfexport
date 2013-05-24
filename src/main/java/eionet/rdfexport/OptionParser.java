/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is RDFExport 1.0
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Søren Roug, EEA
 */
package eionet.rdfexport;

import java.util.HashMap;
import java.util.ArrayList;

/**
 * Parsing command line arguments passed to programs.
 * To use OptionParser, create an OptionParser object with a argv array passed
 * to the constructor and an option string. This is a string containing
 * the legitimate option characters.  If such a character is followed by
 * a colon, the option requires an argument.
 * TODO: Raise an exception of it encounters an unknown option
 */
public class OptionParser {

    /** A map of the options that require an argument. */
    private HashMap<String, Boolean> argExpectations;

    /** The parsed options. */
    private HashMap<String, String> parsedOptions;

    /** List of unrecognized command line arguments. */
    private ArrayList<String> unusedArguments = new ArrayList<String>();

    /**
     * Constructor.
     *
     * @param args
     *            - The arguments from the command line
     * @param optString
     *            - The list of options
     */
    public OptionParser(String[] args, String optString) {

        parsedOptions = new HashMap<String, String>();
        createArgMap(optString);
        parseArguments(args);
    }

    /**
     * Fill the map of a options that require an argument.
     * @param optString - The option string
     */
    private void createArgMap(String optString) {
        argExpectations = new HashMap<String, Boolean>();

        for (int i = 0; i < optString.length(); i++) {
            if (i + 1 < optString.length() && optString.charAt(i + 1) == ':') {
               argExpectations.put(String.valueOf(optString.charAt(i)), true);
            } else {
               argExpectations.put(String.valueOf(optString.charAt(i)), false);
            }
        }
    }

    /**
     * Determine if the option requires a value.
     *
     * @param option - name of option to look up
     * @return true if it needs a value and false if not or the option is unknown.
     */
    private boolean optionNeedsValue(String option) {
        return argExpectations.get(option);
    }

    /**
     * Determine if the option is known.
     *
     * @param arg - value to check if it an option
     * @return true if it is known.
     */
    private boolean isKnownOption(String arg) {
        return arg.startsWith("-") && arg.length() > 1
                && argExpectations.get(arg.substring(1, 2)) != null;
    }

    /**
     * Parse the arguments.
     * TODO: Doesn't check the following flags in a cluster to see if they are known options.
     *
     * @param args
     *            - The arguments from the command line
     */
    private void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (isKnownOption(arg)) {
                String option = String.valueOf(arg.charAt(1));
                if (optionNeedsValue(option)) {
                    if (arg.length() > 2) {
                        parsedOptions.put(option, arg.substring(2));
                    } else {
                        parsedOptions.put(option, args[++i]);
                    }
                } else {
                    for (int clusterInx = 1; clusterInx < arg.length(); clusterInx++) {
                        parsedOptions.put(String.valueOf(arg.charAt(clusterInx)), "");
                    }
                }
            } else {
                unusedArguments.add(arg);
            }
        }
    }

    /**
     * Return the argument for an option. If the option wasn't seen in the
     * parsing, then return null. If the option didn't need an argument, then
     * return the empty string.
     *
     * @param option - name of option to look up
     * @return argument for the option.
     */
    public String getArgument(String option) {
        return parsedOptions.get(option);
    }

    /**
     * Returns true if the argument was seen.
     *
     * @param option - name of option to look up
     * @return true/false
     */
    public boolean getBooleanArgument(String option) {
        return parsedOptions.get(option) != null;
    }

    /**
     * Return the arguments that weren't considered options or values for options.
     * @return arguments are array of string
     */
    public String[] getUnusedArguments() {
        return unusedArguments.toArray(new String[unusedArguments.size()]);
    }
}
// vim: set expandtab sw=4 :
