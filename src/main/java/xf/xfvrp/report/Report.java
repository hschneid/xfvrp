package xf.xfvrp.report;

import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 
 * Copyright (c) 2012-2022 Holger Schneider
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
	
	/**
	 * A Report is the structral representation of a route planning solution.
	 *
	 */
	public Report(Solution solution) {
		this.model = solution.getModel();
		errors.add(solution);
	}

	/**
	 * A Report is the structral representation of a route planning solution.
	 * 
	 * @param model The used data model for gaining this solution.
	 */
	public Report(XFVRPModel model) {
		this.model = model;
	}
	
	public Set<Vehicle> getVehicles() {
		return vehicleSet;
	}
	
	/* Add-Functions */

	public void add(RouteReport route) {
		if(route.getSummary().getNbrOfEvents() > 0) {
			summary.add(route);
			reportList.add(route);
			vehicleSet.add(route.getVehicle());
		}
	}

	/* GetFunctions-Functions */

	public List<RouteReport> getRoutes() {
		return reportList;
	}

	public ReportSummary getSummary() {
		return summary;
	}
	
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
	
	public ErrorSummary getErrors() {
		return errors;
	}
}
