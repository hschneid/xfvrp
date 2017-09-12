package xf.xfvrp.opt.init;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator;
import xf.xfvrp.base.metric.internal.FixCostMetricTransformator;
import xf.xfvrp.base.metric.internal.OpenRouteMetricTransformator;
import xf.xfvrp.base.metric.internal.PresetMetricTransformator;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.BlockNameConverter;

public class ModelBuilder {
	
	/**
	 * Transforms the read data into a model, which can be used
	 * for optimization.
	 * 
	 * @param nodes
	 * @param veh Contains parameters for capacity, max route duration and others.
	 * @return Returns a model, which can be used for optimization procedures.
	 * @throws IllegalArgumentException
	 */
	public XFVRPModel build(Node[] nodes, Vehicle veh, Metric externalMetric, XFVRPParameter parameter, StatusManager statusManager) throws IllegalArgumentException {
		statusManager.fireMessage(StatusCode.RUNNING, "Initialisation of instance for vehicle "+veh.name);

		// Set local node index
		indexNodes(nodes);
		
		// Metric transformations
		InternalMetric internalMetric = buildInternalMetric(nodes, veh, externalMetric, parameter);

		// Metric transformations for optimization
		InternalMetric optMetric = buildOptimizationMetric(nodes, veh, internalMetric);

		statusManager.fireMessage(StatusCode.RUNNING, "Nbr of nodes : "+nodes.length);

		XFVRPModel model = new XFVRPModel(nodes, internalMetric, optMetric, veh, parameter);

		countNbrOfNodesInBlocks(nodes, model);
		
		return model;
	}

	private void countNbrOfNodesInBlocks(Node[] nodes, XFVRPModel model) {
		int[] nbrOfNodesInBlocks = model.getBlockPresetCountList();
		for (int i = 0; i < nodes.length; i++)
			if(nodes[i].getPresetBlockIdx() >= BlockNameConverter.DEFAULT_BLOCK_IDX)
				nbrOfNodesInBlocks[nodes[i].getPresetBlockIdx()]++;
	}

	private InternalMetric buildInternalMetric(Node[] nodes, Vehicle veh, Metric metric, XFVRPParameter parameter) {
		InternalMetric internalMetric = AcceleratedMetricTransformator.transform(metric, nodes, veh);
		if(parameter.isOpenRouteAtStart() || parameter.isOpenRouteAtEnd())
			internalMetric = OpenRouteMetricTransformator.transform(internalMetric, nodes, parameter);
		return internalMetric;
	}

	private void indexNodes(Node[] nodes) {
		for (int i = 0; i < nodes.length; i++)
			nodes[i].setIdx(i);
	}

	private InternalMetric buildOptimizationMetric(Node[] nodes, Vehicle veh, InternalMetric internalMetric) {
		InternalMetric optMetric = internalMetric;
		optMetric = FixCostMetricTransformator.transform(internalMetric, nodes, veh);
		optMetric = PresetMetricTransformator.transform(optMetric, nodes);
		return optMetric;
	}
}
