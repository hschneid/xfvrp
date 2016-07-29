package xf.xfvrp.opt.improve;

import java.util.Set;

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
 * This class provides the basis local search structure
 * of iteratively calling an improve-method and memorizing
 * the best solution.
 * 
 * The downhill search stops if no further improvement
 * can be found.
 * 
 * This class is abstract and will be implemented by the neighborhood
 * generating operators itself.
 * 
 * @author hschneid
 *
 */
public abstract class XFVRPOptImpBase extends XFVRPOptBase {

	/**
	 * Constructor for all improvement heuristics
	 */
	public XFVRPOptImpBase() {
		isSplittable = true;
	}
	
	/**
	 * This abstract method is implemented by the explicit
	 * neighborhood generating operators like 2-opt or 3-opt.
	 * 
	 * The giantRoute will be changed by the improve method, where as
	 * the bestResult won't be changed. The resulting quality represents
	 * the fitness of the changed and imrpoved giantRoute.
	 * 
	 * If the result is null, no improvement was found and giant tour
	 * was not changed.
	 * 
	 * @param giantRoute
	 * @param bestResult
	 * @return
	 */
	protected abstract Quality improve(Node[] giantTour, Quality bestResult);
	
	/**
	 * This method calls the abstract improve method of this optimization class with
	 * a given model. This method is useful if optimization operators are designed with
	 * several lower optimization operators. The model may switch between the lower operators.
	 * 
	 * Currently it is only used by the PathExchange operator.
	 * 
	 * @param giantRoute
	 * @param bestResult
	 * @param model
	 * @return
	 */
	protected Quality improve(Node[] giantTour, Quality bestResult, XFVRPModel model) {
		this.model = model;
		
		return improve(giantTour, bestResult);
	}
	
	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Node[] execute(Node[] giantRoute) {
		// Evaluate current solution
		Quality bestResult = check(giantRoute);

		// Search for improvements and apply them as long as there are improvements or enough time left
		while(statusManager.getDurationSinceStartInSec() < model.getParameter().getMaxRunningTimeInSec()) {
			Quality result = improve(giantRoute, bestResult);

			if (result == null)
				break;
			
//			statusManager.fireMessage(StatusCode.RUNNING1, this.getClass().getName() + " - Found improve: " + result.getCost() + " (" + (bestResult.getCost() - result.getCost()) + ") Best: "+bestResult.getCost());
			
			bestResult = result;
		}
		
		// Normalize resulting solution - Remove empty routes
		return Util.normalizeRoute(giantRoute, model);
	}

	/**
	 * Overwrites the getDistance method. If node b is a depot, then
	 * b is replaced by the depot of node at index aa or if node a is a depot, then
	 * a is replaced by the depot of node at index bb. 
	 * 
	 * @param a Source node
	 * @param b Destination node
	 * @param aa Node index of depot for node b
	 * @param bb Node index of depot for node a
	 * @return distance
	 */
	protected float getDistance(Node a, Node b, int aa, int bb) {
		if(aa != bb) {
			if(b.getSiteType() == SiteType.DEPOT)
				b = model.getNodeArr()[aa];
			else if(a.getSiteType() == SiteType.DEPOT)
				a = model.getNodeArr()[bb];
		}
		return model.getDistanceForOptimization(a, b);
	}
	
	/**
	 * Overwrites the getDistance method. If node b is a depot, then
	 * b is replaced by the allocated depot of node a.
	 * 
	 * @param a Source node
	 * @param b Destination node
	 * @param depotAlloc
	 * @return distance
	 */
	protected float getDistance(Node a, Node b, int depotAlloc) {
		if(b.getSiteType() == SiteType.DEPOT)
			b = model.getNodeArr()[depotAlloc];
		return model.getDistanceForOptimization(a, b);
	}
	
	/**
	 * This methods builds a loading footprint, if the load planning parameter is activated.
	 * 
	 * A loading footprint is String representation of node sequence. It is used to avoid the
	 * load planning on same node sequence multiple times.
	 * 
	 * @param route A sequence of Nodes
	 * @return A loading footprint if parameter for load planning is activated otherwise null
	 */
	protected Set<String> getLoadingFootprint(Node[] route) {
		if(model.getParameter().isWithLoadPlanning())
			return XFVRPLPBridge.getFootprints(route);
		return null;
	}
	
	/**
	 * This method checks a new solution, if it is better than the current best solution. If
	 * the result of this method is not null, then the new solution is better. Otherwise not. The
	 * check includes a possible check of the loading restrictions.
	 * 
	 * @param giantRoute A new VRP solution
	 * @param bestResult The quality of the current best solution
	 * @param loadingFootprint A pre-evaluated footprint of load planning
	 * @return The quality of the new solution. If it is null, then the new solution is not better than the current best solution.
	 */
	protected Quality check(Node[] giantRoute, Set<String> loadingFootprint) {
		// Evaluate the costs and restrictions (penalties) of a giant route
		Quality result = check(giantRoute);

		// Only valid solutions are allowed.
		if(result.getPenalty() == 0) {
			// Load planing, if it is switched on 
			XFVRPLPBridge.check(giantRoute, loadingFootprint, model, result);

			// Only valid solutions are allowed.
			if(result.getPenalty() == 0)
				return result;
		}
		
		return null;
	}
}
