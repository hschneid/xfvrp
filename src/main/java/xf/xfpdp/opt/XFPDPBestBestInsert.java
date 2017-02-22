package xf.xfpdp.opt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xf.xfpdp.XFPDPUtils;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.opt.CheckMethod;
import xf.xfvrp.opt.XFVRPOptBase;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Contains the optimization algorithms
 * for 2-opt
 * 
 * @author hschneid
 *
 */
public class XFPDPBestBestInsert extends XFVRPOptBase {

	private CheckMethod checker = new CheckMethod();
	private int maxDepotId = 1;
	
	private int nbrOfUnplannedShipments;
	private int maxShipmentIdx;

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Node[] execute(Node[] giantTour) {
		Node[] nodeArr = model.getNodeArr();
		Node depot = nodeArr[0];
		Node[][] shipments = getShipments(nodeArr);
		nbrOfUnplannedShipments = shipments.length;

		Quality bestResult = new Quality(null);
		bestResult.addCost(Float.MAX_VALUE);

		Node[] route = new Node[] {Util.createIdNode(depot, maxDepotId++)};
		route = buildRoot(depot, route, shipments);

		while(nbrOfUnplannedShipments > 0) {
			route = expandRoute(route, depot);

			int  bestShipment = -1;
			float[] bestMove = null;
			float bestImprovement = 0;

			for (int i = 0; i < shipments.length; i++) {
				if(shipments[i] == null)
					continue;

				route[route.length - 3] = shipments[i][0];
				route[route.length - 2] = shipments[i][1];

				Quality currentQ = checker.check(route, model);

				List<float[]> localImpList = new ArrayList<>();
				searchSingleDepot(route, localImpList);

				sort(localImpList, 4);

				Node[] copy = new Node[route.length];
				int cnt = 0;
				for (float[] val : localImpList) {
					if(cnt++ == 50)
						break;
					
					int srcA = (int) val[0];
					int srcB = (int) val[1];
					int dstA = (int) val[2];
					int dstB = (int) val[3];

					System.arraycopy(route, 0, copy, 0, route.length);
					XFPDPUtils.move(route, srcA, srcB, dstA, dstB);

					Quality newQ = checker.check(route, model);

					System.arraycopy(copy, 0, route, 0, route.length);

					if(newQ != null && newQ.getFitness() < currentQ.getFitness()) {
						if(currentQ.getFitness() - newQ.getFitness() > bestImprovement) {
							bestImprovement = currentQ.getFitness() - newQ.getFitness();
							bestShipment = i;
							bestMove = val;
						}
						break;
					}
				}
			}
			
			if(bestShipment >= 0) {
				route[route.length - 3] = shipments[bestShipment][0];
				route[route.length - 2] = shipments[bestShipment][1];
				shipments[bestShipment] = null;
				nbrOfUnplannedShipments--;
				XFPDPUtils.move(route, (int)bestMove[0], (int)bestMove[1], (int)bestMove[2], (int)bestMove[3]);
				route = Util.normalizeRoute(route, model);
			} else {
				route = reduceRoute(route);
				route = buildRoot(depot, route, shipments);
			}
			
			Quality newQ = checker.check(route, model);
			System.out.println(nbrOfUnplannedShipments+" "+newQ+" "+Arrays.toString(route));
		}

		return Util.normalizeRoute(route, model);
	}

	private Node[] reduceRoute(Node[] route) {
		return Arrays.copyOf(route, route.length - 3);
	}

	private Node[] expandRoute(Node[] route, Node depot) {
		// Expand by 3 fields (pickup, delivery, last depot)
		Node[] newRoute = new Node[route.length + 3];
		System.arraycopy(route, 0, newRoute, 0, route.length);

		// Add depot at last position
		newRoute[newRoute.length - 1] = Util.createIdNode(depot, maxDepotId++);

		return newRoute;
	}

	/**
	 * 
	 * @param depot
	 * @param shipments
	 * @return
	 */
	public Node[] buildRoot(Node depot, Node[] route, Node[][] shipments) {
		route = expandRoute(route, depot);

		Quality best = new Quality(null);
		best.addCost(Float.MAX_VALUE);
		int bestShipIdx = -1;

		for (int i = 0; i < shipments.length; i++) {
			if(shipments[i] == null)
				continue;
			
			route[route.length - 3] = shipments[i][0];
			route[route.length - 2] = shipments[i][1];

			Quality q = checker.check(route, model);
			if(q != null && q.getFitness() < best.getFitness()) {
				best = q;
				bestShipIdx = i;
			}
		}

		route[route.length - 3] = shipments[bestShipIdx][0];
		route[route.length - 2] = shipments[bestShipIdx][1];
		shipments[bestShipIdx] = null;
		nbrOfUnplannedShipments--;

		return route;
	}

	/**
	 * 
	 * @param nodeArr
	 * @return
	 */
	private Node[][] getShipments(Node[] nodeArr) {
		Map<Integer, Node[]> map = new HashMap<>();
		for (int i = 1; i < nodeArr.length; i++) {
			int shipIdx = nodeArr[i].getShipmentIdx();
			if(!map.containsKey(shipIdx))
				map.put(shipIdx, new Node[2]);

			Node[] p = map.get(shipIdx);

			if(nodeArr[i].getDemand()[0] > 0)
				p[0] = nodeArr[i];
			else
				p[1] = nodeArr[i];
			
			maxShipmentIdx = Math.max(maxShipmentIdx, shipIdx);
		}

		maxShipmentIdx++;
		
		return map.values().toArray(new Node[map.size()][2]);
	}

	/**
	 * Searches all improving valid steps in search space for a VRP with one depot.
	 * 
	 * @param giantRoute
	 * @param improvingStepList
	 */
	private void searchSingleDepot(Node[] route, List<float[]> improvingStepList) {
		int[] routeIdxArr = new int[route.length];
		int id = 0;
		for (int i = 1; i < route.length; i++) {
			if(route[i].getSiteType() == SiteType.DEPOT)
				id++;
			routeIdxArr[i] = id;
		}

		int[] shipmentPairIdxArr = new int[route.length];
		{
			int[][] shipmentsArr = new int[maxShipmentIdx][2];
			for (int i = 1; i < route.length; i++) {
				if(route[i].getSiteType() == SiteType.DEPOT)
					continue;

				int loadTypeIdx = (route[i].getDemand()[0] > 0) ? 0 : 1;

				shipmentsArr[route[i].getShipmentIdx()][loadTypeIdx] = i;
			}
			for (int i = 1; i < route.length; i++) {
				if(route[i].getSiteType() == SiteType.DEPOT)
					continue;

				int loadTypeIdx = (route[i].getDemand()[0] < 0) ? 0 : 1;

				shipmentPairIdxArr[i] = shipmentsArr[route[i].getShipmentIdx()][loadTypeIdx];
			}
		}

		// Search all improving steps in neighborhood
		{
			int srcA = route.length - 3;
			int srcB = route.length - 2;

			for (int dstA = 0; dstA < route.length - 3; dstA++) {
				for (int dstB = dstA; dstB < route.length - 3; dstB++) {
					if(routeIdxArr[dstA] != routeIdxArr[dstB])
						break;

					float val = 0;

					// Destination pointer must not be at Source pointer
					if((srcA - dstA) * (srcB - dstB) * (srcA - dstB) * (srcB - dstA) == 0)
						continue;

					// Einfache Opteration:
					// Schiebe eine Sendung (P & D) nach rechts. Es wird immer nach einem
					// Zielpunkt eingef�gt. Daher soll srcA ungleich (dstA & dstA - 1) sein UND srcB ungleich (dstB & dstB - 1).
					// Da B immer nach A sein muss, muss dstB gr��er dstA sein.
					// Verschiedene Sachen noch ungel�st:
					//  - Verschieben nach links
					//  - srcA und srcB werden mit dstA und dstB ineinander gewurschtelt
					//  - A und B liegen jetzt direkt nebeneinander

					val -= getDistanceForOptimization(route[srcA - 1], route[srcA]);
					val -= getDistanceForOptimization(route[srcA], route[srcA + 1]);
					val -= getDistanceForOptimization(route[srcB], route[srcB + 1]);
					if(srcB - srcA != 1)
						val -= getDistanceForOptimization(route[srcB - 1], route[srcB]);
					if(srcA - dstA != 1 && srcB - dstA != 1)
						val -= getDistanceForOptimization(route[dstA], route[dstA + 1]);
					if(srcA - dstB != 1 && srcB - dstB != 1 && dstA != dstB)
						val -= getDistanceForOptimization(route[dstB], route[dstB + 1]);


					int offset = 1;
					val += getDistanceForOptimization(route[dstA], route[srcA]);
					if(dstA != dstB) {
						offset = (srcB - dstA == 1) ? 2 : (srcA - dstA == 1 && srcB - srcA == 1) ? 3 : 1;
						val += getDistanceForOptimization(route[srcA], route[dstA + offset]);
						val += getDistanceForOptimization(route[dstB], route[srcB]);
					} else
						val += getDistanceForOptimization(route[srcA], route[srcB]);

					offset = (srcA - dstB == 1 || srcB - dstB == 1) ? (srcA - dstB == 1 && srcB - srcA == 1) ? 3 : 2: 1;
					val += getDistanceForOptimization(route[srcB], route[dstB + offset]);

					// Wenn srcA wirklich verschoben wird (dstA also nicht dahinter ist)
					if(srcA - dstA != 1) {
						// Wenn srcA und srcB direkt nebeneinander liegen, dann �berspringe srcB
						offset = (srcB - srcA == 1) ? 2 : 1;
						val += getDistanceForOptimization(route[srcA - 1], route[srcA + offset]);
					}
					// Wenn nicht gilt, dass srcB vor dstA eingef�gt wird oder
					// srcA vor dstA eingef�gt wird und srcA und srcB hintereinander liegen
					if(srcB - srcA != 1 && ! (srcB - dstA == 1 || (srcA - dstA == 1 && srcB - srcA == 1)))
						val += getDistanceForOptimization(route[srcB - 1], route[srcB + 1]);

					if(val < -epsilon)
						improvingStepList.add(new float[]{srcA, srcB, dstA, dstB, -val});
				}
			}
		}
	}
}
