package xf.xfvrp.opt.improve;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;
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
	 */
	protected abstract Quality improve(Solution giantTour, Quality bestResult);

	/**
	 * This method calls the abstract improve method of this optimization class with
	 * a given model. This method is useful if optimization operators are designed with
	 * several lower optimization operators. The model may switch between the lower operators.
	 * 
	 * Currently it is only used by the PathExchange operator.
	 */
	protected Quality improve(Solution giantTour, Quality bestResult, XFVRPModel model) {
		this.model = model;

		return improve(giantTour, bestResult);
	}

	/*
	 * (non-Javadoc)
	 * @see xf.xfvrp.base.XFVRPBase#execute(xf.xfvrp.opt.Solution)
	 */
	@Override
	public Solution execute(Solution giantRoute) {
		// Evaluate current solution
		Quality bestResult = check(giantRoute);

		// Search for improvements and apply them as long as there are improvements or enough time left
		long startTime = System.currentTimeMillis();
		while((System.currentTimeMillis() - startTime)/1000.0 < model.getParameter().getMaxRunningTimeInSec()) {
			Quality result = improve(giantRoute, bestResult);

			if (result == null)
				break;

			//			statusManager.fireMessage(StatusCode.RUNNING1, this.getClass().getName() + " - Found improve: " + result.getCost() + " (" + (bestResult.getCost() - result.getCost()) + ") Best: "+bestResult.getCost());

			bestResult = result;
		}

		// Normalize resulting solution - Remove empty routes
		return NormalizeSolutionService.normalizeRoute(giantRoute, model);
	}

	/**
	 * Overwrites the getDistance method. If node b is a depot, then
	 * b is replaced by the depot of node at index aa or if node a is a depot, then
	 * a is replaced by the depot of node at index bb. 
	 */
	protected float getDistance(Node a, Node b, int aa, int bb) {
		if(aa != bb) {
			if(b.getSiteType() == SiteType.DEPOT)
				b = model.getNodes()[aa];
			else if(a.getSiteType() == SiteType.DEPOT)
				a = model.getNodes()[bb];
		}
		return model.getDistanceForOptimization(a, b);
	}

	/**
	 * Overwrites the getDistance method. If node b is a depot, then
	 * b is replaced by the allocated depot of node a.
	 */
	protected float getDistance(Node a, Node b, int depotAlloc) {
		if(b.getSiteType() == SiteType.DEPOT)
			b = model.getNodes()[depotAlloc];
		return model.getDistanceForOptimization(a, b);
	}
	
	/**
	 * This method checks a new solution, if it is better than the current best solution. If
	 * the result of this method is not null, then the new solution is better. Otherwise not. The
	 * check includes a possible check of the loading restrictions.
	 */
	protected Quality checkIt(Solution solution) {
		// Evaluate the costs and restrictions (penalties) of a giant route
		Quality result = check(solution);

		// Only valid solutions are allowed.
		if(result.getPenalty() == 0) {
			return result;
		}

		return null;
	}
}
