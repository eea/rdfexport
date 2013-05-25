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
 */
public class OptionParser {

    /** A map of the options that require an argument. */
    private HashMap<Character, Boolean> argExpectations;

    /** The parsed options. */
    private HashMap<Character, String> parsedOptions;

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
        parsedOptions = new HashMap<Character, String>();
        createArgMap(optString);
        parseArguments(args);
    }

    /**
     * Fill the map of a options that require an argument.
     * @param optString - The option string
     */
    private void createArgMap(String optString) {
        argExpectations = new HashMap<Character, Boolean>();

        for (int i = 0; i < optString.length(); i++) {
            if (i + 1 < optString.length() && optString.charAt(i + 1) == ':') {
               argExpectations.put(optString.charAt(i), true);
            } else {
               argExpectations.put(optString.charAt(i), false);
            }
        }
    }

    /**
     * Determine if the option requires a value.
     *
     * @param option - name of option to look up
     * @return true if it needs a value and false if not or the option is unknown.
     */
    private boolean optionNeedsValue(char option) {
        return argExpectations.get(option);
    }

    /**
     * Determine if the option is known.
     *
     * @param arg - value to check if it an option
     * @return true if it is known.
     */
    private boolean isOption(String arg) {
        return arg.startsWith("-") && arg.length() > 1;
    }

    /**
     * Determine if the option is known.
     *
     * @param option - value to check if it an option
     * @return true if it is known.
     */
    private boolean isKnownOption(char option) {
        return argExpectations.get(option) != null;
    }

    /**
     * Parse the arguments.
     *
     * @param args
     *            - The arguments from the command line
     */
    private void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (isOption(arg)) {
                char option = arg.charAt(1);
                if (isKnownOption(option)) {
                    if (optionNeedsValue(option)) {
                        if (arg.length() > 2) {
                            parsedOptions.put(option, arg.substring(2));
                        } else {
                            parsedOptions.put(option, args[++i]);
                        }
                    } else {
                        parseOptionCluster(arg);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown option: " + option);
                }
            } else {
                unusedArguments.add(arg);
            }
        }
    }

    /**
     * Loop through a cluster of option flags and put them in the map.
     * A cluster looks like <code>-tzv</code>, where each of "t", "z" and "v"
     * must take no arguments.
     *
     * @param arg - value containing a cluster
     */
    private void parseOptionCluster(String arg) {
        for (int clusterInx = 1; clusterInx < arg.length(); clusterInx++) {
            char option = arg.charAt(clusterInx);
            if (isKnownOption(option)) {
                if (!optionNeedsValue(option)) {
                    parsedOptions.put(option, "");
                } else {
                    throw new IllegalArgumentException("Unknown option: " + option);
                }
            } else {
                throw new IllegalArgumentException("Option requires argument: " + option);
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
    public String getOptionArgument(String option) {
        return parsedOptions.get(option.charAt(0));
    }

    /**
     * Returns true if the option was seen.
     *
     * @param option - name of option to look up
     * @return true/false
     */
    public boolean getOptionFlag(String option) {
        return parsedOptions.get(option.charAt(0)) != null;
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
