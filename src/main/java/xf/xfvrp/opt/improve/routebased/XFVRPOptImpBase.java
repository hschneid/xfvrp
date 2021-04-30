package xf.xfvrp.opt.improve.routebased;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.Queue;

/**
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * This class provides the basis local search structure
 * of iteratively calling an improve-method and memorizing
 * the best solution.
 *
 * The downhill search stops if no further improvement
 * can be found.
 *
 * This class is abstract and will be implemented by the route-based neighborhood
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

	protected abstract Queue<float[]> search(Node[][] routes);
	protected abstract void change(Solution solution, float[] changeParameter) throws XFVRPException;
	protected abstract void reverseChange(Solution solution, float[] changeParameter) throws XFVRPException;

	/**
	 * This method calls the abstract improve method of this optimization class with
	 * a given model. This method is useful if optimization operators are designed with
	 * several lower optimization operators. The model may switch between the lower operators.
	 *
	 * Currently it is only used by the PathExchange operator.
	 */
	public Quality improve(Solution giantTour, Quality bestResult, XFVRPModel model) throws XFVRPException {
		this.model = model;

		return improve(giantTour, bestResult);
	}

	/*
	 * (non-Javadoc)
	 * @see xf.xfvrp.base.XFVRPBase#execute(xf.xfvrp.opt.Solution)
	 */
	@Override
	public Solution execute(Solution solution) throws XFVRPException {
		// Evaluate current solution
		Quality bestResult = check(solution);

		// Search for improvements and apply them as long as there are improvements or enough time left
		long startTime = System.currentTimeMillis();
		while((System.currentTimeMillis() - startTime) / 1000.0 < model.getParameter().getMaxRunningTimeInSec()) {
			Quality result = improve(solution, bestResult);

			if (result == null)
				break;

			bestResult = result;
		}

		// Normalize resulting solution - Remove empty routes
		NormalizeSolutionService.normalizeRouteWithCleanup(solution, model);

		return solution;
	}

	private Quality improve(final Solution solution, Quality bestResult) throws XFVRPException {
		checkIt(solution);

		Queue<float[]> improvingSteps = search(solution.getRoutes());

		// Find first valid improving change
		while(!improvingSteps.isEmpty()) {
			float[] val = improvingSteps.remove();

			// Variation
			change(solution, val);

			Quality result = checkIt(solution, (int)val[1], (int)val[2]);
			if(result != null && result.getFitness() < bestResult.getFitness()) {
				solution.fixateQualities();
				return result;
			}

			// Reverse-Variation
			reverseChange(solution, val);
			solution.resetQualities();
		}

		return null;
	}

	/**
	 * This method checks a new solution, if it is better than the current best solution. If
	 * the result of this method is not null, then the new solution is better. Otherwise not. The
	 * check includes a possible check of the loading restrictions.
	 */
	protected Quality checkIt(Solution solution) throws XFVRPException {
		// Evaluate the costs and restrictions (penalties) of a giant route
		Quality result = check(solution);

		// Only valid solutions are allowed.
		if(result.getPenalty() == 0) {
			return result;
		}

		return null;
	}

	/**
	 * Check a solution for 2 routes
	 */
	protected Quality checkIt(Solution solution, int routeIdxA, int routeIdxB) throws XFVRPException {
		// Evaluate the costs and restrictions (penalties) of a giant route
		Quality result = check(solution, routeIdxA, routeIdxB);

		// Only valid solutions are allowed.
		if(result.getPenalty() == 0) {
			return result;
		}

		return null;
	}
}
