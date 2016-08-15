package org.classilist.knime;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


class FileWriterNodeSettings extends FileWriterSettings {
    
    /** Policy how to proceed when output file exists 
     * (overwrite, abort, append). */
    enum FileOverwritePolicy {
        /** Fail during configure/execute. */
        Abort,
        /** Overwrite existing file. */
        Overwrite,
        /** Append to existing file. */
        Append
    }

    public static final String CFGKEY_FILE = "filename";

    private static final String CFGKEY_ADD_TABLENAME = "addTablename";

    private static final String CFGKEY_APPEND = "isAppendToFile";

    private static final String CFGKEY_OVERWRITE_POLICY = "fileOverwritePolicy";

    private String m_fileName;

    private boolean m_addTableName;

    private FileOverwritePolicy m_fileOverwritePolicy;
    
    FileWriterNodeSettings() {
        m_fileName = null;
        m_fileOverwritePolicy = FileOverwritePolicy.Overwrite;
    }

    /**
     * Constructs a new object reading the settings from the specified
     * NodeSettings object. If the settings object doesn't contain all settings
     * an exception is thrown. Settings are accepted and set internally even if
     * they are invalid or inconsistent.
     */
    FileWriterNodeSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super(settings);
        m_fileName = settings.getString(CFGKEY_FILE);

        FileOverwritePolicy fileOverwritePolicy;
        if (settings.containsKey(CFGKEY_OVERWRITE_POLICY)) { // since v2.1
            String val = settings.getString(CFGKEY_OVERWRITE_POLICY, 
                    FileOverwritePolicy.Abort.toString());
            try {
                fileOverwritePolicy = FileOverwritePolicy.valueOf(val);
            } catch (Exception e) {
                throw new InvalidSettingsException("Unable to parse 'file " 
                        + "overwrite policy' field: " + val, e);
            }
        } else if (settings.containsKey(CFGKEY_APPEND)) { // v1.3 - v2.0
            if (settings.getBoolean(CFGKEY_APPEND)) {
                fileOverwritePolicy = FileOverwritePolicy.Append;
            } else {
                // preferably this should default to Abort but that would 
                // break existing flows (change in behavior)
                fileOverwritePolicy = FileOverwritePolicy.Overwrite;
            }
        } else {
            // way too old - setting meaningful defaults here
            fileOverwritePolicy = FileOverwritePolicy.Abort;
        }
        m_fileOverwritePolicy = fileOverwritePolicy;
        m_addTableName = settings.getBoolean(CFGKEY_ADD_TABLENAME, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addString(CFGKEY_FILE, m_fileName);
        settings.addString(CFGKEY_OVERWRITE_POLICY, 
                m_fileOverwritePolicy.toString());
    }

    /*
     * ----------------------------------------------------------------------
     * Setter and getter methods for all settings.
     * ----------------------------------------------------------------------
     */
    /**
     * @return the addTableName
     */
    boolean addTableName() {
        return m_addTableName;
    }

    /**
     * @param addTableName the addTableName to set
     */
    void setAddTableName(final boolean addTableName) {
        m_addTableName = addTableName;
    }
    /**
     * @param fileOverwritePolicy the fileOverwritePolicy to set
     */
    void setFileOverwritePolicy(final FileOverwritePolicy fileOverwritePolicy) {
        if (fileOverwritePolicy == null) {
            m_fileOverwritePolicy = FileOverwritePolicy.Abort;
        } else {
            m_fileOverwritePolicy = fileOverwritePolicy;
        }
    }
    
    /**
     * @return the fileOverwritePolicy, never null
     */
    FileOverwritePolicy getFileOverwritePolicy() {
        return m_fileOverwritePolicy;
    }

    /**
     * @return the fileName
     */
    String getFileName() {
        return m_fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    void setFileName(final String fileName) {
        m_fileName = fileName;
    }

}

