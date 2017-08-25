package xf.xfvrp.opt.construct.insert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Insertion heuristic First-Best
 * 
 * All customers are marked as unplanned and are brought in 
 * a randomized order. Then the customers are inserted sequentially,
 * where the cheapest insert position is search for the current customers to plan.
 * 
 * Additionally a reinsert is possible, so that bad descisions can be corrected.
 * 
 * @author hschneid
 *
 */
public class XFVRPFirstBestInsert extends XFVRPOptBase {

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Solution execute(Solution solution) {
		List<Node> customers = getCustomers();

		// Init with empty route (attention for multiple depots)
		Node[] giantRoute = initRoute();

		// Randomized ordering of customer insertion
		Collections.shuffle(customers, rand);

		// Insert all customers
		giantRoute = insertNodes(giantRoute, customers);

		// Reinsert all customers (loop-able)
		for (int i = 0; i < model.getParameter().getILSLoops(); i++)
			giantRoute = reinsertNodes(giantRoute, customers);
		
		Solution newSolution = new Solution();
		newSolution.setGiantRoute(giantRoute);
		return NormalizeSolutionService.normalizeRoute(newSolution, model);
	}

	/**
	 * Inserts a number of unplanned customers into an existing giant route solution.
	 * 
	 * Assumption: The unplanned customers must not be in the giant route.
	 * 
	 * @param giantRoute Current valid solution of planned routes
	 * @param unplannedCustomers List of unplanned customers
	 * @return Giant route with all customers inserted
	 */
	public Node[] execute(Node[] giantRoute, List<Node> unplannedCustomers) {
		// Randomized ordering of customer insertion
		Collections.shuffle(unplannedCustomers, rand);

		// Insert all customers
		giantRoute = insertNodes(giantRoute, unplannedCustomers);
		
		return giantRoute;
	}

	/**
	 * Inserts the unplanned customers into the giant route
	 * 
	 * The customers are inserted sequentially in a randomized order. For each
	 * customer the cheapest insert position is calculated with the current
	 * giant route solution. Each insertion adjusts the current solution.
	 * 
	 * @param giantRoute Current valid solution of planned routes
	 * @param customers List of unplanned customers
	 * @return Giant route with all customers inserted
	 */
	private Node[] insertNodes(Node[] giantRoute, List<Node> customers) {
		for (Node customer : customers) {
			// Get all feasable insertion points on current giant route
			List<float[]> insertPoints = evaluate(giantRoute, customer);

			// Sort for lowest insertion costs
			insertPoints.sort( (a,b) -> {return (int) ((a[1] - b[1]) * 1000f);});

			// Prepare new solution (additional space for new customer)
			Node[] newGiantRoute = new Node[giantRoute.length + 1];

			// For all found feasable insertion points
			for (int i = 0; i < insertPoints.size(); i++) {
				float[] val = insertPoints.get(i);

				// Insert customer into new solution
				insertCustomer(giantRoute, newGiantRoute, customer, (int)val[0]);

				// Evaluate new solution
				Solution solution = new Solution();
				solution.setGiantRoute(newGiantRoute);
				Quality qq = check(solution);
				if(qq.getPenalty() == 0) {
					giantRoute = NormalizeSolutionService.normalizeRoute(solution, model).getGiantRoute();
					break;
				}
			}
		}

		return giantRoute;
	}

	/**
	 * Each node will be removed from current solution and reinserted.
	 * 
	 * This make sense, because this heuristic is strongly dependent from
	 * ordered sequence of customer to insert. So a reinsert of first inserted
	 * nodes may give the possibility to find a better insert position than
	 * the insert without the other customers
	 * 
	 * @param giantRoute Current solution of planned routes
	 * @return New solution of planned routes
	 */
	private Node[] reinsertNodes(Node[] giantRoute, List<Node> customers) {
		// New solutions will contain in first step one customer less
		Node[] reducedGiantRoute = new Node[giantRoute.length - 1];

		for (Node customer : customers) {
			// Remove customer from giant route
			giantRoute = removeCustomer(giantRoute, reducedGiantRoute, customer);

			// Get all feasible insertion points on current giant route
			List<float[]> insertPoints = evaluate(giantRoute, customer);

			// Sort for lowest insertion costs
			insertPoints.sort( (a,b) -> (int) ((a[1] - b[1]) * 1000f));

			// Prepare new solution (additional space for new customer)
			Node[] newGiantRoute = new Node[giantRoute.length + 1];

			// For all found feasible insertion points
			for (int i = 0; i < insertPoints.size(); i++) {
				float[] val = insertPoints.get(i);

				// Insert customer into new solution
				insertCustomer(giantRoute, newGiantRoute, customer, (int)val[0]);

				// Evaluate new solution
				Solution solution = new Solution();
				solution.setGiantRoute(newGiantRoute);
				Quality qq = check(solution);
				if(qq.getPenalty() == 0) {
					giantRoute = NormalizeSolutionService.normalizeRoute(solution, model).getGiantRoute();
					reducedGiantRoute = new Node[giantRoute.length - 1];
					break;
				}
			}
		}

		return giantRoute;
	}

	/**
	 * Evaluates all insertion points in a current solution for
	 * a new customer, where this customer is not part of the current
	 * solution.
	 * 
	 * @param giantRoute Current solution of planned routes
	 * @param customer The customer node which shall be inserted
	 * @return List of evaluated insertion points (0 = index of position 1 = insertion cost)
	 */
	private List<float[]> evaluate(Node[] giantRoute, Node customer) {
		List<int[]> routes = getRoutes(giantRoute);

		List<float[]> insertPoints = new ArrayList<>();

		routes.forEach(routeStats -> {
			// Create partial route with one additional empty slot
			Node[] route = createEvaluationRoute(giantRoute, routeStats);

			// Check for feasibility
			route[1] = route[0];
			Solution ss = new Solution();
			ss.setGiantRoute(route);
			Quality currentRouteQuality = check(ss);
			
			// For all insert positions
			int cnt = 1;
			for(int p = routeStats[0] + 1; p <= routeStats[1]; p++) {
				// Insert new node in empty slot
				route[cnt] = customer;

				// Check for feasibility
				Solution solution = new Solution();
				solution.setGiantRoute(route);
				Quality newRouteQuality = check(solution);
				if(newRouteQuality != null && newRouteQuality.getPenalty() == 0) {
					insertPoints.add(new float[]{p, newRouteQuality.getCost() - currentRouteQuality.getCost()});
				}

				// Move empty slot one position further
				route[cnt] = route[cnt + 1];
				cnt++;
			}
		});

		return insertPoints;
	}

	/**
	 * Inserts a customer node into a new giant route at a certain position.
	 * 
	 * @param giantRoute Original giant route with current solution
	 * @param newGiantRoute Only an array with one more slot than the giant route
	 * @param customer Inserting customer node
	 * @param insertPos Position where the insert customer should be placed
	 */
	private void insertCustomer(Node[] giantRoute, Node[] newGiantRoute, Node customer, int insertPos) {
		System.arraycopy(giantRoute, 0, newGiantRoute, 0, insertPos);
		newGiantRoute[insertPos] = customer;
		System.arraycopy(giantRoute, insertPos, newGiantRoute, insertPos + 1, giantRoute.length - insertPos);
	}

	/**
	 * Removes a certain customer node from giant route and returns the giant route without this customer node 
	 * 
	 * @param giantRoute Original giant route with current solution
	 * @param reducedGiantRoute Only an array with one less slot than the giant route
	 * @param customer Removing customer node
	 * @return Giant route without the customer node
	 */
	private Node[] removeCustomer(Node[] giantRoute, Node[] reducedGiantRoute, Node customer) {
		OptionalInt posObj = IntStream.range(0, giantRoute.length).filter(i -> giantRoute[i] == customer).findFirst();
		int pos = posObj.getAsInt();

		System.arraycopy(giantRoute, 0, reducedGiantRoute, 0, pos);
		System.arraycopy(giantRoute, pos + 1, reducedGiantRoute, pos, giantRoute.length - pos - 1);

		return reducedGiantRoute;
	}

	/**
	 * Initialize the giant route object with a single empty route of the depot.
	 * 
	 * @return Empty giant route object (no customers inserted only depot)
	 */
	private Node[] initRoute() {
		Node[] route = new Node[2];
		route[0] = Util.createIdNode(model.getNodes()[0], 0);
		route[1] = Util.createIdNode(model.getNodes()[0], 1);

		Solution solution = new Solution();
		solution.setGiantRoute(route);
		return NormalizeSolutionService.normalizeRoute(solution, model).getGiantRoute();
	}

	/**
	 * Retrieve a single route of the giant route
	 * 
	 * @param giantRoute Current solution of planned routes
	 * @param routeStats Info where the route starts and ends
	 * @return Array of nodes between start and end position (and with an empty slot for new customers)
	 */
	private Node[] createEvaluationRoute(Node[] giantRoute, int[] routeStats) {
		Node[] route = new Node[(routeStats[1] - routeStats[0]) + 1 + 1];
		int cnt = 0;
		for(int p = routeStats[0]; p <= routeStats[1]; p++) {
			if(cnt == 1)
				cnt++;
			route[cnt++] = giantRoute[p];
		}
		
		route[route.length - 1] = route[0];
		
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
	 * Retries all customers nodes from node array in model. 
	 * 
	 * @return List of all customers
	 */
	private List<Node> getCustomers() {
		List<Node> customers = new ArrayList<>();
		for (int i = model.getNbrOfDepots()+model.getNbrOfReplenish(); i < model.getNbrOfNodes(); i++)
			customers.add(model.getNodes()[i]);
		return customers;
	}

}
