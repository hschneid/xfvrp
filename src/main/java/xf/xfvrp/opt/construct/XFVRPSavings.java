package xf.xfvrp.opt.construct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.XFVRPLPBridge;
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
	public Node[] execute(Node[] giantRoute) {		
		final Node depot = giantRoute[0];

		Node[][] routeArr = buildRouteLists(giantRoute, model);

		List<Node> nodeList = new ArrayList<>();
		int[] routeIdxForStartNode = new int[model.getNodeArr().length];
		int[] routeIdxForEndNode = new int[model.getNodeArr().length];
		{
			Arrays.fill(routeIdxForStartNode, -1);
			Arrays.fill(routeIdxForEndNode, -1);

			// Trage f�r jeden Start- oder Ziel-Knoten ein, auf welcher Tour
			// er zu finden ist. Nat�rlich kann ein Knoten nur auf einer Tour sein.
			for (int i = 0; i < routeArr.length; i++) {
				routeIdxForStartNode[routeArr[i][0].getIdx()] = i;
				routeIdxForEndNode[routeArr[i][routeArr[i].length - 1].getIdx()] = i;
				
				nodeList.add(routeArr[i][0]);
				if(routeArr[i][0] != routeArr[i][routeArr[i].length - 1])
					nodeList.add(routeArr[i][routeArr[i].length - 1]);
			}
		}

		// Erstelle Savings-Matrix
		List<float[]> savingsList = new ArrayList<>();
		{
			for (int i = 0; i < nodeList.size(); i++) {
				Node src = nodeList.get(i);
				int srcIdx = src.getIdx();
				float distADepot = getDistanceForOptimization(src, depot);

				for (int j = i + 1; j < nodeList.size(); j++) {
					Node dst = nodeList.get(j);
					
					float dist = getDistanceForOptimization(src, dst);
					float distBDepot = getDistanceForOptimization(dst, depot);
					float saving = (distADepot + distBDepot) - lamda * dist;
					if(saving > 0) {
						savingsList.add(new float[]{srcIdx, dst.getIdx(), 1000f/saving});
						savingsList.add(new float[]{dst.getIdx(), srcIdx, 1000f/saving});
					}
				}
			}

			// Sortiere Savings in aufsteigender Reihenfolge (Letzter Eintrag = h�chstes Saving)
			sort(savingsList, 2);
		}

		Node depotStart = Util.createIdNode(depot, 0);
		Node depotEnd = Util.createIdNode(depot, 1);

		int savingsIdx = savingsList.size() - 1;
		SEARCH: while(true) {
			// Bestimme das beste g�ltige Saving
			for (int i = savingsIdx; i >= 0; i--) {
				savingsIdx--;
				float[] saving = savingsList.get(i);
				
				int route1 = routeIdxForEndNode[(int) saving[0]];
				int route2 = routeIdxForStartNode[(int) saving[1]];

				// Beide Routen m�ssen noch zur Verf�gung stehen
				// ODER Wenn beide Knoten auf der selben Tour liegen,
				// ist eine Verkn�pfung nicht mehr m�glich
				if(route1 == -1 || route2 == -1 || route1 == route2)
					continue;

				// Baue neue Tour zusammen
				final int routeLength1 = routeArr[route1].length;
				final int routeLength2 = routeArr[route2].length;

				Node[] newRoute = new Node[routeLength1 + routeLength2 + 2];
				newRoute[0] = depotStart;
				System.arraycopy(routeArr[route1], 0, newRoute, 1, routeLength1);
				System.arraycopy(routeArr[route2], 0, newRoute, routeLength1 + 1, routeLength2);
				newRoute[newRoute.length - 1] = depotEnd;
				
				// Pr�fe die neue Tour
				Quality q = check(newRoute);

				if(q.getPenalty() == 0) {
					// Efficient Load Pr�fung (Achtung: Kein Footprint, da immer nur zwei Routen verkn�pft und bewertet werden!) 
					XFVRPLPBridge.check(newRoute, null, model, q);

					if(q.getPenalty() == 0) {
						// Aktualisiere die Datenstrukturen
						// Der Start von Route1 bleibt
						// Der Start von Route2 f�llt weg
						routeIdxForStartNode[routeArr[route2][0].getIdx()] = -1;
						// Das Ziel von Route1 f�llt weg
						routeIdxForEndNode[routeArr[route1][routeLength1 - 1].getIdx()] = -1;
						// Das Ziel von Route2 ist jetzt auf Route1
						routeIdxForEndNode[routeArr[route2][routeLength2 - 1].getIdx()] = route1;

						// Die neue Route1
						newRoute = new Node[routeLength1 + routeLength2];
						System.arraycopy(routeArr[route1], 0, newRoute, 0, routeLength1);
						System.arraycopy(routeArr[route2], 0, newRoute, routeLength1, routeLength2);
						routeArr[route1] = newRoute;

						// Route2 ist obsolet
						routeArr[route2] = null;

						continue SEARCH;
					}
				}
			}

			break;
		}

		return buildGiantRoute(routeArr, depot);
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
	private Node[][] buildRouteLists(Node[] giantRoute, XFVRPModel model) {
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
