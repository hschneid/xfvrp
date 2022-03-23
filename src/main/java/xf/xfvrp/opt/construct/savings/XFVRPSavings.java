package xf.xfvrp.opt.construct.savings;

import xf.xfvrp.base.*;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Contains the Savings optimization procedure with
 * acceptance value lambda = 1.
 *
 * @author hschneid
 *
 */
public class XFVRPSavings extends XFVRPOptBase {

	protected float lambda = 1;

	public XFVRPSavings() {
		super.isSplittable = true;
	}

	/**
	 * Executes the Savings routing
	 */
	@Override
	public Solution execute(Solution solution) throws XFVRPException {
		final Node depot = solution.getRoutes()[0][0];

		SavingsDataBag dataBag = buildRoutes(solution);

		prepare(dataBag);

		createSavingsMatrix(depot, dataBag, false);

		improve(depot, dataBag);

		handleOverhangingRoutes(depot, dataBag);

		return createSolution(depot, dataBag, solution.getModel());
	}

	private void prepare(SavingsDataBag dataBag) {
		Node[][] routes = dataBag.getRoutes();

		List<Node> nodeList = new ArrayList<>();
		int[] routeIdxForStartNode = new int[model.getNodes().length];
		int[] routeIdxForEndNode = new int[model.getNodes().length];

		Arrays.fill(routeIdxForStartNode, -1);
		Arrays.fill(routeIdxForEndNode, -1);

		// Trage f�r jeden Start- oder Ziel-Knoten ein, auf welcher Tour
		// er zu finden ist. Nat�rlich kann ein Knoten nur auf einer Tour sein.
		for (int i = 0; i < routes.length; i++) {
			if(routes[i] == null)
				continue;

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
	}

	private void improve(Node depot, SavingsDataBag dataBag) throws XFVRPException {
		boolean isImproved = true;
		while(isImproved) {
			isImproved = applyNextSaving(depot, dataBag);
		}
	}

	private void improveOverhangingRoutes(Node depot, SavingsDataBag dataBag) throws XFVRPException {
		boolean isImproved = true;
		while(hasOverhangingRoutes(depot, dataBag) && isImproved) {
			isImproved = applyNextSaving(depot, dataBag);
		}
	}

	private boolean applyNextSaving(Node depot, SavingsDataBag dataBag) throws XFVRPException {
		Node depotStart = Util.createIdNode(depot, 0);
		Node depotEnd = Util.createIdNode(depot, 1);

		List<float[]> savingsMatrix = dataBag.getSavingsMatrix();

		int savingsIdx = savingsMatrix.size() - 1;

		// Search for best valid saving
		for (int i = savingsIdx; i >= 0; i--) {
			savingsIdx--;
			float[] saving = savingsMatrix.get(i);

			int srcNodeIdx = (int)saving[0];
			int dstNodeIdx = (int)saving[1];

			if(!isSavingAvailable(srcNodeIdx, dstNodeIdx, dataBag))
				continue;

			// Merge
			Node[] coreRoute = mergeRoutes(srcNodeIdx, dstNodeIdx, dataBag);
			Node[] newRoute = addDepots(coreRoute, depotStart, depotEnd);

			// Check
			Solution smallSolution = new Solution(model);
			smallSolution.addRoute(newRoute);
			Quality q = check(smallSolution);

			if(q.getPenalty() == 0) {
				updateRoutes(coreRoute, srcNodeIdx, dstNodeIdx, dataBag);

				return true;
			}
		}

		return false;
	}

	private boolean isSavingAvailable(int srcNodeIdx, int dstNodeIdx, SavingsDataBag dataBag) {
		int[] routeIdxForStartNode = dataBag.getRouteIdxForStartNode();
		int[] routeIdxForEndNode = dataBag.getRouteIdxForEndNode();

		int routeIdxSrc = Math.max(routeIdxForStartNode[srcNodeIdx], routeIdxForEndNode[srcNodeIdx]);
		int routeIdxDst = Math.max(routeIdxForStartNode[dstNodeIdx], routeIdxForEndNode[dstNodeIdx]);

		// Beide Routen m�ssen noch zur Verf�gung stehen
		// ODER Wenn beide Knoten auf der selben Tour liegen,
		// ist eine Verkn�pfung nicht mehr m�glich
		return routeIdxSrc != -1 && routeIdxDst != -1 && routeIdxSrc != routeIdxDst;
	}

	private void updateRoutes(Node[] mergedRoute, int srcNodeIdx, int dstNodeIdx, SavingsDataBag dataBag) {
		Node[][] routes = dataBag.getRoutes();

		int[] routeIdxForStartNode = dataBag.getRouteIdxForStartNode();
		int[] routeIdxForEndNode = dataBag.getRouteIdxForEndNode();

		int routeIdxSrc = Math.max(routeIdxForStartNode[srcNodeIdx], routeIdxForEndNode[srcNodeIdx]);
		int routeIdxDst = Math.max(routeIdxForStartNode[dstNodeIdx], routeIdxForEndNode[dstNodeIdx]);

		int routeLengthSrc = routes[routeIdxSrc].length;
		int routeLengthDst = routes[routeIdxDst].length;

		// Aktualisiere die Datenstrukturen
		if(routeIdxForStartNode[srcNodeIdx] != -1) {
			routeIdxForStartNode[srcNodeIdx] = -1;
			routeIdxForStartNode[routes[routeIdxSrc][routeLengthSrc - 1].getIdx()] = routeIdxSrc;
			routeIdxForEndNode[routes[routeIdxSrc][routeLengthSrc - 1].getIdx()] = -1;
		} else {
			routeIdxForEndNode[srcNodeIdx] = -1;
		}

		if(routeIdxForStartNode[dstNodeIdx] != -1) {
			routeIdxForStartNode[dstNodeIdx] = -1;
			routeIdxForEndNode[routes[routeIdxDst][routeLengthDst - 1].getIdx()] = routeIdxSrc;
		} else {
			routeIdxForEndNode[dstNodeIdx] = -1;
			routeIdxForStartNode[routes[routeIdxDst][0].getIdx()] = -1;
			routeIdxForEndNode[routes[routeIdxDst][0].getIdx()] = routeIdxSrc;
		}

		// Die neue Route1
		routes[routeIdxSrc] = mergedRoute;

		// Route2 ist obsolet
		routes[routeIdxDst] = null;
	}

	private Node[] addDepots(Node[] routeWithoutDepots, Node depotStart, Node depotEnd) {
		Node[] route = new Node[routeWithoutDepots.length + 2];

		route[0] = depotStart;
		System.arraycopy(routeWithoutDepots, 0, route, 1, routeWithoutDepots.length);
		route[route.length - 1] = depotEnd;

		return route;
	}

	private Node[] mergeRoutes(int srcNodeIdx, int dstNodeIdx, SavingsDataBag dataBag) {
		Node[][] routes = dataBag.getRoutes();

		int[] routeIdxForStartNode = dataBag.getRouteIdxForStartNode();
		int[] routeIdxForEndNode = dataBag.getRouteIdxForEndNode();

		int routeIdxSrc = Math.max(routeIdxForStartNode[srcNodeIdx], routeIdxForEndNode[srcNodeIdx]);
		int routeIdxDst = Math.max(routeIdxForStartNode[dstNodeIdx], routeIdxForEndNode[dstNodeIdx]);

		int routeLengthSrc = routes[routeIdxSrc].length;
		int routeLengthDst = routes[routeIdxDst].length;

		Node[] r1 = Arrays.copyOf(routes[routeIdxSrc], routeLengthSrc);
		if(routeIdxForStartNode[srcNodeIdx] != -1)
			this.swap(r1, 0, r1.length - 1);

		Node[] r2 = Arrays.copyOf(routes[routeIdxDst], routeLengthDst);
		if(routeIdxForEndNode[dstNodeIdx] != -1)
			this.swap(r2, 0, r2.length - 1);

		// Concat both routes
		Node[] newRoute = new Node[r1.length + r2.length];
		System.arraycopy(r1, 0, newRoute, 0, r1.length);
		System.arraycopy(r2, 0, newRoute, r1.length, r2.length);
		return newRoute;
	}

	private void createSavingsMatrix(Node depot, SavingsDataBag dataBag, boolean acceptAllChanges) {
		dataBag.getSavingsMatrix().clear();
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
				float saving = (distADepot + distBDepot) - lambda * dist;

				if(saving > 0 || acceptAllChanges) {
					dataBag.addSaving(srcIdx, dst.getIdx(), saving);
					dataBag.addSaving(dst.getIdx(), srcIdx, saving);
				}
			}
		}

		sort(dataBag.getSavingsMatrix(), 2);
	}

	/**
	 * Creates the initial list of lists where each
	 * sub list is a route of customers. Each savings route contains only Customer nodes.
	 */
	private SavingsDataBag buildRoutes(Solution solution) {
		return new SavingsDataBag(
				Arrays.stream(solution.getRoutes())
						.map(route -> Arrays
								.stream(route)
								.filter(node -> node.getSiteType() == SiteType.CUSTOMER)
								.toArray(Node[]::new)
						)
						.filter(route -> route.length > 0)
						.toArray(Node[][]::new)
		);
	}

	private Solution createSolution(Node depot, SavingsDataBag dataBag, XFVRPModel model) {
		Solution newSolution = new Solution(model);

		int depotId = 0;
		for (int i = 0; i < dataBag.getRoutes().length; i++) {
			Node[] customers = dataBag.getRoutes()[i];
			if(customers != null) {
				Node[] route = new Node[customers.length + 2];

				route[0] = Util.createIdNode(depot, depotId++);
				System.arraycopy(customers, 0, route, 1, customers.length);
				route[route.length - 1] = Util.createIdNode(depot, depotId++);

				newSolution.addRoute(route);
			}
		}

		NormalizeSolutionService.normalizeRouteWithCleanup(newSolution);

		return newSolution;
	}

	private void handleOverhangingRoutes(Node depot, SavingsDataBag dataBag) {
		if(hasOverhangingRoutes(depot, dataBag)) {
			prepare(dataBag);

			createSavingsMatrix(depot, dataBag, true);

			improveOverhangingRoutes(depot, dataBag);
		}
	}

	private boolean hasOverhangingRoutes(Node depot, SavingsDataBag dataBag) {
		int nbrOfUsableRoutes = depot.getMaxNbrOfRoutes();
		for (int i = 0; i < dataBag.getRoutes().length; i++) {
			if(dataBag.getRoutes()[i] != null && dataBag.getRoutes()[i].length > 0)
				nbrOfUsableRoutes--;
			if(nbrOfUsableRoutes < 0)
				return true;
		}

		return false;
	}
}
