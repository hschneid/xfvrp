package xf.xfvrp.opt.fleetmix;

import java.util.List;

import xf.xfvrp.RoutingDataBag;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.opt.XFVRPSolution;

public interface IMixedFleetHeuristic {
	interface RoutePlanningFunction {
		XFVRPSolution apply(RoutingDataBag bag) throws XFVRPException;
	}
	
	List<XFVRPSolution> execute(
			Node[] nodes,
			Vehicle[] vehicles,
			RoutePlanningFunction routePlanningFunction,
			Metric metric,
			XFVRPParameter parameter,
			StatusManager statusManager) throws XFVRPException;
	
}
