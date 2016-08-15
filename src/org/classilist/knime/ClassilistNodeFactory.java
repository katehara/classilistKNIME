package org.classilist.knime;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Classilist" Node.
 * Connector Node for KNIME and classilist application for probabilistic classification results.
 *
 * @author Medha Katehara
 */
public class ClassilistNodeFactory 
        extends NodeFactory<ClassilistNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassilistNodeModel createNodeModel() {
        return new ClassilistNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<ClassilistNodeModel> createNodeView(final int viewIndex,
            final ClassilistNodeModel nodeModel) {
        return new ClassilistNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new ClassilistNodeDialog();
    }

}

