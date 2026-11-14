package xf.xfvrp.opt.fleetmix;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.compartment.CompartmentType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.opt.Solution;

import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2026 Lars Hackstein
 * <p>
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Mixed fleet heuristic for the Vehicle Routing Problem with Trailers and
 * Transshipments (VRPTT): After the individual planning of each vehicle
 * (super class), the per-vehicle solutions are optimized together as a
 * joint multi-vehicle plan, so that transshipment records can span the
 * routes of DIFFERENT vehicles ("lorry A drops load x at the
 * transshipment location; lorry B picks it up there").
 * <p>
 * Without transshipment locations, this heuristic behaves exactly like the
 * {@link DefaultMixedFleetHeuristic}.
 *
 * @author lhackste
 */
public class VRPTTMixedFleetHeuristic extends DefaultMixedFleetHeuristic {

	private final VRPTTJointRecordOptimizer recordOptimizer = new VRPTTJointRecordOptimizer();

	@Override
	public List<Solution> execute(
			Node[] nodes,
			CompartmentType[] compartmentTypes,
			xf.xfvrp.base.Vehicle[] vehicles,
			RoutePlanningFunction routePlanningFunction,
			Metric metric,
			XFVRPParameter parameter,
			StatusManager statusManager) throws XFVRPException {
		List<Solution> solutions = super.execute(nodes, compartmentTypes, vehicles, routePlanningFunction, metric,
				parameter, statusManager);

		if (containsTransshipment(nodes))
			recordOptimizer.optimize(solutions, statusManager);

		return solutions;
	}

	/**
	 * VRPTT models contain transshipment locations.
	 */
	private boolean containsTransshipment(Node[] nodes) {
		return Arrays.stream(nodes).anyMatch(n -> n.getSiteType() == SiteType.TRANSSHIPMENT);
	}
}
