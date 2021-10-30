package xf.xfvrp.opt.fleetmix;

import xf.xfvrp.RoutingDataBag;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.compartment.CompartmentType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.opt.Solution;

import java.util.List;

public interface IMixedFleetHeuristic {

	interface RoutePlanningFunction {
		Solution apply(RoutingDataBag bag) throws XFVRPException;
	}
	
	List<Solution> execute(
			Node[] nodes,
			CompartmentType[] compartmentTypes,
			Vehicle[] vehicles,
			RoutePlanningFunction routePlanningFunction,
			Metric metric,
			XFVRPParameter parameter,
			StatusManager statusManager) throws XFVRPException;
	
}
