package xf.xfvrp.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xf.xfvrp.base.Node;
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
public class Report {

	private final XFVRPModel model;
	
	private final ReportSummary summary = new ReportSummary();
	private final ErrorSummary errors = new ErrorSummary();
	private final List<RouteReport> reportList = new ArrayList<>();

	private final Set<Vehicle> vehicleSet = new HashSet<>();
	
	/* If XFLP uses isLoading constraints, the solution contains package events */
	private boolean hasPackageEvents = false;
	
	/**
	 * A Report is the structral representation of a route planning solution.
	 * 
	 * @param model The used data model for gaining this solution.
	 */
	public Report(Node[] route, XFVRPModel model) {
		this.model = model;
		errors.add(route);
	}

	/**
	 * A Report is the structral representation of a route planning solution.
	 * 
	 * @param model The used data model for gaining this solution.
	 */
	public Report(XFVRPModel model) {
		this.model = model;
	}
	
	/**
	 * 
	 * @return
	 */
	public Set<Vehicle> getVehicles() {
		return vehicleSet;
	}
	
	/* Add-Functions */

	/**
	 * 
	 */
	public void add(RouteReport route) {
		if(route.getSummary().getNbrOfEvents() > 0) {
			summary.add(route);
			reportList.add(route);
			vehicleSet.add(route.getVehicle());
		}
		hasPackageEvents |= route.hasPackageEvents();
	}

	/* GetFunctions-Functions */

	/**
	 * 
	 */
	public List<RouteReport> getRoutes() {
		return reportList;
	}

	/**
	 * 
	 * @return
	 */
	public ReportSummary getSummary() {
		return summary;
	}
	
	/**
	 * Returns the used planning model for obtaining this solution report.
	 * @return
	 */
	public XFVRPModel getModel() {
		return model;
	}
	
	/**
	 * Import the route reports of another report object into this report.
	 * 
	 * @param rep Another report object
	 */
	public void importReport(Report rep) {
		for (RouteReport tRep : rep.getRoutes())
			add(tRep);
		
		errors.importErrors(rep.getErrors());
	}
	
	/**
	 * If the report contains package events, because XFLP used isLoading constrains
	 * in the planning, then return value is true, otherwise false;
	 * 
	 * @return true if isLoading constraint was used by XFLP.
	 */
	public boolean hasPackageEvents() {
		return hasPackageEvents;
	}
	
	/**
	 * 
	 * @return
	 */
	public ErrorSummary getErrors() {
		return errors;
	}
}
