package org.classilist.knime;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

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
public class ClassilistNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring Classilist node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected ClassilistNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    ClassilistNodeModel.CFGKEY_COUNT,
                    ClassilistNodeModel.DEFAULT_COUNT,
                    Integer.MIN_VALUE, Integer.MAX_VALUE),
                    "Counter:", /*step*/ 1, /*componentwidth*/ 5));
                    
    }
}

