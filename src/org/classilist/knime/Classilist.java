package org.classilist.knime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.RowInput;
import org.classilist.knime.FileWriterSettings;

public class Classilist extends BufferedWriter {

    private final FileWriterSettings m_settings;

    private String m_lastWarning;

    private String m_newLine;

    public Classilist(final Writer writer) {
        this(writer, new FileWriterSettings());
        m_lastWarning = null;
    }

    /**
     * Creates new instance which writes tables to the given writer class. An
     * immediate write operation, will write the table headers (both column and
     * row headers) and will write missing values as "" (empty string).
     *
     * @param writer to write to
     * @param settings the object holding all settings, influencing how data
     *            tables are written to file.
     */
    public Classilist(final Writer writer, final FileWriterSettings settings) {
        super(writer);
        if (settings == null) {
            throw new NullPointerException(
                    "The CSVWriter doesn't accept null settings.");
        }

        m_lastWarning = null;
        m_settings = settings;

        // change all null strings to empty strings
        if (m_settings.getColSeparator() == null) {
            m_settings.setColSeparator("");
        }
        if (m_settings.getMissValuePattern() == null) {
            m_settings.setMissValuePattern("");
        }
        if (m_settings.getQuoteBegin() == null) {
            m_settings.setQuoteBegin("");
        }
        if (m_settings.getQuoteEnd() == null) {
            m_settings.setQuoteEnd("");
        }
        if (m_settings.getQuoteReplacement() == null) {
            m_settings.setQuoteReplacement("");
        }
        if (m_settings.getSeparatorReplacement() == null) {
            m_settings.setSeparatorReplacement("");
        }
        m_newLine = m_settings.getLineEndingMode().getEndString();
        if (m_newLine == null) {
            m_newLine = System.getProperty("line.separator");
        }
    }

    /**
     * @return the settings object that configures this writer. Modifying it
     *         influences its behavior.
     */
    protected FileWriterSettings getSettings() {
        return m_settings;
    }

    /**
     * Writes <code>table</code> with current settings.
     *
     * @param table the table to write to the file
     * @param exec an execution monitor where to check for canceled status and
     *            report progress to. (In case of cancellation, the file will be
     *            deleted.)
     * @throws IOException if any related I/O error occurs
     * @throws CanceledExecutionException if execution in <code>exec</code>
     *             has been canceled
     * @throws NullPointerException if table is <code>null</code>
     */
    public void write(final DataTable table, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        try {
            write(new DataTableRowInput(table), exec);
        } catch (InterruptedException e) {
            CanceledExecutionException cce = new CanceledExecutionException();
            cce.initCause(e);
            throw cce;
        }
    }

    /**
     * Same as above just usable with a streaming node implementation.
    */
    public void write(final RowInput input, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException, InterruptedException {

        DataTableSpec inSpec = input.getDataTableSpec();
        final int colCount = inSpec.getNumColumns();
        boolean first; // if first entry in the row (skip separator then)
        m_lastWarning = null; // reset any previous warning
        boolean correct = false; // if predicted column names are correctly set
        boolean classCorr = false;
        boolean fcorr = false , prcorr = false;
        int predInd = colCount-1;
        String classCol = "";

        // write column names
        if (m_settings.writeColumnHeader()) {

            if (m_settings.writeRowID()) {
                write(quoteString("row ID", false)); // RowHeader header
                first = false;
            } else {
                first = true;
            }
            // check is predicted column name is correct or not else throw exception
            for (int i = colCount-1; i >= 0; i--) {
            	String clnm = inSpec.getColumnSpec(i).getName();
            	int j = clnm.indexOf("Prediction (");
            	int k = clnm.indexOf(")");
            	if(j != -1 && k != -1 ) 
            		{
            		correct = true;
            		predInd = i;
            		//get the actual class column name
            		for(j += 12;j<k;j++) classCol = classCol.concat(clnm.charAt(j)+"");
            		
            		break;
            		}
            	
            }
            if (!correct) throw new CanceledExecutionException("Predicted column name not correct");
//            if(!correct) System.out.println("Predicted column name not correct");
            
                      
            // Change column names as per following scheme
            // Actual Class column - A-<colName> : having column name same as classCol
            // Predicted Class Column - Predicted : column at index predInd
            // Class Probabilities Column - P-<className> : having format "P (<classCol>=className)"
            // Features - F-<attributeName> : all others
            for (int i = 0; i < colCount; i++) {
                String cName = inSpec.getColumnSpec(i).getName();
                String newC="";
                // assign new column names to newC
//                System.out.print(cName + "-");
                
                if(cName.equals(classCol)) //class column
                {
                	newC = "A-"+classCol;
                	classCorr = true;
                }
                else if(i == predInd) //predicted column
                	newC = "Predicted";
                else if(cName.contains("P ("+classCol+"=")) //probability column
                {
                	prcorr = true;
                	newC = "";
                	int j = cName.indexOf('=');
                	int k = cName.indexOf(')');
                	for(j += 1; j<k ;j++)
                		newC = newC.concat(cName.charAt(j)+"");
                	
                	newC = "P-"+newC;
                }
                else // feature column
                {
                	fcorr = true;
                	newC = "F-"+cName;
                }
                		
//                System.out.println(newC); 
                // Done with column name changes
                
                if (!first) {
                    write(m_settings.getColSeparator());
                }
                first = false;
                write(quoteString(newC, false));
            }
            newLine();
        } // end of if write column names
        if(!classCorr) throw new CanceledExecutionException("Actual classified column does not exist");
        if(!fcorr) throw new CanceledExecutionException("Features do not exist");
        if(!prcorr) throw new CanceledExecutionException("Class Probabilities do not exist");

        // write each row of the data
        int i = 0;
        long rowCnt = -1;
        if (input instanceof DataTableRowInput) {
            rowCnt = ((DataTableRowInput)input).getRowCount();
        }

        DataRow row;
        while ((row = input.poll()) != null) {

            String rowKey = row.getKey().toString();
            String msg;

            // set the progress
            if (rowCnt <= 0) {
                msg = "Writing row " + (i + 1) + " (\"" + rowKey + "\")";
            } else {
                msg =
                        "Writing row " + (i + 1) + " (\"" + rowKey + "\") of "
                                + rowCnt;
                exec.setProgress(i / (double)rowCnt, msg);
            }
            // Check if execution was canceled !
            exec.checkCanceled();

            // write the columns
            first = true;
            // first, the row id
            if (m_settings.writeRowID()) {
                write(quoteString(row.getKey().getString(), false));
                first = false;
            }
            // now all data cells
            for (int c = 0; c < colCount; c++) {

                DataCell colValue = row.getCell(c);
                if (!first) {
                    write(m_settings.getColSeparator());
                }
                first = false;

                if (colValue.isMissing()) {
                    // never quote missing patterns.
                    write(m_settings.getMissValuePattern());
                } else {
                    boolean isNumerical = false;
                    DataType type = inSpec.getColumnSpec(c).getType();
                    String strVal = colValue.toString();

                    if (type.isCompatible(DoubleValue.class)) {
                        isNumerical = true;
                    }
                    if (isNumerical
                            && (m_settings.getDecimalSeparator() != '.')) {
                        // use the new separator only if it is not already
                        // contained in the value.
                        if (strVal.indexOf(m_settings.getDecimalSeparator())
                                < 0) {
                            strVal =
                                    replaceDecimalSeparator(strVal, m_settings
                                            .getDecimalSeparator());
                        } else {
                            if (m_lastWarning == null) {
                                m_lastWarning = "Specified decimal separator ('"
                                    + m_settings.getDecimalSeparator() + "') is"
                                    + " contained in the numerical value. "
                                    + "Not replacing decimal separator (e.g. "
                                    + "in row #" + i + " column #" + c + ").";
                            }
                        }
                    }
                    write(quoteString(strVal, isNumerical));

                }
            }
            newLine();
            i++;
        }
    }

    /**
     * If the specified string contains exactly one dot it is replaced by the
     * specified character.
     *
     */
    private String replaceDecimalSeparator(final String val,
            final char newSeparator) {

        int dotIdx = val.indexOf('.');

        // not a floating point number
        if (dotIdx < 0) {
            return val;
        }
        if (val.indexOf('.', dotIdx + 1) >= 0) {
            // more than one dot in val: not a floating point
            return val;
        }
        return val.replace('.', newSeparator);
    }

    /**
     * Returns a string that can be written out to the file that is treated
     * (with respect to quotes) according to the current settings.
    */
    protected String quoteString(final String data, final boolean isNumerical) {

        String result = data;

        switch (m_settings.getQuoteMode()) {
        case ALWAYS:
            if (m_settings.replaceSeparatorInStrings() && !isNumerical) {
                result = replaceSeparator(data);
                result = replaceAndQuote(result);
            } else {
                result = replaceAndQuote(data);
            }
            break;
        case IF_NEEDED:
            boolean needsQuotes = false;
            // we need quotes if the data contains the separator, equals the
            // missing value pattern.
            if (m_settings.getColSeparator().length() > 0) {
                needsQuotes = data.contains(m_settings.getColSeparator());
            } else {
                needsQuotes = true;
            }
            needsQuotes |= data.equals(m_settings.getMissValuePattern());

            result = data;
            if (m_settings.replaceSeparatorInStrings() && !isNumerical) {
                result = replaceSeparator(result);
            }
            if (needsQuotes) {
                result = replaceAndQuote(result);
            }
            break;
        case REPLACE:
            result = replaceSeparator(data);
            break;
        case STRINGS:
            if (isNumerical) {
                result = data;
            } else {
                result = data;
                if (m_settings.replaceSeparatorInStrings()) {
                    result = replaceSeparator(result);
                }
                result = replaceAndQuote(result);
            }
            break;
        }

        return result;
    }

    /**
     * Replaces the quote end pattern contained in the string and puts quotes
     * around the string.
     */
    protected String replaceAndQuote(final String data) {

        if (m_settings.getQuoteEnd().length() == 0) {
            return m_settings.getQuoteBegin() + data;
        }

        // start with the opening quotes
        StringBuilder result = new StringBuilder(m_settings.getQuoteBegin());
        int examined = 0; // index up to which the input string is handled

        do {
            int quoteIdx = data.indexOf(m_settings.getQuoteEnd(), examined);
            if (quoteIdx < 0) {
                // no (more) quote end pattern in the string. Copy the rest.
                result.append(data.substring(examined));
                // done.
                break;
            }

            // copy the part up to the quote pattern
            result.append(data.substring(examined, quoteIdx));

            // replace the quote pattern with the specified string
            result.append(m_settings.getQuoteReplacement());

            examined = quoteIdx + m_settings.getQuoteEnd().length();

        } while (examined < data.length());

        // finally append the closing quote
        result.append(m_settings.getQuoteEnd());

        return result.toString();

    }

    /**
     * Derives a string from the input string that has all appearances of the
     * separator replaced with the specified replacer string.
     */
    protected String replaceSeparator(final String data) {

        if (m_settings.getColSeparator().length() == 0) {
            return data;
        }

        boolean changed = false;
        StringBuilder result = new StringBuilder();
        int examined = 0; // index up to which the input string is handled

        do {
            int sepIdx = data.indexOf(m_settings.getColSeparator(), examined);
            if (sepIdx < 0) {
                // no (more) separator in the string. Copy the rest.
                result.append(data.substring(examined));
                // done.
                break;
            }

            changed = true;

            // copy the part up to the separator
            result.append(data.substring(examined, sepIdx));

            // replace the separator with the specified string
            result.append(m_settings.getSeparatorReplacement());

            examined = sepIdx + m_settings.getColSeparator().length();

        } while (examined < data.length());

        if (changed) {
            return result.toString();
        } else {
            return data;
        }

    }

    /**
     * @return true if a warning message is available
     */
    public boolean hasWarningMessage() {
        return m_lastWarning != null;
    }

    /**
     * Returns a warning message from the last write action. Or null, if there
     * is no warning set.
     */
    public String getLastWarningMessage() {
        return m_lastWarning;
    }

    /**
     * {@inheritDoc}
     * Writes a line feed according to the writer settings.
     */
    @Override
    public void newLine() throws IOException {
        write(m_newLine);
    }
}
