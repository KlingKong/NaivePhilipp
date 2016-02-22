package org.knime;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "NaivePhilipp" Node.
 * Implementation of the Naive Bayes concept in KNIME with an adaption to streaming scenario.
 *
 * @author Philipp Kling
 */
public class NaivePhilippNodeFactory 
        extends NodeFactory<NaivePhilippNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public NaivePhilippNodeModel createNodeModel() {
        return new NaivePhilippNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<NaivePhilippNodeModel> createNodeView(final int viewIndex,
            final NaivePhilippNodeModel nodeModel) {
        return new NaivePhilippNodeView(nodeModel);
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
        return new NaivePhilippNodeDialog();
    }

}

