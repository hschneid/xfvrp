package xf.xfvrp.opt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xf.xflp.XFLP;
import xf.xflp.report.LPReport;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class XFVRPLPBridge {

	@SuppressWarnings("unused")
	private static int lifoImportance = 1;

	/**
	 * This method checks the 3D loading restrictions.
	 * 
	 * @param giantRoute The routes in giant route representation
	 * @param footprint Set of already evaluated routes. Only if changes are determined, routes are re-evaluated. If this value is null, then all routes are evaluated. 
	 * @param model
	 * @param q The current quality of the whole giant route. This method only adds penalties to this quality object.
	 */
	public static void check(Solution giantRoute, Set<String> footprint, XFVRPModel model, Quality q) {
		if(!model.getParameter().isWithLoadPlanning())
			return;

		// Adjustment of the lifo importance, if user unchecked it.
		lifoImportance = (model.getParameter().isWithoutLifoInLoadPlanning()) ? 0 : 1;

		for (List<Node> route : splitRoutes(giantRoute)) {
			
			// Wenn diese Tour unverändert gegenüber einer bekannten Lösung
			// ist, dann bewerte diese auch nicht nochmal neu.
			if(footprint != null && footprint.contains(getFootprint(route)))
				continue;
			
			XFLP lp = new XFLP();
			
			// Transform vehicle into LP model
			Vehicle veh = model.getVehicle();
			lp.addContainer()
			.setHeight(veh.heightOfVesselFirst)
			.setLength(veh.lengthOfVesselFirst)
			.setWidth(veh.widthOfVesselFirst)
			.setMaxWeight(veh.capacityOfVesselFirst);

			// Transform route into LP model
			getTours(route, model, lp);
			
			// Execute planning
			lp.executeLoadPlanning();
			
			// Evaluate report for unpacked items
			LPReport rep = lp.getReport();
			q.addPenalty(rep.getSummary().getNbrOfNotLoadedPackages(), Quality.PENALTY_REASON_EFFLOAD);
		}
	}

	/**
	 * This method creates loading plan for each route of the given giant route. A loading plan
	 * describes the specific 3D locations of the items in a loading vessel.
	 * 
	 * @param giantRoute The routes in giant route representation
	 * @param model The data model to the giant route
	 * @return A set of reports to loading plans
	 */
	public static LPReport[] getLoadingPlan(Solution giantRoute, XFVRPModel model) {
		if(!model.getParameter().isWithLoadPlanning())
			return null;

		// Adjustment of the lifo importance, if user unchecked it.
		lifoImportance = (model.getParameter().isWithoutLifoInLoadPlanning()) ? 0 : 1;

		List<List<Node>> routes = splitRoutes(giantRoute);
		LPReport[] lpReports = new LPReport[routes.size()];
		for (int i = 0; i < routes.size(); i++) {
			List<Node> route = routes.get(i);
			
			XFLP lp = new XFLP();
			
			// Transform vehicle into LP model
			Vehicle veh = model.getVehicle();
			lp.addContainer()
			.setHeight(veh.heightOfVesselFirst)
			.setLength(veh.lengthOfVesselFirst)
			.setWidth(veh.widthOfVesselFirst)
			.setMaxWeight(veh.capacityOfVesselFirst);

			// Transform route into LP model
			getTours(route, model, lp);
			
			// Execute planning
			lp.executeLoadPlanning();
			
			// Evaluate report for unpacked items
			lpReports[i] = lp.getReport();
		}
		
		return lpReports;
	}

	/**
	 * Builds a footprint for each route of the giant route. A footprint
	 * is a unique string representation of a route.
	 * 
	 * @param giantTour The routes in giant route representation
	 * @return Set of unique string representations
	 */
	public static Set<String> getFootprints(Solution giantTour) {
		Set<String> routeFootprint = new HashSet<>();

		List<List<Node>> routes = splitRoutes(giantTour);
		for (List<Node> list : routes)
			routeFootprint.add(getFootprint(list));

		return routeFootprint;
	}

	/**
	 * Build the footprint of a route, which is the string
	 * concatenation of the extern ids of the nodes.
	 * 
	 * @param list - List of the nodes of one route
	 * @return Unique identifying string
	 */
	private static String getFootprint(List<Node> list) {
		StringBuilder sb = new StringBuilder();
		for (Node node : list)
			sb.append(node.getExternID()+"#");

		return sb.toString();
	}

	/**
	 * Splits the giant route into several routes. The giant route is
	 * cut at the depot nodes. The depot nodes are exkluded from the result.
	 * 
	 * @param giantRoute The routes in giant route representation
	 * @return List of node lists, where each node list has all nodes of one route
	 */
	private static List<List<Node>> splitRoutes(Solution solution) { 
		List<List<Node>> routes = new ArrayList<>();
		
		// Cut giant route into separated routes
		// The cut token is the depot (site type)
		// Depot nodes are exkluded.
		
		List<Node> list = new ArrayList<>();
		Node[] giantTour = solution.getGiantRoute();
		for (int i = 1; i < giantTour.length; i++) {
			Node node = giantTour[i];

			if(node.getSiteType() == SiteType.DEPOT) { 
				if(list.size() > 0) {
					routes.add(list);
					list = new ArrayList<>();
				}
			} else 
				list.add(node);
		}

		return routes;
	}

	/**
	 * 
	 * @param giantRoute
	 * @param model
	 * @return
	 */
	private static void getTours(List<Node> route, XFVRPModel model, XFLP lp) {
		// Location idx of route start is 0
		int startDepotIdx = 0;
		// Location idx of route end is number of nodes + 1
		int endDepotIdx = route.size() + 1;

		// Current location idx for nodes in route
		int currentNodeIdx = 1;

		Node currentNode = route.get(0);
		for (int i = 0; i < route.size(); i++) {
			Node node = route.get(i);

			// Wenn zwei Aufträge am selben Knoten statt finden,
			// ändert sich der LocationIdx nicht.
			if(model.getDistance(currentNode, node) > 0)
				currentNodeIdx++;

			int loading, unloading;
			if(node.getLoadType() == LoadType.DELIVERY) {
				loading = startDepotIdx;
				unloading = currentNodeIdx;
			} else {
				loading = currentNodeIdx;
				unloading = endDepotIdx;
			}

			for (int j = 0; j < node.getNbrOfPackages(); j++) {
				lp.addItem()
				.setExternID(node.getExternID()+"-"+j)
				.setHeight(node.getHeightOfPackage())
				.setWidth(node.getWidthOfPackage())
				.setLength(node.getLengthOfPackage())
				.setWeight(node.getWeightOfPackage())
				.setStackingWeightLimit(node.getLoadBearingOfPackage())
				.setStackingGroup(String.valueOf(node.getStackingGroupOfPackage()))
				.setLoadingLocation(String.valueOf(loading))
				.setUnloadingLocation(String.valueOf(unloading));
			}

			currentNode = node;
		}
	}
}
