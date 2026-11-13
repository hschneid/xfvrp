package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;

/**
 * Copyright (c) 2026 Lars Hackstein
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Simulates the time evolution of a route, which mirrors the time model of
 * the {@link EvaluationService}: departure at the earliest possible time at
 * the depot, waiting until time window opening at each node and service
 * times. It is used by the SynchronizationAnalyzer to evaluate the temporal
 * consistency of transshipment records between two routes.
 * <p>
 * Not mirrored are the optional loading/unloading times at the depot
 * (parameterized, deactivated by default) and driving time restrictions,
 * which do not shift the arrival times relevant for transshipment records.
 *
 * @author hschneid
 */
public class RouteTimingSimulator {

	/**
	 * Simulates the route and returns the arrival time (including waiting
	 * until the time window opening) of each node position.
	 */
	public static float[] simulate(Node[] route, XFVRPModel model) {
		float[] arrivals = new float[route.length];

		float time = departureTimeAtDepot(route, model);

		Node lastNode = route[0];
		for (int i = 1; i < route.length; i++) {
			Node node = route[i];

			float travelTime = model.getDistanceAndTime(lastNode, node)[1];
			float arrival = time + travelTime;

			// Wait, if the node is reached before its time window opening
			float[] timeWindow = node.getTimeWindow(arrival);
			float waiting = Math.max(0, timeWindow[0] - arrival);
			arrivals[i] = arrival + waiting;

			time = arrivals[i] + node.getServiceTime();
			lastNode = node;
		}

		return arrivals;
	}

	/**
	 * Simulates the route and returns the arrival time of the visit at the
	 * given position.
	 */
	public static float simulateArrival(Node[] route, int pos, XFVRPModel model) {
		return simulate(route, model)[pos];
	}

	/**
	 * Mirrors the departure time calculation of the EvaluationService: The
	 * depot is left as early, that the first customer is reached at the
	 * opening of its time window.
	 */
	private static float departureTimeAtDepot(Node[] route, XFVRPModel model) {
		Node depot = route[0];

		Node nextNode = null;
		for (int i = 1; i < route.length - 1; i++) {
			if (route[i].getSiteType() == SiteType.CUSTOMER) {
				nextNode = route[i];
				break;
			}
		}

		if (nextNode == null) {
			// First stop is no customer (e.g. a transshipment location)
			nextNode = (route.length > 1) ? route[1] : null;
		}

		if (nextNode == null)
			return depot.getTimeWindow(0)[0];

		float earliestDepartureTime = nextNode.getTimeWindow(0)[0] - model.getDistanceAndTime(depot, nextNode)[1];

		return Math.max(depot.getTimeWindow(0)[0], earliestDepartureTime);
	}
}
