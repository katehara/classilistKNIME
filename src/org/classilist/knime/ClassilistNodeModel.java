package org.classilist.knime;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.StringHistory;
import org.knime.core.util.FileUtil;
import org.classilist.knime.Classilist;
import org.classilist.knime.ClassilistNodeModel;
import org.classilist.knime.FileWriterNodeSettings;
import org.classilist.knime.FileWriterSettings;
import org.classilist.knime.FileWriterNodeSettings.FileOverwritePolicy;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;




/**
 * This is the model implementation of Classilist.
 * Connector Node for KNIME and classilist application for probabilistic classification results.
 *
 * @author Medha Katehara
 */
public class ClassilistNodeModel extends NodeModel {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ClassilistNodeModel.class);

    private FileWriterNodeSettings m_settings;

    /**
     * Identifier for StringHistory.
     */
    public static final String FILE_HISTORY_ID = "csvwrite";

    /**
     * Constructor, sets port count.
     */
    public ClassilistNodeModel() {
        super(1, 0);
        m_settings = new FileWriterNodeSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[] {InputPortRole.NONDISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {

        // the constructor complains if settings are missing
        FileWriterNodeSettings fws = new FileWriterNodeSettings(settings);

        // check consistency of settings

        String fileName = fws.getFileName();
        if (fileName == null || fileName.length() == 0) {
            throw new InvalidSettingsException("Missing output file name.");
        }

        // the separator must not be contained in the missing value pattern
        // nor in the quote begin pattern.
        if (notEmpty(fws.getColSeparator())) {
            if (notEmpty(fws.getMissValuePattern())) {
                if (fws.getMissValuePattern().contains(fws.getColSeparator())) {
                    throw new InvalidSettingsException(
                            "The pattern for missing values ('"
                                    + fws.getMissValuePattern()
                                    + "') must not contain the data "
                                    + "separator ('" + fws.getColSeparator()
                                    + "').");
                }
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings = new FileWriterNodeSettings(settings);

        if (notEmpty(m_settings.getFileName())) {
            StringHistory history = StringHistory.getInstance(FILE_HISTORY_ID);
            history.add(m_settings.getFileName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {

        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                    throws Exception {
                assert outputs.length == 0;
                RowInput input = (RowInput)inputs[0];
                doIt(null, input, exec);
                return;
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws Exception {
        return doIt(data[0], null, exec);
    }

    private BufferedDataTable[] doIt(final BufferedDataTable data, final RowInput input, final ExecutionContext exec)
            throws Exception {

        CheckUtils.checkDestinationFile(m_settings.getFileName(),
            m_settings.getFileOverwritePolicy() != FileOverwritePolicy.Abort);

        URL url = FileUtil.toURL(m_settings.getFileName());
        Path localPath = FileUtil.resolveToPath(url);

        boolean writeColHeader = m_settings.writeColumnHeader();
        OutputStream tempOut;
        URLConnection urlConnection = null;
        boolean appendToFile;
        if (localPath != null) {
            // figure out if the writer is actually supposed to write col headers
            if (Files.exists(localPath)) {
                appendToFile = m_settings.getFileOverwritePolicy() == FileOverwritePolicy.Append;
                writeColHeader = true;
            } else {
                appendToFile = false;
            }
            if (appendToFile) {
                tempOut = Files.newOutputStream(localPath, StandardOpenOption.APPEND);
            } else {
                tempOut = Files.newOutputStream(localPath);
            }
        } else {
            urlConnection = FileUtil.openOutputConnection(url, "PUT");
            tempOut = urlConnection.getOutputStream();
            appendToFile = false;
        }

        // make a copy of the settings with the modified value
        FileWriterSettings writerSettings = new FileWriterSettings(m_settings);
        writerSettings.setWriteColumnHeader(writeColHeader);

        tempOut = new BufferedOutputStream(tempOut);
        Charset charSet = Charset.defaultCharset();
        String encoding = writerSettings.getCharacterEncoding();
        if (encoding != null) {
            charSet = Charset.forName(encoding);
        }
        Classilist tableWriter = new Classilist(new OutputStreamWriter(tempOut, charSet), writerSettings);
        // write the comment header, if we are supposed to
        String tableName;
        if (input == null) {
            tableName = data.getDataTableSpec().getName();
        } else {
            tableName = input.getDataTableSpec().getName();
        }

        try {
            if (input == null) {
                tableWriter.write(data, exec);
            } else {
                tableWriter.write(input, exec);
            }
            tableWriter.close();            

            if (tableWriter.hasWarningMessage()) {
                setWarningMessage(tableWriter.getLastWarningMessage());
            }

            // execution successful
            if (input == null) {
                return new BufferedDataTable[0];
            } else {
                return null;
            }
        } catch (CanceledExecutionException cee) {
            try {
                tableWriter.close();                  
            } catch (IOException ex) {
                // may happen if the stream is already closed by the interrupted thread
            }
            if (localPath != null) {
                LOGGER.info("Table FileWriter canceled.");
                try {
                    Files.delete(localPath);
                    LOGGER.debug("File '" + m_settings.getFileName() + "' deleted after node has been canceled.");
                } catch (IOException ex) {
                    LOGGER.warn("Unable to delete file '"
                            + m_settings.getFileName() + "' after cancellation: " + ex.getMessage(), ex);
                }
            }
            throw cee;
        }

    }
    protected void reset() {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to save.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        String warnMsg = "";

        /*
         * check file access
         */
        String fileCheckWarning =
                CheckUtils.checkDestinationFile(m_settings.getFileName(),
                    m_settings.getFileOverwritePolicy() != FileOverwritePolicy.Abort);
        if (fileCheckWarning != null) {
            if (m_settings.getFileOverwritePolicy() == FileOverwritePolicy.Append) {
                fileCheckWarning = fileCheckWarning.replace("overwritten", "appended");
            }
            warnMsg = fileCheckWarning + "\n";
        }


        /*
         * check settings
         */
        if (isEmpty(m_settings.getColSeparator())
                && isEmpty(m_settings.getMissValuePattern())
                && (isEmpty(m_settings.getQuoteBegin()) || isEmpty(m_settings
                        .getQuoteEnd()))) {
            // we will write the table out - but they will have a hard
            // time reading it in again.
            warnMsg +=
                    "No separator and no quotes and no missing value "
                            + "pattern set."
                            + "\nWritten data will be hard to read!";
        }

        DataTableSpec inSpec = inSpecs[0];
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataType c = inSpec.getColumnSpec(i).getType();
            if (!c.isCompatible(DoubleValue.class)
                    && !c.isCompatible(IntValue.class)
                    && !c.isCompatible(StringValue.class)) {
                throw new InvalidSettingsException(
                        "Input table must only contain "
                                + "String, Int, or Doubles");
            }
        }
        if (inSpec.containsCompatibleType(DoubleValue.class)) {
            if (m_settings.getColSeparator().indexOf(
                    m_settings.getDecimalSeparator()) >= 0) {
                warnMsg +=
                        "The data separator contains (or is equal to) the "
                                + "decimal separator\nWritten data will be hard to read!";
            }
        }

        if (!warnMsg.isEmpty()) {
            setWarningMessage(warnMsg.trim());
        }

        return new DataTableSpec[0];
    }

    static boolean notEmpty(final String s) {
        if (s == null) {
            return false;
        }
        return (s.length() > 0);
    }

    static boolean isEmpty(final String s) {
        return !notEmpty(s);
    }

    /**
     * Creates an URL from the "file"name entered in the dialog.
     *
     */
    static URL getUrl(final String fileName) throws MalformedURLException {
        try {
            return new URL(fileName);
        } catch (MalformedURLException ex) {
            return Paths.get(fileName).toAbsolutePath().toUri().toURL();
        }
    }
}
