package xf.xfvrp.opt.construct;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


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
	 */
	@Override
	public Solution execute(Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		final Node depot = giantRoute[0];

		SavingsDataBag dataBag = buildRoutes(giantRoute);

		prepare(dataBag);

		createSavingsMatrix(depot, dataBag);

		improve(depot, dataBag);

		Solution newSolution = new Solution();
		newSolution.setGiantRoute(buildGiantRoute(dataBag, depot));
		return newSolution;
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

	private void improve(Node depot, SavingsDataBag dataBag) {
		boolean isImproved = true;
		while(isImproved) {
			isImproved = applyNextSaving(depot, dataBag);
		}
	}

	private boolean applyNextSaving(Node depot, SavingsDataBag dataBag) {
		Node depotStart = Util.createIdNode(depot, 0);
		Node depotEnd = Util.createIdNode(depot, 1);

		List<float[]> savingsMatrix = dataBag.getSavingsMatrix();

		int savingsIdx = savingsMatrix.size() - 1;

		// Bestimme das beste gültige Saving
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
			Solution smallSolution = new Solution();
			smallSolution.setGiantRoute(newRoute);
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

		return Stream
				.concat(Arrays.stream(r1), Arrays.stream(r2))
				.toArray(Node[]::new);
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
	 */
	private Node[] buildGiantRoute(SavingsDataBag dataBag, Node depot) {
		Node[][] routeArr = dataBag.getRoutes();

		int maxId = 0;
		List<Node> giantList = new ArrayList<>();
		for (int i = 0; i < routeArr.length; i++) {
			if (routeArr[i] == null)
				continue;

			giantList.add(Util.createIdNode(depot, maxId++));
			giantList.addAll(Arrays.asList(routeArr[i]));

		}
		giantList.add(Util.createIdNode(depot, maxId));

		return giantList.toArray(new Node[0]);
	}

	/**
	 * Creates the initial list of lists where each
	 * sub list is a route.
	 */
	private SavingsDataBag buildRoutes(Node[] giantRoute) {
		SavingsDataBag dataBag = new SavingsDataBag();

		Node[][] routeArr = new Node[giantRoute.length][];

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

		dataBag.setRoutes(Arrays.copyOf(routeArr, idx));

		return dataBag;
	}
}
