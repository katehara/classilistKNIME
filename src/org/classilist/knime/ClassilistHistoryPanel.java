package org.classilist.knime;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.LinkedHashSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.StringHistory;
import org.classilist.knime.ClassilistHistoryPanel;
import org.classilist.knime.ClassilistNodeModel;

public class ClassilistHistoryPanel extends JPanel {

    private final JComboBox<String> m_textBox;

    private final JButton m_chooseButton;

    private final JLabel m_warnMsg;

    /**
     * Creates new instance, sets properties, for instance renderer,
     * accordingly.
     */
    public ClassilistHistoryPanel() {
        this(null);
    }

    /**
     * Creates new instance, sets properties, for instance renderer,
     * accordingly.
     */
    ClassilistHistoryPanel(final FlowVariableModel fvm) {
        super(new GridBagLayout());
        m_textBox = new JComboBox<String>(new DefaultComboBoxModel<String>());
        m_textBox.setEditable(true);
        m_textBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        m_textBox.setPreferredSize(new Dimension(300, 25));
        m_textBox.setRenderer(new ConvenientComboBoxRenderer());

        // install listeners to update warn message whenever file name changes
        m_textBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                fileLocationChanged();
            }
        });
        /* install action listeners */
        m_textBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                fileLocationChanged();
            }
        });
        Component editor = m_textBox.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                @Override
                public void changedUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }
            });
        }

        m_chooseButton = new JButton("Browse...");
        m_chooseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String newFile = getOutputFileName();
                if (newFile != null) {
                    newFile = newFile.trim();
                    m_textBox.setSelectedItem(newFile);
                }
            }
        });
        m_warnMsg = new JLabel();
        // this ensures correct display of the changing label content...
        m_warnMsg.setSize(new Dimension(350, 25));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        add(m_textBox, c);
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 5, 0, 0);
        add(m_chooseButton, c);

        if (fvm != null) {
            c.gridx = 2;
            c.insets = new Insets(2, 10, 2, 2);
            add(new FlowVariableModelButton(fvm), c);
            fvm.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(final ChangeEvent evt) {
                    FlowVariableModel wvm =
                            (FlowVariableModel)(evt.getSource());
                    m_textBox.setEnabled(!wvm.isVariableReplacementEnabled());
                    m_chooseButton.setEnabled(!wvm
                        .isVariableReplacementEnabled());
                }
            });
            c.insets = new Insets(0, 0, 0, 0);
        }

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = (fvm == null) ? 2 : 3;
        add(m_warnMsg, c);

        updateHistory();
        fileLocationChanged();
    }

    private void fileLocationChanged() {
        String selFile = getSelectedFile();
        m_warnMsg.setText("");
        if ((selFile != null) && !selFile.isEmpty()) {
                File file = getFile(selFile);
                File df = getFile(m_textBox.getEditor().getItem().toString() + "/data");
                File hf = getFile(m_textBox.getEditor().getItem().toString() + "/index.html");
                if (file.exists() && df.exists() && hf.exists()) {
                    m_warnMsg.setText("Perfect!");
                }
                else m_warnMsg.setText("Not a Classilist Installation!!!");
        }
    }

    private String getOutputFileName() {
        // file chooser triggered by choose button
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int r = fileChooser.showDialog(ClassilistHistoryPanel.this, "Done");
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.exists()){
                return file.getAbsolutePath();               
            }
            return null;
        }
        return null;
    }

    /**
     * Get currently selected file.
     */
    public String getSelectedFile() {
        return ((JTextField) m_textBox.getEditor().getEditorComponent()).getText();
    }

    /**
     * Set the file url as default.
     *
     */
    public void setSelectedFile(final String url) {
        m_textBox.setSelectedItem(url);
    }

    /** Updates the elements in the combo box, reads the file history. */
    public void updateHistory() {
        StringHistory history =
                StringHistory.getInstance(ClassilistNodeModel.FILE_HISTORY_ID);
        String[] allVals = history.getHistory();
        LinkedHashSet<String> list = new LinkedHashSet<String>();
        for (String s : allVals) {
            list.add(s);
        }
        DefaultComboBoxModel<String> comboModel =
                (DefaultComboBoxModel<String>)m_textBox.getModel();
        comboModel.removeAllElements();
        for (String s : list) {
            comboModel.addElement(s);
        }
        // changing the model will also change the minimum size to be
        // quite big. We have a tooltip, we don't need that
        Dimension newMin = new Dimension(0, getPreferredSize().height);
        setMinimumSize(newMin);
    }

    /**
     * Return a file object for the given fileName. It makes sure that if the
     * fileName is not absolute it will be relative to the user's home dir.
     */
    public static File getFile(final String fileName) {
        File f = new File(fileName);
        if (!f.isAbsolute()) {
            f = new File(new File(System.getProperty("user.home")), fileName);
        }
        return f;
    }

    /**
     * Adds a change listener to the panel that gets notified whenever the entered file name changes.
     */
    public void addChangeListener(final ChangeListener cl) {
        ((JTextField) m_textBox.getEditor().getEditorComponent()).addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(final KeyEvent e) {
            }

            @Override
            public void keyReleased(final KeyEvent e) {
                cl.stateChanged(new ChangeEvent(e.getSource()));
            }

            @Override
            public void keyPressed(final KeyEvent e) {
            }
        });
        m_textBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                cl.stateChanged(new ChangeEvent(e.getSource()));
            }
        });
    }
}

