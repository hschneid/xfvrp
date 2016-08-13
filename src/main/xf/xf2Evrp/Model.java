package xf.xf2Evrp;

import java.util.List;

import xf.xfvrp.base.metric.Metric;

public class Model {

	public String[][] facilities;
	public String[][] satellites;
	public String[][] suppliers;
	
	public List<String[]> demandsFull;
	public List<String[]> demandsEmpty;
	
	public Metric metric;	
}
