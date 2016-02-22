package org.knime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

public class Learner {
	
	
	private HashMap<String, HashMap<String, HashMap<String, Integer>>> counts = new HashMap<String, HashMap<String, HashMap<String, Integer>>>();
	private HashMap<String, HashMap<String, ArrayList<Double>>>  stats = new HashMap<String, HashMap<String, ArrayList<Double>>>(); 
    private HashMap<String, Double> class_counts = new HashMap<String, Double>();
    private int class_column;
    private DataTableSpec spec;
    private int correct_guesses;
    private int observations;
    
	public Learner(DataTableSpec spec, int class_column){
		this.class_column = class_column;
		this.spec = spec;
		this.correct_guesses = 0;
    	Set<DataCell> class_cells = spec.getColumnSpec(class_column-1).getDomain().getValues();
    	String[] column_names = spec.getColumnNames();
        class_counts.put("total", (double)0);
        
        for (DataCell class_cell : class_cells){        	
        	HashMap<String, HashMap<String, Integer>> var_count = new HashMap<String, HashMap<String, Integer>>();
        	HashMap<String, ArrayList<Double>> var_stat = new HashMap<String, ArrayList<Double>>();
        	
        	int col = 0;
        	for (String column : column_names){
        		if (spec.getColumnSpec(col).getType() == StringCell.TYPE & col != class_column-1){
        			HashMap<String, Integer> count = new HashMap<String, Integer>();
        			for (DataCell cell : spec.getColumnSpec(col).getDomain().getValues()){
        				count.put(cell.toString(), 0);
        			}
        			var_count.put(column, count);
        		}
        		else if (spec.getColumnSpec(col).getType() == DoubleCell.TYPE & col != class_column-1){
        			ArrayList<Double> stat = new ArrayList<Double>();
        			stat.add((double)0); //mean initialization
        			stat.add((double)0); //stddev intitialization
        			stat.add((double)0); // observations initializations
        			var_stat.put(column, stat);
        		}
        		else if (spec.getColumnSpec(col).getType() == IntCell.TYPE & col != class_column-1){
        			ArrayList<Double> stat = new ArrayList<Double>();
        			stat.add((double)0); //mean initialization
        			stat.add((double)0); //var intitialization
        			stat.add((double)0); // observations initializations
        			var_stat.put(column, stat);
        		}
        		col++;
        	}
        	
        	counts.put(class_cell.toString(), var_count);
        	stats.put(class_cell.toString(), var_stat);
        	class_counts.put(class_cell.toString(), (double)0);
        }
	}
	
	
	public void learn(DataRow example){
		int col = 0;
    	for (DataCell cell : example){
    		if (cell.getType() == StringCell.TYPE & col != class_column-1){
    			HashMap<String, HashMap<String, Integer>> var_count = counts.get(example.getCell(class_column-1).toString());
    			HashMap<String, Integer> count = var_count.get(spec.getColumnNames()[col]);
    			int c = count.get(cell.toString());
    			count.put(cell.toString(), c+1);
    			var_count.put(spec.getColumnNames()[col], count);
    			counts.put(example.getCell(class_column-1).toString(), var_count);
    		}
    		else if (cell.getType() == DoubleCell.TYPE & col != class_column-1 ){
    			HashMap<String, ArrayList<Double>> var_stat = stats.get(example.getCell(class_column-1).toString());
    			ArrayList<Double> stat = var_stat.get(spec.getColumnNames()[col]);
    			double x = ((DoubleCell)cell).getDoubleValue();
    			double mean_old = stat.get(0);
    			double var_old = stat.get(1);
    			double obs_old = stat.get(2); 
    			double obs_new = obs_old+1; //now = i in formula.
    			double mean_new = ((obs_old)*mean_old + x)/obs_new;
    			double var_new = (1/obs_new)*(obs_old*var_old + (x - mean_old)*(x-mean_new));
    			stat.set(0, mean_new);
    			stat.set(1, var_new);
    			stat.set(2, obs_new);
    			var_stat.put(spec.getColumnNames()[col], stat);
    			stats.put(example.getCell(class_column-1).toString(), var_stat);
    		}
    		else if (cell.getType() == IntCell.TYPE & col != class_column-1 ){
    			HashMap<String, ArrayList<Double>> var_stat = stats.get(example.getCell(class_column-1).toString());
    			ArrayList<Double> stat = var_stat.get(spec.getColumnNames()[col]);
    			double x = (double)((IntCell)cell).getIntValue();
    			double mean_old = stat.get(0);
    			double var_old = stat.get(1);
    			double obs_old = stat.get(2); 
    			double obs_new = obs_old+1; //now = i in formula.
    			double mean_new = ((obs_old)*mean_old + x)/obs_new;
    			double var_new = (1/obs_new)*((obs_old)*var_old + (x - mean_old)*(x-mean_new));
    			stat.set(0, mean_new);
    			stat.set(1, var_new);
    			stat.set(2, obs_new);
    			var_stat.put(spec.getColumnNames()[col], stat);
    			stats.put(example.getCell(class_column-1).toString(), var_stat);
    		}
    		else if (col == class_column-1){
    			observations++;
    			double count = class_counts.get(cell.toString());
    			//double total = class_counts.get("total");
    			class_counts.put(cell.toString(), count+1);        			
    			class_counts.put("total", (double)observations);
    		}
    		else
    			System.out.println("KEIN VERWERTBARER DATENTYP");
    		col++;
    	}
	}
	
	public DataCell predict(DataRow example, Double laplace){
		Set<DataCell> classes = spec.getColumnSpec(class_column-1).getDomain().getValues();
    	String[] names_of_columns = spec.getColumnNames();
    	
    	// Placeholders for later updates:
    	double max_proba = Double.NEGATIVE_INFINITY;
    	DataCell predicted_class = new StringCell("Error");
    	
    	int j = 0;
    	for (DataCell class_j: classes){
    		double proba = 0;
    		int k = 0;
    		for (String var: names_of_columns){
    			if (k != class_column-1){
    				if (spec.getColumnSpec(k).getType() == DoubleCell.TYPE){    					
    					double x = (((DoubleCell)example.getCell(k)).getDoubleValue());
    					double mu = stats.get(class_j.toString()).get(var).get(0);
    					double variance = stats.get(class_j.toString()).get(var).get(1);
    					double std = Math.sqrt(variance);
    					double gp = Gaussian.phi(x, mu, std);
    					proba += Math.log(gp);
    					k++;
    				}
    				else if (spec.getColumnSpec(k).getType() == IntCell.TYPE){
    					double x = (double)(((IntCell)example.getCell(k)).getIntValue());
    					double mu = stats.get(class_j.toString()).get(var).get(0);
    					double variance = stats.get(class_j.toString()).get(var).get(1);
    					double std = Math.sqrt(variance);
    					double gp = Gaussian.phi(x, mu, std);
    					proba += Math.log(gp);
            			k++;
    				}
    				else if (spec.getColumnSpec(k).getType() == StringCell.TYPE){        			
            			int count = counts.get(class_j.toString()).get(var.toString()).get(example.getCell(k).toString());
            			double sum = class_counts.get(class_j.toString());            			
            			proba += Math.log(((double)count + laplace)/(laplace * counts.get(class_j.toString()).get(var.toString()).size() + (double)sum));    										
    					k++;
    				}
    				else
    					k++;
    			}
    			else
    				k++;
    		}
    		//After iterating through all columns: multiply by class share
    		proba += Math.log((class_counts.get(class_j.toString()) + laplace)/(class_counts.get("total") + laplace * (class_counts.size()-1)));
    		//After iterating through all columns: check whether probability is higher than max_proba    		
    		if (proba > max_proba){
    			max_proba = proba;
    			predicted_class = class_j;
    		}
    		j++;
    	}    	
    	return predicted_class;
	}
	
	public double getAccuracy(){	
		double accuracy = (double)correct_guesses/(double)observations;
		return accuracy;
	}
	
	public int getCorrectGuesses(){
		return correct_guesses;
	}
	
	public HashMap<String, HashMap<String, HashMap<String, Integer>>> getCounts(){
		return counts;
	}
	
	public HashMap<String, HashMap<String, ArrayList<Double>>> getStats(){
		return stats;
	}
	
	public HashMap<String, Double> getClassCounts(){
		return class_counts;
	}
	
	public int getObs(){
		return observations;
	}
	
}
