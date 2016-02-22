package org.knime;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of NaivePhilipp.
 * 
 *
 * @author Philipp Kling
 */
public class NaivePhilippNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(NaivePhilippNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
    public static final String CFGKEY_CLASSVAR = "ClassVariable";
    public static final String CFGKEY_LAPLACE = "LaPlaceCorrection";
    public static final String CFGKEY_STARTPREDICT = "StartPredictionValue";
    public static final String CFGKEY_PARTITION = "Partition";

    /** initial default class value. */
    static final int DEFAULT_CLASSVAR = 1;
    static final double DEFAULT_LAPLACE = 0;
    static final int DEFAULT_STARTPREDICT = 1;
    static final double DEFAULT_PARTITION = 0.8;
    
    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels". 
    private final SettingsModelString m_classvar = new SettingsModelString(CFGKEY_CLASSVAR, 
    		"");
    private final SettingsModelDoubleBounded m_laplace = new SettingsModelDoubleBounded(NaivePhilippNodeModel.CFGKEY_LAPLACE,
    		NaivePhilippNodeModel.DEFAULT_LAPLACE,
    		Double.NEGATIVE_INFINITY,
    		Double.POSITIVE_INFINITY);
    private final SettingsModelIntegerBounded m_startpredict = new SettingsModelIntegerBounded(NaivePhilippNodeModel.CFGKEY_STARTPREDICT, 
    		NaivePhilippNodeModel.DEFAULT_STARTPREDICT, 
    		Integer.MIN_VALUE, 
    		Integer.MAX_VALUE);
    private final SettingsModelDoubleBounded m_partition = new SettingsModelDoubleBounded(NaivePhilippNodeModel.CFGKEY_PARTITION, 
    		NaivePhilippNodeModel.DEFAULT_PARTITION, 
    		Double.MIN_VALUE, 
    		Double.MAX_VALUE);

	private Exception Exception;
    
    /**
     * Constructor for the node model.
     */
    protected NaivePhilippNodeModel() {
    
        // TODO 1 incoming ports and 1 outgoing port is assumed
        super(1, 1);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	// Retrieval of basic information
    	DataTableSpec spec = inData[0].getDataTableSpec();
    	String[] columnNames = spec.getColumnNames();
    	int classColumn = DEFAULT_CLASSVAR;
    	int k_i = 1;
    	for(String columnName: columnNames){
    		if(columnName.equals(m_classvar.getStringValue())){
    			classColumn = k_i;
    		}
    		k_i++;
    	}
    	double trainShare = m_partition.getDoubleValue();
    	double laplace = m_laplace.getDoubleValue();
    	Learner learner = new Learner(spec, classColumn);    	
    	int iteration = 0;
    	int noColumns = columnNames.length;
    	
    	// Output preparation
    	DataColumnSpec[] allColSpecs = new DataColumnSpec[noColumns+2];
    	for (int k = 0; k < noColumns; k++){
        	allColSpecs[k] = new DataColumnSpecCreator(spec.getColumnSpec(k).getName(), spec.getColumnSpec(k).getType()).createSpec();
        }
    	allColSpecs[noColumns] = new DataColumnSpecCreator("Prediction", StringCell.TYPE).createSpec();
    	allColSpecs[noColumns+1] = new DataColumnSpecCreator("Accuracy", DoubleCell.TYPE).createSpec();
    	DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
    	
    	// Learning and Predicting 
    	for( DataRow row : inData[0]){
        	exec.checkCanceled();
    		if(iteration < inData[0].size()*trainShare){
    			learner.learn(row);
    		}
    		else {
    			int rowNo = iteration+1;    			
    			RowKey key = new RowKey("Row " + rowNo);
    			DataCell[] cells = new DataCell[noColumns+2];
    			for (int k = 0; k < noColumns; k++){
            		cells[k] = row.getCell(k);
            	}
            	cells[noColumns] = learner.predict(row, laplace);
            	cells[noColumns+1] = new DoubleCell(0.0);
            	DataRow rowOut = new DefaultRow(key, cells);
            	container.addRowToTable(rowOut);
                exec.setProgress(rowNo / (double)(inData[0].size()-inData[0].size()*trainShare),
                    "Adding row " + rowNo);
    			}
    		iteration++;
    		}
    	container.close();
    	exec.checkCanceled();
    	BufferedDataTable out = container.getTable();
    	return new BufferedDataTable[]{out};
    }

    
    /** {@inheritDoc} */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[] {InputPortRole.NONDISTRIBUTED_STREAMABLE};
    }

    /** {@inheritDoc} */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[] {OutputPortRole.NONDISTRIBUTED};
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(
            final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public StreamableOperatorInternals saveInternals() {
                return null;
            }

            @Override
            public void runFinal(final PortInput[] inputs,
                    final PortOutput[] outputs,
                    final ExecutionContext ctx) throws Exception {
                RowInput in = (RowInput)inputs[0];
                RowOutput out = (RowOutput)outputs[0];
                NaivePhilippNodeModel.this.execute(in, out, ctx);
            }
        };
    }
    
    protected void execute(final RowInput inData, final RowOutput output,
            final ExecutionContext exec) throws Exception{
    	
    	// Initialization
    	exec.setMessage("Started initializing...");
    	DataTableSpec spec = inData.getDataTableSpec();
    	String[] names_of_columns = spec.getColumnNames();
    	int class_column = DEFAULT_CLASSVAR;
    	int k_i = 1;
    	for(String name: names_of_columns){
    		if(name.equals(m_classvar.getStringValue())){
    			class_column = k_i;
    		}
    		k_i++;
    	}
    	double laplace = m_laplace.getDoubleValue();
    	int startPredict = m_startpredict.getIntValue();
    	Learner learner = new Learner(spec, class_column);

    	
    	int number_of_columns = spec.getColumnNames().length;

     
        // Streaming Part
        exec.setMessage("Started streaming...");
    	int iteration = 0;
    	int predictions = 0;
    	int correct_guesses = 0;
    	DataRow example;
    	while ((example = inData.poll()) != null) {
    		DataCell predictedClass = new StringCell("NaN");
    		// Predictions and Learning:
    		if(iteration + 1 >= startPredict){
    			predictedClass = learner.predict(example, laplace);
    			predictions++;
    			if(predictedClass.equals(example.getCell(class_column-1))){
    	    		correct_guesses++;
    	    	}
    		}
    		learner.learn(example);
    		
    		// Preparation of Output
    		RowKey key = example.getKey();
    		DataCell[] cells = new DataCell[number_of_columns+2];
        	for (int k = 0; k < number_of_columns; k++){
        		cells[k] = example.getCell(k);
        		}
    		cells[number_of_columns] = predictedClass;
    		if(predictions > 0){
    			cells[number_of_columns+1] = new DoubleCell((double)correct_guesses/(double)predictions);
    		}
    		else {
    			cells[number_of_columns+1] = new DoubleCell(0.0);
    		}
    		DataRow outRow = new DefaultRow(key, cells);
    		exec.setMessage("Added row " + iteration + " (\"" + key + "\")");
    		output.push(outRow);
    		
    		exec.checkCanceled();
    		iteration++;
    	}
    	exec.checkCanceled();
    	inData.close();
    	output.close();      	
    }

    protected static DataTableSpec createOutputSpec(DataTableSpec inSpec){
    	
    	// Initialization of Output tables:
    	//Table 1: Original data, predictions and accuracy.    	
    	int number_of_columns = inSpec.getColumnNames().length;
    	DataColumnSpec[] allColSpecs = new DataColumnSpec[number_of_columns+2];
    	for (int k = 0; k < number_of_columns; k++){
    		allColSpecs[k] = new DataColumnSpecCreator(inSpec.getColumnSpec(k).getName(), inSpec.getColumnSpec(k).getType()).createSpec();
    	}
        allColSpecs[number_of_columns] = new DataColumnSpecCreator("Prediction", StringCell.TYPE).createSpec();
        allColSpecs[number_of_columns+1] = new DataColumnSpecCreator("Accuracy", DoubleCell.TYPE).createSpec();
    	return new DataTableSpec(allColSpecs);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Code executed on reset.
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        // TODO: check if user settings are available, fit to the incoming
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message
    	
        return new DataTableSpec[]{createOutputSpec(inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        // TODO save user settings to the config object.
        m_classvar.saveSettingsTo(settings);
        m_laplace.saveSettingsTo(settings);
        m_startpredict.saveSettingsTo(settings);
        m_partition.saveSettingsTo(settings);        

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        // TODO load (valid) settings from the config object.
        // It can be safely assumed that the settings are valided by the 
        // method below.
        m_classvar.loadSettingsFrom(settings);
        m_laplace.loadSettingsFrom(settings);
        m_startpredict.loadSettingsFrom(settings);
        m_partition.loadSettingsFrom(settings);
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        // TODO check if the settings could be applied to our model
        // e.g. if the count is in a certain range (which is ensured by the
        // SettingsModel).
        // Do not actually set any values of any member variables.

        m_classvar.validateSettings(settings);
        m_laplace.validateSettings(settings);
        m_startpredict.validateSettings(settings);
        m_partition.validateSettings(settings);

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
        // TODO load internal data. 
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care 
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
        // TODO save internal models. 
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care 
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }

}

