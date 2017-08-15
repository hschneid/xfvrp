package xf.xfvrp.opt.construct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;
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
 * Contains the Savings optimization procedure with
 * acceptance value lamda. 
 * 
 * @author hschneid
 *
 */
public class XFVRPSavings extends XFVRPOptBase {

	protected float lamda = 1;

	/**
	 * Executes the Savings routing
	 * 
	 * @param giantRoute
	 * @return
	 */
	@Override
	public Solution execute(Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		final Node depot = giantRoute[0];

		Node[][] routes = buildRoutes(giantRoute, model);

		SavingsDataBag dataBag = prepare(routes);

		createSavingsMatrix(depot, dataBag);

		improve(depot, routes, dataBag);

		Solution newSolution = new Solution();
		newSolution.setGiantRoute(buildGiantRoute(routes, depot));
		return newSolution;
	}

	private SavingsDataBag prepare(Node[][] routes) {
		SavingsDataBag dataBag = new SavingsDataBag();

		List<Node> nodeList = new ArrayList<>();
		int[] routeIdxForStartNode = new int[model.getNodes().length];
		int[] routeIdxForEndNode = new int[model.getNodes().length];

		Arrays.fill(routeIdxForStartNode, -1);
		Arrays.fill(routeIdxForEndNode, -1);

		// Trage f�r jeden Start- oder Ziel-Knoten ein, auf welcher Tour
		// er zu finden ist. Nat�rlich kann ein Knoten nur auf einer Tour sein.
		for (int i = 0; i < routes.length; i++) {
			Node firstCustomer = routes[i][0];
			Node lastCustomer = routes[i][routes[i].length - 1];
			
			routeIdxForStartNode[firstCustomer.getIdx()] = i;
			routeIdxForEndNode[lastCustomer.getIdx()] = i;

			nodeList.add(firstCustomer);
			if(firstCustomer != lastCustomer)
				nodeList.add(lastCustomer);
		}

		dataBag.setRouteIdxForStartNode(routeIdxForStartNode);
		dataBag.setRouteIdxForEndNode(routeIdxForEndNode);
		dataBag.setNodeList(nodeList);

		return dataBag;
	}

	private void improve(Node depot, Node[][] routeArr, SavingsDataBag dataBag) {
		boolean isImproved = true;
		while(isImproved) {
			isImproved = applyNextSaving(depot, routeArr, dataBag);
		}
	}

	private boolean applyNextSaving(Node depot, Node[][] routes, SavingsDataBag dataBag) {
		Node depotStart = Util.createIdNode(depot, 0);
		Node depotEnd = Util.createIdNode(depot, 1);

		int[] routeIdxForStartNode = dataBag.getRouteIdxForStartNode();
		int[] routeIdxForEndNode = dataBag.getRouteIdxForEndNode();
		List<float[]> savingsMatrix = dataBag.getSavingsMatrix();

		int savingsIdx = savingsMatrix.size() - 1;

		// Bestimme das beste g�ltige Saving
		for (int i = savingsIdx; i >= 0; i--) {
			savingsIdx--;
			float[] saving = savingsMatrix.get(i);

			int route1 = routeIdxForEndNode[(int) saving[0]];
			int route2 = routeIdxForStartNode[(int) saving[1]];

			// Beide Routen m�ssen noch zur Verf�gung stehen
			// ODER Wenn beide Knoten auf der selben Tour liegen,
			// ist eine Verkn�pfung nicht mehr m�glich
			if(route1 == -1 || route2 == -1 || route1 == route2)
				continue;

			// Merge
			int routeLength1 = routes[route1].length;
			int routeLength2 = routes[route2].length;

			Node[] coreRoute = mergeRoutes(routes, route1, route2);
			Node[] newRoute = addDepots(coreRoute, depotStart, depotEnd);

			// Check
			Solution smallSolution = new Solution();
			smallSolution.setGiantRoute(newRoute);
			Quality q = check(smallSolution);

			if(q.getPenalty() == 0) {
				// Aktualisiere die Datenstrukturen
				// Der Start von Route1 bleibt
				// Der Start von Route2 f�llt weg
				routeIdxForStartNode[routes[route2][0].getIdx()] = -1;
				// Das Ziel von Route1 f�llt weg
				routeIdxForEndNode[routes[route1][routeLength1 - 1].getIdx()] = -1;
				// Das Ziel von Route2 ist jetzt auf Route1
				routeIdxForEndNode[routes[route2][routeLength2 - 1].getIdx()] = route1;

				// Die neue Route1
				newRoute = coreRoute;

				// Route2 ist obsolet
				routes[route2] = null;

				return true;
			}
		}

		return false;
	}

	private Node[] addDepots(Node[] routeWithoutDepots, Node depotStart, Node depotEnd) {
		Node[] route = new Node[routeWithoutDepots.length + 2];

		route[0] = depotStart;
		System.arraycopy(routeWithoutDepots, 0, route, 1, routeWithoutDepots.length);
		route[route.length - 1] = depotEnd;

		return route;
	}

	private Node[] mergeRoutes(Node[][] routeArr, int route1, int route2) {
		int routeLength1 = routeArr[route1].length;
		int routeLength2 = routeArr[route2].length;

		Node[] newRoute = new Node[routeLength1 + routeLength2];
		System.arraycopy(routeArr[route1], 0, newRoute, 0, routeLength1);
		System.arraycopy(routeArr[route2], 0, newRoute, routeLength1, routeLength2);
		routeArr[route1] = newRoute;

		return newRoute;		
	}

	private void createSavingsMatrix(Node depot, SavingsDataBag dataBag) {
		List<Node> nodeList = dataBag.getNodeList();
		int[] routeIdxForStartNode = dataBag.getRouteIdxForStartNode();
		int[] routeIdxForEndNode = dataBag.getRouteIdxForEndNode();

		for (int i = 0; i < nodeList.size(); i++) {
			Node src = nodeList.get(i);
			int srcIdx = src.getIdx();
			int srcRouteIdx = routeIdxForStartNode[srcIdx];
			float distADepot = getDistanceForOptimization(src, depot);

			for (int j = i + 1; j < nodeList.size(); j++) {
				Node dst = nodeList.get(j);
				int dstIdx = dst.getIdx();
				int dstRouteIdx = routeIdxForEndNode[dstIdx];
				
				if(srcRouteIdx == dstRouteIdx)
					continue;

				float dist = getDistanceForOptimization(src, dst);
				float distBDepot = getDistanceForOptimization(dst, depot);
				float saving = (distADepot + distBDepot) - lamda * dist;
				
				if(saving > 0) {
					dataBag.addSaving(srcIdx, dst.getIdx(), saving);
					dataBag.addSaving(dst.getIdx(), srcIdx, saving);
				}
			}
		}

		sort(dataBag.getSavingsMatrix(), 2);
	}

	/**
	 * Converts a list of lists into a giant route representation
	 * 
	 * @param routeArr
	 * @param depot
	 * @return giant route
	 */
	private Node[] buildGiantRoute(Node[][] routeArr, Node depot) {
		int maxId = 0;
		List<Node> giantList = new ArrayList<>();
		for (int i = 0; i < routeArr.length; i++) {
			if(routeArr[i] == null)
				continue;

			giantList.add(Util.createIdNode(depot, maxId++));
			for (int j = 0; j < routeArr[i].length; j++)
				giantList.add(routeArr[i][j]);

		}
		giantList.add(Util.createIdNode(depot, maxId++));

		return giantList.toArray(new Node[0]);
	}

	/**
	 * Creates the initial list of lists where each
	 * sub list is a route.
	 * 
	 * @param giantRoute
	 * @param model
	 * @return
	 */
	private Node[][] buildRoutes(Node[] giantRoute, XFVRPModel model) {
		int nbrOfCustomers = model.getNbrOfNodes() - model.getNbrOfDepots() - model.getNbrOfReplenish();
		Node[][] routeArr = new Node[nbrOfCustomers][1];

		int idx = 0;
		for (int i = 0; i < giantRoute.length; i++) {
			if(giantRoute[i].getSiteType() == SiteType.DEPOT)
				continue;
			if(giantRoute[i].getSiteType() == SiteType.REPLENISH)
				continue;

			List<Node> list = new ArrayList<>();
			for (int j = i; j < giantRoute.length; j++) {
				if(giantRoute[j].getSiteType() == SiteType.DEPOT)
					break;

				list.add(giantRoute[j]);
				i++;
			}
			routeArr[idx++] = list.toArray(new Node[0]);
		}

		return Arrays.copyOf(routeArr, idx);
	}
}
