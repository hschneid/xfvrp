package xf.xfvrp.opt.construct.insert;

import xf.xfvrp.base.*;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.*;
import java.util.stream.IntStream;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Insertion heuristic First-Best for Pickup and Delivery Problem
 * 
 * All shipments are marked as unplanned and are brought in 
 * a randomized order. Then the shipments are inserted sequentially,
 * where the cheapest insert position is searched for the current route plan.
 * 
 * Additionally a reinsert is possible, so that bad decisions can be corrected.
 * 
 * @author hschneid
 *
 */
public class XFPDPFirstBestInsert extends XFVRPOptBase {

	private static final int PICKUP_POS = 0;
	private static final int DELIVERY_POS = 1;
	private static final int COST_VALUE = 2;
	
	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Solution execute(Solution solution) throws XFVRPException {
		List<Node[]> shipments = getShipments();
		
		// Init with empty route (attention for multiple depots)
		Node[] giantRoute = initRoute();

		// Randomized ordering of shipment insertion
		Collections.shuffle(shipments, rand);

		// Insert all shipments
		giantRoute = insertShipments(giantRoute, shipments);

		// Reinsert all shipments (loop-able)
		for (int i = 0; i < model.getParameter().getILSLoops(); i++)
			giantRoute = reinsertShipments(giantRoute, shipments);

		Solution newSolution = new Solution(model);
		newSolution.setGiantRoute(giantRoute);
		return NormalizeSolutionService.normalizeRoute(newSolution, model);
	}

	/**
	 * Inserts the given shipments (unplanned) into the giant route
	 * 
	 * The shipments are inserted sequentially as the ordering of the given shipment list.
	 * For each shipment the cheapest insert positions for pickup and delivery are 
	 * calculated with the current giant route solution. 
	 * Each insertion adjusts the current solution.
	 * 
	 * @param giantRoute Current valid solution of planned routes
	 * @param shipments List of unplanned shipments
	 * @return Giant route with all shipments inserted
	 */
	private Node[] insertShipments(Node[] giantRoute, List<Node[]> shipments) throws XFVRPException {
		for (Node[] shipment : shipments) {
			// Get all feasible insertion points on current giant route
			List<float[]> insertPoints = evaluate(giantRoute, shipment);

			// Sort for lowest insertion costs
			insertPoints.sort( (a,b) -> {return (int) ((a[COST_VALUE] - b[COST_VALUE]) * 1000f);});

			// Prepare new solution (additional space for two customer)
			Node[] newGiantRoute = new Node[giantRoute.length + 2];

			// For all found feasible insertion points
			for (int i = 0; i < insertPoints.size(); i++) {
				float[] val = insertPoints.get(i);

				// Insert shipment into new solution
				insertShipment(giantRoute, newGiantRoute, shipment[0], shipment[1], (int)val[PICKUP_POS], (int)val[DELIVERY_POS]);

				// Evaluate new solution
				Solution newSolution = new Solution(model);
				newSolution.setGiantRoute(newGiantRoute);
				Quality qq = check(newSolution);
				if(qq.getPenalty() == 0) {
					giantRoute = NormalizeSolutionService.normalizeRoute(newSolution, model).getGiantRoute();
					break;
				}
			}
		}

		return giantRoute;
	}

	/**
	 * Each shipment will be removed from current solution and reinserted.
	 * 
	 * This make sense, because the insertion heuristic is strongly dependent from
	 * ordered sequence of shipments to insert. Hence a reinsert of first inserted
	 * shipments may give the possibility to find better insert positions than
	 * the insert without the other shipments.
	 * 
	 * @param giantRoute Current solution of planned routes
	 * @param shipments List of all planned shipments
	 * @return New solution of planned routes
	 */
	private Node[] reinsertShipments(Node[] giantRoute, List<Node[]> shipments) throws XFVRPException {
		// New solutions will contain in first step one customer less
		Node[] reducedGiantRoute = new Node[giantRoute.length - 2];
		
		for (Node[] shipment : shipments) {
			Node pickup = shipment[0];
			Node delivery = shipment[1];
			
			// Remove shipment nodes from giant route
			giantRoute = removeShipment(giantRoute, reducedGiantRoute, pickup, delivery);

			// Get all feasible insertion points on current giant route
			List<float[]> insertPoints = evaluate(giantRoute, shipment);

			// Sort for lowest insertion costs
			insertPoints.sort( (a,b) -> {return (int) ((a[COST_VALUE] - b[COST_VALUE]) * 1000f);});

			// Prepare new solution (additional space for new customer)
			Node[] newGiantRoute = new Node[giantRoute.length + 2];

			// For all found feasible insertion points
			for (int i = 0; i < insertPoints.size(); i++) {
				float[] val = insertPoints.get(i);

				// Insert shipment into new solution
				insertShipment(giantRoute, newGiantRoute, shipment[0], shipment[1], (int)val[PICKUP_POS], (int)val[DELIVERY_POS]);

				// Evaluate new solution
				Solution newSolution = new Solution(model);
				newSolution.setGiantRoute(newGiantRoute);
				Quality qq = check(newSolution);
				if(qq.getPenalty() == 0) {
					giantRoute = NormalizeSolutionService.normalizeRoute(newSolution, model).getGiantRoute();
					reducedGiantRoute = new Node[giantRoute.length - 2];
					break;
				}
			}
		}

		return giantRoute;
	}

	/**
	 * Evaluates all possible insertion points in a current solution for
	 * a new shipment, where this shipment must not be part of the current
	 * solution.
	 * 
	 * @param giantRoute Current solution of planned routes
	 * @param shipment The shipment which shall be inserted
	 * @return List of evaluated insertion points (0 = index of pickup, position 1 = index of delivery position, 2 = insertion cost)
	 */
	private List<float[]> evaluate(Node[] giantRoute, Node[] shipment) throws XFVRPException {
		List<int[]> routes = getRoutes(giantRoute);

		List<float[]> insertPoints = new ArrayList<>();

		Node pickup = shipment[0];
		Node delivery = shipment[1];

		for (int[] routeStats : routes) {// Create partial route with two additional empty slot
			Node[] route = createEvaluationRoute(giantRoute, routeStats);

			// Get current solution
			route[1] = route[0];
			route[2] = route[0];
			Solution ss = new Solution(model);
			ss.setGiantRoute(route);
			Quality currentRouteQuality = check(ss);

			// For all insert positions for pickup
			int pickCnt = 1;
			for (int p = routeStats[0] + 1; p <= routeStats[1]; p++) {
				// Insert new pickup node in first empty slot
				route[pickCnt] = pickup;

				int deliCnt = pickCnt + 1;
				for (int k = p; k <= routeStats[1]; k++) {
					// Insert new delivery node in second empty slot
					route[deliCnt] = delivery;

					// Check for feasibility
					Solution newSolution = new Solution(model);
					newSolution.setGiantRoute(route);
					Quality newRouteQuality = check(newSolution);
					if (newRouteQuality != null && newRouteQuality.getPenalty() == 0) {
						insertPoints.add(new float[]{p, k, newRouteQuality.getCost() - currentRouteQuality.getCost()});
					}

					// Move second empty slot one position further
					route[deliCnt] = route[deliCnt + 1];
					deliCnt++;
				}

				// Restore the original giant route with start index pickCnt
				System.arraycopy(route, pickCnt + 1, route, pickCnt + 2, route.length - pickCnt - 2);

				// Move first empty slot one position further
				route[pickCnt] = route[pickCnt + 1];
				pickCnt++;
			}
		}

		return insertPoints;
	}

	/**
	 * Inserts a shipment (pickup and delivery node) into the giant route at certain positions.
	 * 
	 * @param giantRoute Original giant route with current solution
	 * @param newGiantRoute Node array with two places more.
	 * @param pickup Inserting pickup node
	 * @param delivery Inserting delivery node
	 * @param pickupPos Position where the pickup should be placed
	 * @param deliveryPos Position where the delivery should be placed
	 */
	private void insertShipment(Node[] giantRoute, Node[] newGiantRoute, Node pickup, Node delivery, int pickupPos, int deliveryPos) {
		System.arraycopy(giantRoute, 0, newGiantRoute, 0, pickupPos);
		newGiantRoute[pickupPos] = pickup;
		System.arraycopy(giantRoute, pickupPos, newGiantRoute, pickupPos + 1, deliveryPos - pickupPos);
		newGiantRoute[deliveryPos + 1] = delivery;
		System.arraycopy(giantRoute, deliveryPos, newGiantRoute, deliveryPos + 2, giantRoute.length - deliveryPos);
	}

	/**
	 * Removes a certain shipment from giant route and returns the giant route without this shipment.
	 * 
	 * @param giantRoute Original giant route with current solution
	 * @param reducedGiantRoute Node array with two places less.
	 * @param pickup Removing pickup node
	 * @param delivery Removing delivery node
	 * @return Giant route without the shipment
	 */
	private Node[] removeShipment(Node[] giantRoute, Node[] reducedGiantRoute, Node pickup, Node delivery) throws XFVRPException {
		OptionalInt posObj = IntStream
				.range(0, giantRoute.length)
				.filter(i -> giantRoute[i] == pickup)
				.findFirst();
		int pickupPos = posObj.orElseThrow(() -> new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "Could not find pickup position"));

		posObj = IntStream.range(0, giantRoute.length).filter(i -> giantRoute[i] == delivery).findFirst();
		int deliveryPos = posObj.orElseThrow(() -> new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "Could not find delivery position"));

		System.arraycopy(giantRoute, 0, reducedGiantRoute, 0, pickupPos);
		System.arraycopy(giantRoute, pickupPos + 1, reducedGiantRoute, pickupPos, deliveryPos - 1 - pickupPos);
		System.arraycopy(giantRoute, deliveryPos + 1, reducedGiantRoute, deliveryPos - 1, giantRoute.length - deliveryPos - 1);

		return reducedGiantRoute;
	}

	/**
	 * Initialize the giant route object with a single empty route of the depot.
	 * 
	 * @return Empty giant route object (no customers inserted only depots)
	 */
	private Node[] initRoute() {
		Node[] route = new Node[2];
		route[0] = Util.createIdNode(model.getNodes()[0], 0);
		route[1] = Util.createIdNode(model.getNodes()[0], 1);

		Solution newSolution = new Solution(model);
		newSolution.setGiantRoute(route);
		return NormalizeSolutionService.normalizeRoute(newSolution, model).getGiantRoute();
	}

	/**
	 * Returns a certain single route of the giant route with two empty slots more than the original route.
	 * 
	 * @param giantRoute Current solution of planned routes
	 * @param routeStats Info, where the route starts and ends
	 * @return Array of nodes between start and end position and with two empty slots more than the original route.
	 */
	private Node[] createEvaluationRoute(Node[] giantRoute, int[] routeStats) {
		Node[] route = new Node[(routeStats[1] - routeStats[0]) + 1 + 2];
		int cnt = 0;
		for(int p = routeStats[0]; p <= routeStats[1]; p++) {
			if(cnt == 1)
				cnt+=2;
			route[cnt++] = giantRoute[p];
		}
		
		return route;
	}

	/**
	 * Creates a list of indexes where the routes in the giant route start and end.
	 * 
	 * Start and end index are always at a depot node. The end of one route can be the start of next route. 
	 * 
	 * @param giantRoute Current solution of planned routes
	 * @return List of indexes where the routes in the giant route start and end. (0 = start index in giant route 1 = end index in giant route)
	 */
	private List<int[]> getRoutes(Node[] giantRoute) {
		List<int[]> routeList = new ArrayList<>();

		int[] arr = new int[] {0,-1};
		for (int i = 1; i < giantRoute.length; i++) 
			if(giantRoute[i].getSiteType() == SiteType.DEPOT) {
				arr[1] = i;
				routeList.add(arr);
				arr = new int[] {i,-1};
			}

		return routeList;
	}

	/**
	 * Retries all shipments with pickup and delivery nodes from node array in model. 
	 * 
	 * @return List of all shipments
	 */
	private List<Node[]> getShipments() {
		Map<String, Node[]> shipmentMap = new HashMap<>();

		for (int i = model.getNbrOfDepots() + model.getNbrOfReplenish(); i < model.getNbrOfNodes(); i++) {
			Node node = model.getNodes()[i];
			if(!shipmentMap.containsKey(node.getShipID()))
				shipmentMap.put(node.getShipID(), new Node[2]);

			Node[] nodes = shipmentMap.get(node.getShipID());
			if(node.getDemand()[0] > 0)
				nodes[PICKUP_POS] = node;
			else
				nodes[DELIVERY_POS] = node;
		}

		return new ArrayList<>(shipmentMap.values());
	}
}
