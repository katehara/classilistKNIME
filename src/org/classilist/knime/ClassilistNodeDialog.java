package org.classilist.knime;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.util.FileUtil;
import org.classilist.knime.ClassilistHistoryPanel;
import org.classilist.knime.FileWriterNodeSettings;
import org.classilist.knime.FileWriterNodeSettings.FileOverwritePolicy;

/**
 * <code>NodeDialog</code> for the "Classilist" Node.
 * Connector Node for KNIME and classilist application for probabilistic classification results.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Medha Katehara
 */
public class ClassilistNodeDialog extends NodeDialogPane {

    /** textfield to enter file name. */
    private final ClassilistHistoryPanel m_textBox;

    /** Checkbox for writing column header. */

    boolean m_isLocalDestination;


    /**
     * Creates a new CSV writer dialog.
     */
    public ClassilistNodeDialog() {
        super();

        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Classilist location:"));
        m_textBox = new ClassilistHistoryPanel(createFlowVariableModel(
              FileWriterNodeSettings.CFGKEY_FILE,
              FlowVariable.Type.STRING));
        m_textBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                String selFile = m_textBox.getSelectedFile();
                if ((selFile != null) && !selFile.isEmpty()) {
                    try {
                        URL newUrl = FileUtil.toURL(selFile);
                        Path path = FileUtil.resolveToPath(newUrl);
                        m_isLocalDestination = (path != null);
                    } catch (IOException | URISyntaxException | InvalidPathException ex) {
                        // ignore
                    }
                }
            }
        });
        filePanel.add(m_textBox);
        filePanel.add(Box.createHorizontalGlue());


        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(filePanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(Box.createVerticalGlue());

        addTab("Settings", panel);

    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {

        FileWriterNodeSettings newValues;
        try {
            newValues = new FileWriterNodeSettings(settings);
        } catch (InvalidSettingsException ise) {
            // use default settings if we didn't get any useful ones
            newValues = new FileWriterNodeSettings();
        }

        m_textBox.updateHistory();
        m_textBox.setSelectedFile(newValues.getFileName());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {

        FileWriterNodeSettings values = new FileWriterNodeSettings();

        values.setFileName(m_textBox.getSelectedFile()+"/data/out.csv");

        FileOverwritePolicy overwritePolicy = FileOverwritePolicy.Overwrite;
        values.setFileOverwritePolicy(overwritePolicy);
        values.saveSettingsTo(settings);
    }
}


