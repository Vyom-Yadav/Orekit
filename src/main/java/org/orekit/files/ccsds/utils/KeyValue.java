/* Copyright 2002-2021 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.files.ccsds.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.Keyword;

/** Holder for key-value pair.
 * <p>
 * The syntax for key-value lines in CCSDS files is:
 * </p>
 * <pre>
 * KEY = value [unit]
 * </pre>
 * <p>
 * The "[unit]" part (with the square brackets included) is optional.
 * The COMMENT keyword is an exception and does not have an '=' but directly
 * the value as free form text. The META_START, META_STOP, COVARIANCE_START
 * and COVARIANCE_STOP keywords are other exception and do not have anything
 * else following them on the line.
 * </p>
 * @author Luc Maisonobe
 * @since 6.1
 */
public class KeyValue {

    /** Regular expression for spaces. */
    private static final Pattern SPACE = Pattern.compile("\\p{Space}+");

    /** Regular expression for splitting lines. */
    private static final Pattern PATTERN =
            Pattern.compile("\\p{Space}*([A-Z][A-Z_0-9]*)\\p{Space}*=?\\p{Space}*(.*?)\\p{Space}*(?:\\[.*\\])?\\p{Space}*");

    /** Regular expression for user defined keywords. */
    private static final Pattern USER_DEFINED_KEYWORDS =
            Pattern.compile("USER_DEFINED_[A-Z][A-Z_]*");

    /** Pattern for splitting comma-separated lists. */
    private static final Pattern SPLIT_AT_COMMAS = Pattern.compile("\\p{Space}*,\\p{Space}*");

    /** Line from which pair is extracted. */
    private final String line;

    /** Number of the line from which pair is extracted. */
    private final int lineNumber;

    /** Name of the file. */
    private final String fileName;

    /** Keyword enum corresponding to parsed key. */
    private final Keyword keyword;

    /** Key part of the pair. */
    private final String key;

    /** Value part of the line. */
    private final String value;

    /** Build a pair by splitting a key-value line.
     * <p>
     * The splitting is very basic and only extracts words using a regular
     * expression ignoring the '=' sign and the optional unit. No attempt
     * is made to recognize the special keywords. The key and value parts
     * may be empty if not matched, and the keyword may be null.
     * </p>
     * <p> The value part may be upper case or lower case. This constructor
     * converts all lower case values to upper case.
     * @param line to split
     * @param lineNumber number of the line in the CCSDS data message
     * @param fileName name of the file
     */
    public KeyValue(final String line, final int lineNumber, final String fileName) {

        this.line       = line;
        this.lineNumber = lineNumber;
        this.fileName   = fileName;

        final Matcher matcher = PATTERN.matcher(line);
        if (matcher.matches()) {
            key   = matcher.group(1);
            final String rawValue = matcher.group(2);
            Keyword recognized;
            try {
                recognized = Keyword.valueOf(key);
            } catch (IllegalArgumentException iae) {
                if (USER_DEFINED_KEYWORDS.matcher(key).matches()) {
                    recognized = Keyword.USER_DEFINED_X;
                } else {
                    recognized = null;
                }
            }
            keyword = recognized;
            if (recognized == Keyword.COMMENT) {
                value = rawValue;
            } else {
                value = SPACE.matcher(rawValue.toUpperCase(Locale.US).replace('_', ' '))
                        .replaceAll(" ");
            }
        } else {
            key     = "";
            value   = key;
            keyword = null;
        }
    }

    /** Build a pair by giving the input arguments.
     *  This is essentially used while parsing XML files.
     *  It is made to be allow the use of the class KeyValue for both Keyvalue and XML file formats.
     *  Thus common functions can be used for the parsing.
     * <p>
     * The splitting is very basic and only extracts words using a regular
     * expression ignoring the '=' sign and the optional unit. No attempt
     * is made to recognize the special keywords. The key and value parts
     * may be empty if not matched, and the keyword may be null.
     * </p>
     * <p> The value part may be upper case or lower case. This constructor
     * converts all lower case values to upper case.
     * @param keyword the keyword
     * @param value the value attached to the keyword
     * @param line the line where the keyword was found
     * @param lineNumber number of the line in the CCSDS data message
     * @param fileName name of the file
     */
    public KeyValue(final Keyword keyword, final String value,
                    final String line, final int lineNumber,
                    final String fileName) {
        this.keyword = keyword;
        this.key = keyword.name();
        this.value = value;
        this.lineNumber = lineNumber;
        this.line = line;
        this.fileName = fileName;
    }

    /** Get the line where the keyword was found.
     * @return line where the keyword was found
     * @since 10.1
     */
    public String getLine() {
        return line;
    }

    /** Keyword corresponding to the parsed key.
     * @return keyword corresponding to the parsed key
     * (null if not recognized)
     */
    public Keyword getKeyword() {
        return keyword;
    }

    /** Get the key.
     * @return key
     */
    public String getKey() {
        return key;
    }

    /** Get the value.
     * @return value
     */
    public String getValue() {
        return value;
    }

    /** Get the value as a double number.
     * @return value
     */
    public double getDoubleValue() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            throw generateException();
        }
    }

    /** Get the value as an integer number.
     * @return value
     */
    public int getIntegerValue() {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            throw generateException();
        }
    }

    /** Get the value as a list.
     * @return value
     * @since 10.1
     */
    public List<String> getListValue() {
        return Arrays.asList(SPLIT_AT_COMMAS.split(value));
    }

    /** Generate a parse exception for this key value pair.
     * @return exception for this key value pair
     */
    public OrekitException generateException() {
        return new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                   lineNumber, fileName, line);
    }

}
