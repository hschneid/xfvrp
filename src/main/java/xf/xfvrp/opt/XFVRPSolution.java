package xf.xfvrp.opt;

import java.util.ArrayList;
import java.util.List;

import xf.xflp.report.ContainerReport;
import xf.xflp.report.LPPackageEvent;
import xf.xflp.report.LPReport;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.PackageEvent;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * The solution of the planning is stored in an
 * XFVRPSolution instance. The final giant tour
 * and the allocated model are stored.
 * 
 * @author hschneid
 *
 */
public class XFVRPSolution {

	protected Solution solution;
	protected XFVRPModel model;
	protected final Vehicle vehicle;

	private float time = 0;
	private float delay = 0;
	private float drivingTime = 0;

	/**
	 * 
	 * @param giantRoute
	 * @param model
	 */
	public XFVRPSolution(Solution solution, XFVRPModel model) {
		this.solution = solution;
		this.model = model;
		this.vehicle = model.getVehicle();
	}

	/**
	 * 
	 * @return
	 */
	public Report getReport() {
		Report rep = new Report(solution, model);
		
		Node[] giantRoute = solution.getGiantRoute();

		// Wenn die Giant Route Kunden enthält
		if(findNextCustomer(giantRoute, 0) != -1) {
			final float[][] routeDepotServiceMap = createRouteDepotServiceMap(giantRoute);

			Node lastNode = giantRoute[0];
			Node currNode;

			delay = 0;
			drivingTime = 0;

			Node lastDepot = lastNode;

			RouteReport tRep = createNewRoute(lastNode, giantRoute[findNextCustomer(giantRoute, 0)], routeDepotServiceMap);

			List<RouteReport> routeReports = new ArrayList<>();
			boolean routeHasCustomers = false;
			for (int i = 1; i < giantRoute.length; i++) {
				currNode = giantRoute[i];

				if(currNode == null)
					throw new IllegalStateException("customer is null at position in giant tour: "+i);

				// Der letzte Knoten ist ein DEPOT gewesen
				if(currNode.getSiteType() == SiteType.DEPOT) {
					// Bewerte Kante zu�ck zum Ausgangsdepot in lastDepot
					eval(lastNode, lastDepot, tRep, routeDepotServiceMap);

					// Beende alte Tour durch Einf�gen in Report und Zur�cksetzen der laufenden Variabeln
					if(routeHasCustomers) {
						rep.add(tRep);
						routeReports.add(tRep);
					}

					delay = 0;
					drivingTime = 0;
					routeHasCustomers = false;

					// Beginne eine neue Tour, leere Touren werden hier �bersprungen
					i = skipToNextDepot(i, giantRoute);
					currNode = giantRoute[i];

					// Wenn es noch weitere Kunden gibt, erzeuge weitere RouteReports.
					int nextCustomerIdx = findNextCustomer(giantRoute, i);
					nextCustomerIdx = (nextCustomerIdx == -1) ? i : nextCustomerIdx;
					tRep = createNewRoute(currNode, giantRoute[nextCustomerIdx], routeDepotServiceMap);

					lastDepot = currNode;
				} else {
					// Bewerte Schritt vom letzten Knoten zu diesem Knoten
					eval(lastNode, currNode, tRep, null);
					if(currNode.getSiteType() == SiteType.CUSTOMER)
						routeHasCustomers = true;
				}

				lastNode = currNode;
			}

			// Loading plan
			addLoadingPlan(routeReports);
		}

		return rep;
	}

	/**
	 * Evaluates the edge from lastNode to currNode and
	 * updates the progressive values.
	 * 
	 * @param lastNode the predecessor of currNode in giant tour
	 * @param currNode the successor of lastNode in giant tour
	 * @param tRep the current tour report with progressive values
	 * @param routeDepotServiceMap 
	 */
	private void eval(Node lastNode, Node currNode, RouteReport tRep, float[][] routeDepotServiceMap) {
		// Der letzte Knoten ist ein KUNDE gewesen
		// Tour fortführen
		float[] metric = model.getDistanceAndTime(lastNode, currNode);
		time += metric[1];
		drivingTime += metric[1];

		// Driving time restriction
		// A pause of waitingTimeBetweenShifts minutes after maxDrivingTimePerShift minutes
		if(drivingTime >= tRep.getVehicle().maxDrivingTimePerShift) {
			drivingTime = 0;
			time += tRep.getVehicle().waitingTimeBetweenShifts;

			Node waitingNode = currNode.copy();
			waitingNode.setSiteType(SiteType.PAUSE);
			Event e = new Event(waitingNode); // Driver Pause
			e.setDuration(tRep.getVehicle().waitingTimeBetweenShifts);
			e.setLoadType(LoadType.PAUSE);
			tRep.add(e);
		}

		// Depot service time at end depot
		float depotServiceTime = 0;
		if(model.getParameter().isWithUnloadingTimeAtDepot() && currNode.getSiteType() == SiteType.DEPOT) {
			depotServiceTime = routeDepotServiceMap[currNode.getDepotId()][1];
			time += depotServiceTime; 
		}

		float[] demand = currNode.getDemand();
		Event e = new Event(currNode);
		e.setAmount(demand[0]);
		if (demand.length > 1) e.setAmount2(demand[1]);
		if (demand.length > 2) e.setAmount3(demand[2]);
		e.setDistance(metric[0]);
		e.setTravelTime(metric[1]);
		e.setArrival(time);

		delay += Math.max(0, - currNode.getTimeWindow(time)[1]);
		float waitingTime = (lastNode.getSiteType() != SiteType.DEPOT) ? Math.max(0, currNode.getTimeWindow(time)[0] - time) : 0;
		float serviceTime = (metric[0] == 0) ? currNode.getServiceTime() : currNode.getServiceTime() + currNode.getServiceTimeForSite();
		time = Math.max(time, currNode.getTimeWindow(time)[0]);

		time += serviceTime;

		e.setDeparture(time);
		e.setService(serviceTime + depotServiceTime);
		e.setWaiting(waitingTime);
		e.setLoadType(currNode.getLoadType());
		e.setDelay(delay);
		e.setDuration(e.getTravelTime() + waitingTime + e.getService());

		tRep.add(e);
	}

	/**
	 * Creates a new route and a new route report.
	 * 
	 * All variables are initialized.
	 * 
	 * @param depotNode Current depot node
	 * @param routeDepotServiceMap 
	 * @return Route report
	 */
	private RouteReport createNewRoute(Node depotNode, Node nextNode, float[][] routeDepotServiceMap) {
		if (depotNode.getSiteType() != SiteType.DEPOT)
			throw new IllegalStateException("Found unexpected site type for depot ("+depotNode.getSiteType().toString()+")");
		
		final float depotOpeningTime = depotNode.getTimeWindow(0)[0];

		RouteReport tRep = new RouteReport(vehicle);

		float nextNodeTime = 0;
		if(nextNode != null)
			nextNodeTime = model.getTime(depotNode, nextNode);

		float loadingTime = 0;
		if(model.getParameter().isWithLoadingTimeAtDepot())
			loadingTime = routeDepotServiceMap[depotNode.getDepotId()][0];

		time = Math.max(
				depotOpeningTime + loadingTime,
				nextNode.getTimeWindow(time)[0] - nextNodeTime
				);

		Event e = new Event(depotNode);
		e.setAmount(0);
		e.setAmount2(0);
		e.setAmount3(0);
		e.setDistance(0);
		e.setArrival(depotOpeningTime);
		e.setDeparture(time);
		e.setService(loadingTime);
		e.setWaiting(time - depotOpeningTime - loadingTime);
		e.setDelay(0);
		e.setLoadType(null);
		e.setTravelTime(0);
		tRep.add(e);
		return tRep;
	}

	/**
	 * Skippes depot nodes if empty tours are available.
	 * 
	 * @param pos Position of the last found depot 
	 * @param giantRoute Current giant tour
	 * @return Position of the next valid depot
	 */
	private int skipToNextDepot(int pos, Node[] giantTour) {
		int nextDepot = pos;
		for (int j = pos + 1; j < giantTour.length; j++) {
			if(giantTour[j].getSiteType() == SiteType.DEPOT)
				nextDepot = j;
			else
				break;
		}
		return nextDepot;
	}

	/**
	 * 
	 * @param giantRoute
	 * @param pos
	 * @return
	 */
	private int findNextCustomer(Node[] giantRoute, int pos) {
		for (int i = pos + 1; i < giantRoute.length; i++) {
			if(giantRoute[i].getSiteType() == SiteType.CUSTOMER)
				return i;
		}

		return -1;
	}

	/**
	 * 
	 * @param routeReports
	 */
	protected void addLoadingPlan(List<RouteReport> routeReports) {
		if(model.getParameter().isWithLoadPlanning()) {
			int routeIdx = 0;
			
			LPReport[] lpReports = XFVRPLPBridge.getLoadingPlan(solution, model);
			
			// Create a loading plan for each route with the package planning
			// of the LP-Solver
			for (LPReport lpReport : lpReports) {
				RouteReport routeReport = routeReports.get(routeIdx);

				// VALIDS
				for (ContainerReport conRep : lpReport) {
					for (LPPackageEvent pe : conRep) {
						PackageEvent e = new PackageEvent();

						e.setId(pe.getId());
						e.setX(pe.getX());
						e.setY(pe.getY());
						e.setZ(pe.getZ());
						e.setUsedVolumeInContainer(pe.getUsedVolumeInContainer());
						e.setInvalid(false);

						routeReport.add(e);
					}
				}

				// INVALIDS
				for (LPPackageEvent unplannedPackage : lpReport.getUnplannedPackages()) {
					PackageEvent e = new PackageEvent();

					e.setId(unplannedPackage.getId());
					e.setX(unplannedPackage.getX());
					e.setY(unplannedPackage.getY());
					e.setZ(unplannedPackage.getZ());
					e.setUsedVolumeInContainer(0);
					e.setInvalid(false);

					routeReport.add(e);
				}

				routeIdx++;
			}
		}
	}

	/**
	 * 
	 * @param giantRoute
	 * @param model 
	 * @return
	 */
	private float[][] createRouteDepotServiceMap(Node[] giantRoute) {
		if(model.getParameter().isWithLoadingTimeAtDepot() || model.getParameter().isWithUnloadingTimeAtDepot()) {
			int maxDepotId = 0;
			for (int i = 1; i < giantRoute.length; i++)
				maxDepotId = Math.max(maxDepotId, giantRoute[i].getDepotId());
			float[][] list = new float[maxDepotId + 1][];

			float pickupServiceTime = 0;
			float deliverySericeTime = 0;

			int lastDepotId = giantRoute[0].getDepotId();
			for (int i = 1; i < giantRoute.length; i++) {
				final SiteType siteType = giantRoute[i].getSiteType();
				if(siteType == SiteType.DEPOT) {
					list[lastDepotId] = new float[]{deliverySericeTime, pickupServiceTime};
					pickupServiceTime = deliverySericeTime = 0;
					lastDepotId = giantRoute[i].getDepotId();
				} else if(siteType == SiteType.CUSTOMER) {
					final LoadType loadType = giantRoute[i].getLoadType();

					if(loadType == LoadType.PICKUP)
						pickupServiceTime += giantRoute[i].getServiceTime(); 
					else if(loadType == LoadType.DELIVERY)
						deliverySericeTime += giantRoute[i].getServiceTime();
					else
						throw new IllegalStateException("Found unexpected load type ("+loadType.toString()+")");
				} else
					throw new IllegalStateException("Found unexpected site type ("+siteType.toString()+")");
			}
			return list;
		}

		return null;
	}

	/**
	 * 
	 * @return
	 */
	public XFVRPModel getModel() {
		return model;
	}
}
