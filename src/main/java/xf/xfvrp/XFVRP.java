package xf.xfvrp;

import java.util.ArrayList;
import java.util.List;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.xfvrp.XFVRP_Parameter;
import xf.xfvrp.opt.FullRouteMixedFleetHeuristic;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.XFVRPOptSplitter;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.opt.XFVRPSolution;
import xf.xfvrp.opt.init.ModelBuilder;
import xf.xfvrp.opt.init.precheck.PreCheckException;
import xf.xfvrp.opt.init.precheck.PreCheckService;
import xf.xfvrp.opt.init.solution.InitialSolutionBuilder;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.build.ReportBuilder;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * XFVRP is central user interface for this suite. 
 * It combines all methods for data import, optimization execution, parameters and
 * retrieval of solutions.
 * 
 * The modeling of this class represents a state machine, where iteratively several 
 * methods must be called. The execution method take all inserted data and parameters
 * and start the optimizers.
 * 
 * 
 * @author hschneid
 * 
 */
public class XFVRP extends XFVRP_Parameter {

	/* List of optimization procedures */
	private List<XFVRPOptBase> optList = new ArrayList<>();

	/* Last model for the last created solution */
	private XFVRPModel lastModel;

	/* Solutions - List of generated solutions per each vehicle type*/
	private final List<XFVRPSolution> vehicleSolutionList = new ArrayList<>();

	/**
	 * Calculates the VRP with the before inserted data
	 * by addDepot(), addCustomer(), addMetric() and 
	 * addVehicle() or the parameters setCapacity() and setMaxRouteDuration()
	 * @throws PreCheckException 
	 */
	public void executeRoutePlanning() throws PreCheckException {
		statusManager.fireMessage(StatusCode.RUNNING, "XFVRP started");
		statusManager.setStartTime();

		// Flush import buffer
		importer.finishImport();
		vehicleSolutionList.clear();

		// Copy imported data to internal data structure
		Vehicle[] vehicles = importer.getVehicles();
		Node[] nodes = importer.getNodes(vehicles, statusManager);

		// Check of input parameter
		if(importer.getDepotList().size() == 0) {
			statusManager.fireMessage(StatusCode.ABORT, "No depot is given.");
			throw new IllegalArgumentException("No depot is given.");
		}
		if(vehicles.length == 0) {
			statusManager.fireMessage(StatusCode.ABORT, "No vehicle information were set.");
			throw new IllegalArgumentException("No vehicle information were set.");
		}

		vehicleSolutionList.addAll(
				new FullRouteMixedFleetHeuristic().execute(
						nodes,
						vehicles,
						(dataBag) -> {
							try {
								return executeRoutePlanning(dataBag);
							} catch (PreCheckException e) {
								e.printStackTrace();
							}
							return null;
						},
						metric,
						parameter,
						statusManager)
				);


		statusManager.fireMessage(StatusCode.FINISHED, "XFVRP finished sucessfully.");
	}

	/**
	 * Calculates a single vehicle VRP for a given vehicle with all
	 * announced optimization procedures.
	 * 
	 * @param depotList
	 * @param customerList 
	 * @param veh Container with parameters for capacity and route duration
	 * @param plannedCustomers Marker for customers which are planned already in other stages
	 * @throws PreCheckException 
	 */
	private XFVRPSolution executeRoutePlanning(RoutingDataBag dataBag) throws PreCheckException {
		Node[] nodes = new PreCheckService().precheck(dataBag.nodes, dataBag.vehicle, parameter);
		XFVRPModel model = new ModelBuilder().build(nodes, dataBag.vehicle, metric, parameter, statusManager);
		Solution route = new InitialSolutionBuilder().build(model, parameter, statusManager);

		// VRP optimizations, if initiated route has appropriate length
		if(route.getGiantRoute().length > 0) {
			/*
			 * For each given optimization procedure the current
			 * route plan is searched for optimizations. If route
			 * splitting is allowed, big route plans with a big 
			 * number of routes is splitted into smaller route plans.
			 * This is a speed up. 
			 */
			for (XFVRPOptBase xfvrp : optList) {				
				statusManager.fireMessage(StatusCode.RUNNING, "Optimiziation for algorithm "+xfvrp.getClass().getSimpleName() + " started.");

				try {
					if(parameter.isRouteSplittingAllowed() && xfvrp.isSplittable)
						route = new XFVRPOptSplitter().execute(route, model, statusManager, xfvrp);
					else
						route = xfvrp.execute(route, model, statusManager);
				} catch(UnsupportedOperationException usoex) {
					statusManager.fireMessage(StatusCode.EXCEPTION, "Splitting encountert problem:\n"+usoex.getMessage());
				}
			}

			// Normalization of last result
			route = NormalizeSolutionService.normalizeRoute(route, model);
		}

		lastModel = model;
		return new XFVRPSolution(route, model);
	}

	/**
	 * Uses the last planned solution and turns it
	 * into a report representation.
	 * 
	 * All route plan informations can be akquired by this report.
	 * 
	 * @return A report data structure with detailed information about the route plan or null if no solution was calculated.
	 */
	public Report getReport() {
		if(vehicleSolutionList.size() > 0) {

			Report rep = new Report(lastModel);
			for (XFVRPSolution sol : vehicleSolutionList)
				rep.importReport(new ReportBuilder().getReport(sol));

			return rep;
		}
		return null;
	}

	/**
	 * Adds a certain optimization algorithm out
	 * of the spectrum of accessible methods in
	 * the enumeration XFVRPOptType.
	 * 
	 * @param type algorithm type. it can't be null.
	 */
	public void addOptType(XFVRPOptType type) {
		if(type != null)
			optList.add(type.createInstance());
	}

	/**
	 * Clears all added optimization algorithms
	 */
	public void clearOptTypes() {
		optList.clear();
	}
}
