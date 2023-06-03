package xf.xfvrp.opt.construct.insert;

import xf.xfvrp.base.*;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Insertion heuristic First-Best
 *
 * Scope: This heuristic shall reduce the number of routes during construction.
 *        If you wish a reduction of route length choose for Savings.
 *
 * All shipments are marked as unplanned and are brought in
 * a randomized order. Then the shipments are inserted sequentially,
 * where the cheapest insert position is searched for the current shipment.
 *
 * Additionally a reinsert is possible, so that bad decisions can be corrected.
 *
 * @author hschneid
 *
 */
public class XFPDPFirstBestInsert extends XFVRPOptBase {

	private static final int PICKUP = 0;
	private static final int DELIVERY = 1;

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Solution execute(Solution input) throws XFVRPException {
		List<Node[]> shipments = getShipments(input.getModel());

		// Init with empty route (attention for multiple depots)
		Solution solution = initNewSolution(input.getModel());

		// Randomized ordering of shipment insertions
		Collections.shuffle(shipments, rand);

		// Insert all shipments
		insertShipments(solution, shipments);

		// Reinsert all shipments (loop-able)
		for (int i = 0; i < model.getParameter().getILSLoops(); i++)
			reinsertNodes(solution, shipments);

		NormalizeSolutionService.normalizeRouteWithCleanup(solution);
		return solution;
	}

	/**
	 * Inserts the unplanned shipments into the solution
	 *
	 * The shipments are inserted sequentially in a randomized order. For each
	 * shipment the cheapest insert position is calculated with the current
	 * solution.
	 */
	private void insertShipments(Solution solution, List<Node[]> shipments) throws XFVRPException {
		for (Node[] shipment : shipments) {
			boolean inserted = insertShipment(solution, shipment);
			if(!inserted) {
				// If no feasible insertion can be found, add empty routes
				NormalizeSolutionService.normalizeRoute(solution);
				insertShipment(solution, shipment);
			}
		}
	}

	/**
	 * Each shipment will be removed from current solution and reinserted.
	 *
	 * This make sense, because this heuristic is strongly dependent from
	 * ordered sequence of shipments to insert. So a reinsert of first inserted
	 * nodes may give the possibility to find a better insert position than
	 * the order insertion before
	 */
	private void reinsertNodes(Solution solution, List<Node[]> shipments) throws XFVRPException {
		for (Node[] shipment : shipments) {
			// Remove shipment
			removeShipment(solution, shipment);
			// Reinsert shipment
			boolean inserted = insertShipment(solution, shipment);
			if(!inserted) {
				// If no feasible insertion can be found, add empty routes
				NormalizeSolutionService.normalizeRoute(solution);
				insertShipment(solution, shipment);
			}
		}
	}

	private boolean insertShipment(Solution solution, Node[] shipment) {
		// Get all feasible insertion points on current routes
		List<float[]> insertPoints = evaluate(solution, shipment);

		// Sort for lowest insertion costs (reverse orientation)
		insertPoints.sort((a, b) -> (int) ((b[0] - a[0]) * 1000f));

		// For all found feasible insertion points
		for (int i = insertPoints.size() - 1; i >= 0; i--) {
			float[] val = insertPoints.get(i);
			int routeIdx = (int) val[1];

			// Insert shipment
			Node[] oldRoute = insertShipment(solution, shipment, routeIdx, (int) val[2], (int) val[3]);

			// Evaluate new solution
			Quality qq = check(solution, routeIdx, routeIdx);
			if (qq.getPenalty() == 0) {
				solution.fixateQualities();
				return true;
			}

			// Reverse change
			solution.setRoute(
					routeIdx,
					oldRoute
			);
			solution.resetQualities();
		}

		return false;
	}

	/**
	 * Evaluates all insertion points in a current solution for
	 * a new shipment, where this shipment is not part of the current
	 * solution.
	 */
	private List<float[]> evaluate(Solution solution, Node[] shipment) throws XFVRPException {
		List<float[]> insertPoints = new ArrayList<>();

		// For all insert positions
		for (int routeIdx = 0; routeIdx < solution.getRoutes().length; routeIdx++) {
			Node[] route = solution.getRoutes()[routeIdx];
			for (int posA = 1; posA < route.length; posA++) {
				for (int posB = posA; posB < route.length; posB++) {
					insertPoints.add(new float[]{
							getEffortOfInsertion(shipment, route, posA, posB, solution.getModel()),
							routeIdx,
							posA,
							posB
					});
				}
			}
		}

		return insertPoints;
	}

	/**
	 * Calculates the additional distance to add a new shipment to given route.
	 */
	private float getEffortOfInsertion(Node[] shipment, Node[] route, int posA, int posB, XFVRPModel model) {
		if(posA == posB) {
			return model.getDistanceForOptimization(route[posA - 1], shipment[PICKUP]) +
					model.getDistanceForOptimization(shipment[DELIVERY], route[posA]) +
					model.getDistanceForOptimization(shipment[PICKUP], shipment[DELIVERY])
					- model.getDistanceForOptimization(route[posA - 1], route[posA]);
		}
		float effort = model.getDistanceForOptimization(route[posA - 1], shipment[PICKUP]) +
				model.getDistanceForOptimization(shipment[PICKUP], route[posA])
				- model.getDistanceForOptimization(route[posA - 1], route[posA]);
		effort += model.getDistanceForOptimization(route[posB - 1], shipment[DELIVERY]) +
				model.getDistanceForOptimization(shipment[DELIVERY], route[posB])
				- model.getDistanceForOptimization(route[posB - 1], route[posB]);

		return effort;
	}

	/**
	 * Inserts a shipment into a solution at a certain position.
	 */
	private Node[] insertShipment(Solution solution, Node[] shipment, int routeIdx, int posA, int posB) {
		try {
			Node[] route = solution.getRoutes()[routeIdx];
			Node[] newRoute = new Node[route.length + 2];

			System.arraycopy(route, 0, newRoute, 0, posA);
			newRoute[posA] = shipment[PICKUP];
			if(posA != posB) {
				System.arraycopy(route, posA, newRoute, posA + 1, posB - posA);
			}
			newRoute[posB + 1] = shipment[DELIVERY];
			System.arraycopy(route, posB, newRoute, posB + 2, route.length - posB);

			solution.setRoute(routeIdx, newRoute);

			return route;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Removes a certain shipment from route and returns the route without this shipment
	 */
	private void removeShipment(Solution solution, Node[] shipment) throws XFVRPException {
		for (int routeIdx = solution.getRoutes().length - 1; routeIdx >= 0; routeIdx--) {
			boolean found = false;
			Node[] route = solution.getRoutes()[routeIdx];

			for (int pos = route.length - 1; pos >= 0; pos--) {
				if(route[pos] == shipment[PICKUP] || route[pos] == shipment[DELIVERY]) {
					solution.setRoute(routeIdx, removeNode(route, pos));
					route = solution.getRoutes()[routeIdx];
					found = true;
				}
			}

			if(found)
				return;
		}
	}

	private Node[] removeNode(Node[] route, int pos) {
		Node[] newRoute = new Node[route.length - 1];

		System.arraycopy(route, 0, newRoute, 0, pos);
		System.arraycopy(route, pos + 1, newRoute, pos, route.length - pos - 1);

		return newRoute;
	}

	/**
	 * Initialize a new solution with single empty routes per depot.
	 */
	private Solution initNewSolution(XFVRPModel model) {
		Node[] route = new Node[2];
		route[0] = Util.createIdNode(model.getNodes()[0], 0);
		route[1] = Util.createIdNode(model.getNodes()[0], 1);

		Solution solution = new Solution(model);
		solution.addRoute(route);
		return NormalizeSolutionService.normalizeRoute(solution);
	}

	/**
	 * Retries all shipments with pickup and delivery nodes from node array in model.
	 */
	private List<Node[]> getShipments(XFVRPModel model) {
		return Arrays.stream(model.getNodes())
				.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
				.collect(Collectors.groupingBy(Node::getShipID))
				.values()
				.stream()
				.map(e -> {
					Node[] shipNodes = new Node[2];
					for (Node node : e) {
						if(node.getLoadType() == LoadType.PICKUP)
							shipNodes[PICKUP] = node;
						else if(node.getLoadType() == LoadType.DELIVERY)
							shipNodes[DELIVERY] = node;
					}

					return shipNodes;
				})
				.collect(Collectors.toList());
	}
}
