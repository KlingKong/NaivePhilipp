package org.knime;

import org.knime.core.data.NominalValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "NaivePhilipp" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Philipp Kling
 */
public class NaivePhilippNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring NaivePhilipp node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected NaivePhilippNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(NaivePhilippNodeModel.CFGKEY_CLASSVAR, " "),
        		"Select class attribute", 
        		0, 
        		true, 
        		NominalValue.class));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(
        				NaivePhilippNodeModel.CFGKEY_LAPLACE,
        				NaivePhilippNodeModel.DEFAULT_LAPLACE,
        				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
        		"LaPlace Correction value (gamma):", 
        		/*step*/ 0.05,
        		/*componentwidth*/ 5));  
        
        addDialogComponent(new DialogComponentNumber(
                new SettingsModelIntegerBounded(
                    NaivePhilippNodeModel.CFGKEY_STARTPREDICT,
                    NaivePhilippNodeModel.DEFAULT_STARTPREDICT,
                    Integer.MIN_VALUE, Integer.MAX_VALUE),
                    "(Online) Start Predictions at observation number: ", 
                    /*step*/ 1, 
                    /*componentwidth*/ 5));
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(
        				NaivePhilippNodeModel.CFGKEY_PARTITION,
        				NaivePhilippNodeModel.DEFAULT_PARTITION,
        				Double.MIN_VALUE, Double.MAX_VALUE),
        		"(Offline) Partitioning/ Train-Share: ", 
        		/*step*/ 0.05,
        		/*componentwidth*/ 5));         
    
    }
}

