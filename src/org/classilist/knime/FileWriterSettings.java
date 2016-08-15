package org.classilist.knime;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Holds all settings used by the file writer. Writes them to and reads them
 * from the node settings objects and checks the values. This object is used in
 * the NodeModel to pass settings to the file writer.
 *
 */
public class FileWriterSettings {

    /** mode specifying how to quote the data. */
    public enum quoteMode {
        /** use quotes only if needed. */
        IF_NEEDED,
        /** use quotes always on non-numerical data. */
        STRINGS,
        /** always put quotes around the data. */
        ALWAYS,
        /** don't use quotes, replace separator pattern in data. */
        REPLACE
    }

    /**
     * Mode specifying how to end a line in the file written.
     */
    public enum LineEnding {
        /** System default. Null will cause the writer to read the system property */
        SYST(null),
        /** LF only. */
        LF(new String(new char[] {10})),
        /** CR + LF. */
        CRLF(new String(new char[] {13, 10})),
        /** CR only. */
        CR(new String(new char[] {13}));

        private final String m_ending;
        private LineEnding(final String ending) {
            m_ending = ending;
        }
        /** @return the mode specific line ending. */
        public String getEndString() {
            return m_ending;
        }
    }

    private static final String CFGKEY_SEPARATOR = "colSeparator";

    private static final String CFGKEY_QUOTE_BEGIN = "quoteBegin";

    private static final String CFGKEY_QUOTE_END = "quoteEnd";

    private static final String CFGKEY_QUOTE_REPL = "quoteReplacement";

    private static final String CFGKEY_QUOTE_MODE = "quoteMode";

    private static final String CFGKEY_SEPARATOR_REPL = "sepReplacePattern";

    private static final String CFGKEY_REPL_SEP_IN_STR = "ReplSepInStrings";

    private static final String CFGKEY_COLHEADER = "writeColHeader";

    private static final String CFGKEY_ROWHEADER = "writeRowHeader";

    private static final String CFGKEY_MISSING = "missing";

    private static final String CFGKEY_DEC_SEPARATOR = "decimalSeparator";

    private static final String CFGKEY_LINEENDING = "lineEndingMode";

    private static final String CFGKEY_CHARENCODING = "charSet";

    private String m_colSeparator;

    private String m_missValuePattern;

    private String m_quoteBegin;

    private String m_quoteEnd;

    private String m_quoteReplacement;

    private quoteMode m_quoteMode;

    private String m_separatorReplacement; // only used with mode=REPLACE

    private boolean m_writeColumnHeader;

    private boolean m_writeRowID;

    // replace colSep even in quoted strings.
    private boolean m_replaceSepInString;

    private char m_decimalSeparator;

    private LineEnding m_lineEnding;

    private String m_encoding; // if null, default encoding is used

    /**
     * Creates a settings object with default settings (backward compatible to
     * the old CSV writer). I. e. Comma as separator, always quote with double
     * quotes and remove quotes.
     */
    public FileWriterSettings() {
        m_colSeparator = ",";
        m_missValuePattern = "";
        m_quoteBegin = "\"";
        m_quoteEnd = "\"";
        m_quoteReplacement = "";

        m_quoteMode = quoteMode.STRINGS;
        m_separatorReplacement = "";
        m_replaceSepInString = false;
        m_writeColumnHeader = true;
        m_writeRowID = false;

        m_decimalSeparator = '.';

        m_lineEnding = LineEnding.SYST;
        m_encoding = null; // uses the default
    }

    /**
     * Creates a copy of the specified settings object.
     */
    public FileWriterSettings(final FileWriterSettings settings) {
        m_colSeparator = settings.m_colSeparator;
        m_missValuePattern = settings.m_missValuePattern;
        m_quoteBegin = settings.m_quoteBegin;
        m_quoteEnd = settings.m_quoteEnd;
        m_quoteReplacement = settings.m_quoteReplacement;

        m_quoteMode = settings.m_quoteMode;
        m_separatorReplacement = settings.m_separatorReplacement;
        m_replaceSepInString = settings.m_replaceSepInString;
        m_writeColumnHeader = true;
        m_writeRowID = settings.m_writeRowID;

        m_decimalSeparator = settings.m_decimalSeparator;
        m_lineEnding = settings.m_lineEnding;
        m_encoding = settings.m_encoding;
    }

    /**
     * Constructs a new object reading the settings from the specified
     * NodeSettings object. If the settings object doesn't contain all settings
     * an exception is thrown. Settings are accepted and set internally even if
     * they are invalid or inconsistent.
     */
    public FileWriterSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_missValuePattern = settings.getString(CFGKEY_MISSING);
        m_writeColumnHeader = settings.getBoolean(CFGKEY_COLHEADER);
        m_writeRowID = settings.getBoolean(CFGKEY_ROWHEADER);

        // these options are available since 1.2.++ only
        m_colSeparator = settings.getString(CFGKEY_SEPARATOR, ",");
        m_quoteBegin = settings.getString(CFGKEY_QUOTE_BEGIN, "\"");
        m_quoteEnd = settings.getString(CFGKEY_QUOTE_END, "\"");
        String mode = settings.getString(CFGKEY_QUOTE_MODE, quoteMode.STRINGS.name());
        try {
            m_quoteMode = Enum.valueOf(quoteMode.class, mode);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Specified quotation mode ('" + mode + "') is unknown.");
        }

        m_quoteReplacement = settings.getString(CFGKEY_QUOTE_REPL, "");
        m_separatorReplacement = settings.getString(CFGKEY_SEPARATOR_REPL, "");
        m_replaceSepInString = settings.getBoolean(CFGKEY_REPL_SEP_IN_STR,
                false);

        // available since 2.0
        m_decimalSeparator = settings.getChar(CFGKEY_DEC_SEPARATOR, '.');

        // available since 2.8.1
        String lineMode = settings.getString(CFGKEY_LINEENDING, LineEnding.SYST.name());
        try {
            m_lineEnding = Enum.valueOf(LineEnding.class, lineMode);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException("Specified line ending mode ('" + lineMode + "') is unknown.");
        }
        // since 3.1
        m_encoding = settings.getString(CFGKEY_CHARENCODING, null);
    }

    /**
     * Saves the current values (even if they are incomplete or invalid) in the
     * specified settings object.
     *
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {

        settings.addString(CFGKEY_SEPARATOR, m_colSeparator);
        settings.addString(CFGKEY_MISSING, m_missValuePattern);

        settings.addString(CFGKEY_QUOTE_BEGIN, m_quoteBegin);
        settings.addString(CFGKEY_QUOTE_END, m_quoteEnd);
        settings.addString(CFGKEY_QUOTE_MODE, m_quoteMode.name());
        settings.addString(CFGKEY_QUOTE_REPL, m_quoteReplacement);

        settings.addString(CFGKEY_SEPARATOR_REPL, m_separatorReplacement);
        settings.addBoolean(CFGKEY_REPL_SEP_IN_STR, m_replaceSepInString);
        settings.addBoolean(CFGKEY_COLHEADER, m_writeColumnHeader);
        settings.addBoolean(CFGKEY_ROWHEADER, m_writeRowID);

        settings.addChar(CFGKEY_DEC_SEPARATOR, m_decimalSeparator);
        settings.addString(CFGKEY_LINEENDING, m_lineEnding.name());
        settings.addString(CFGKEY_CHARENCODING, m_encoding);
    }

    /*
     * ----------------------------------------------------------------------
     * Setter and getter methods for all settings.
     * ----------------------------------------------------------------------
     */
    /**
     * @return the colSeparator
     */
    public String getColSeparator() {
        return m_colSeparator;
    }

    /**
     * @param colSeparator the string that is written our between data items.
     */
    public void setColSeparator(final String colSeparator) {
        m_colSeparator = colSeparator;
    }

    /**
     * @return the missValuePattern
     */
    public String getMissValuePattern() {
        return m_missValuePattern;
    }

    /**
     * @param missValuePattern the string that is written out for data cells
     *            with missing values.
     */
    public void setMissValuePattern(final String missValuePattern) {
        m_missValuePattern = missValuePattern;
    }

    /**
     * @return the quoteBegin
     */
    public String getQuoteBegin() {
        return m_quoteBegin;
    }

    /**
     * @param quoteBegin the string that is used as opening quotation mark.
     */
    public void setQuoteBegin(final String quoteBegin) {
        m_quoteBegin = quoteBegin;
    }

    /**
     * @return the quoteEnd
     */
    public String getQuoteEnd() {
        return m_quoteEnd;
    }

    /**
     * @param quoteEnd the string used as closing quotation mark.
     */
    public void setQuoteEnd(final String quoteEnd) {
        m_quoteEnd = quoteEnd;
    }

    /**
     * @return the quoteMode
     */
    public quoteMode getQuoteMode() {
        return m_quoteMode;
    }

    /**
     * @param quoteMode the quoteMode to set
     */
    public void setQuoteMode(final quoteMode quoteMode) {
        m_quoteMode = quoteMode;
    }

    /**
     * @return the separatorReplacement
     */
    public String getSeparatorReplacement() {
        return m_separatorReplacement;
    }

    /**
     * @param separatorReplacement the separatorReplacement to set
     */
    public void setSeparatorReplacement(final String separatorReplacement) {
        m_separatorReplacement = separatorReplacement;
    }


    /**
     * @return the replaceSepInString
     */
    public boolean replaceSeparatorInStrings() {
        return m_replaceSepInString;
    }

    /**
     * @param replaceSepInStrings if set true, the column separator will be
     * replaced in non-numerical columns - even if the data item written was
     * quoted.
     */
    public void setReplaceSeparatorInStrings(
            final boolean replaceSepInStrings) {
        m_replaceSepInString = replaceSepInStrings;
    }

    /**
     * @return the writeColumnHeader
     */
    public boolean writeColumnHeader() {
        return true;
    }

    /**
     * @param writeColumnHeader the writeColumnHeader to set
     */
    public void setWriteColumnHeader(final boolean writeColumnHeader) {
        m_writeColumnHeader = true;
    }

    /**
     * @return the writeRowID
     */
    public boolean writeRowID() {
        return m_writeRowID;
    }

    /**
     * @param writeRowID the writeRowID to set
     */
    public void setWriteRowID(final boolean writeRowID) {
        m_writeRowID = writeRowID;
    }

    /**
     * @return the quoteReplacement
     */
    public String getQuoteReplacement() {
        return m_quoteReplacement;
    }

    /**
     * @param quoteReplacement the quoteReplacement to set
     */
    public void setQuoteReplacement(final String quoteReplacement) {
        m_quoteReplacement = quoteReplacement;
    }

    /**
     * @return the current decimal separator
     */
    char getDecimalSeparator() {
        return m_decimalSeparator;
    }

    /**
     * Sets a new decimal separator character.
     * @param newSeparator the new decimal separator
     */
    void setDecimalSeparator(final char newSeparator) {
        m_decimalSeparator = newSeparator;
    }

    /**
     * @return the selected line ending mode
     * @since 2.9
     */
    public LineEnding getLineEndingMode() {
        return m_lineEnding;
    }

    /**
     * Set a new line ending mode.
     * @param mode to set
     * @since 2.9
     */
    public void setLineEndingMode(final LineEnding mode) {
        m_lineEnding = mode;
    }

    /**
     * Character set - or null if default encoding should be used.
     * @return character set - or null if default encoding should be used.
     * @since 3.1
     */
    public String getCharacterEncoding() {
        return m_encoding;
    }

    /**
     * The new char set - can be null indicating default encoding.
     * @param charSet or null.
     * @since 3.1
     */
    public void setCharacterEncoding(final String charSet) {
        m_encoding = charSet;
    }

    /**
     * takes a string that could contain "\t", or "\n", or "\\", and returns a
     * corresponding string with these patterns replaced by the characters '\t',
     * '\n', '\'.
     *
     */
    public static String unescapeString(final String str) {
        // Garbage in garbage out:
        if (str == null) {
            return null;
        }

        if (str.indexOf('\\') == -1) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < str.length()) {

            char c = str.charAt(i++);

            if (c == '\\') {
                if (i < str.length()) {
                    // it was not the last char
                    char c2 = str.charAt(i++);
                    switch (c2) {
                    case 'n':
                        result.append('\n');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case '\\':
                        result.append('\\');
                        break;
                    default:
                        result.append(c);
                        result.append(c2);
                        break;
                    }
                } else {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();

    }

    /**
     * Returns a string with all TABS and newLines being replaced by "\t" or
     * "\n" - and backslashes replaced by "\\".
     */
    public static String escapeString(final String str) {

        if (str == null) {
            return null;
        }

        boolean replaced = false; // return the input, if nothing changed

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\t') {
                result.append("\\t");
                replaced = true;
            } else if (c == '\n') {
                result.append("\\n");
                replaced = true;
            } else if (c == '\\') {
                result.append("\\\\");
                replaced = true;
            } else {
                result.append(c);
            }
        }

        if (!replaced) {
            return str;
        } else {
            return result.toString();
        }

    }
}
