package xf.xfvrp.opt.improve.routebased;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveUtil;

import java.util.Queue;

/**
 * Copyright (c) 2012-2022 Holger Schneider
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

	protected abstract Queue<float[]> search(Solution solution);
	protected abstract Node[][] change(Solution solution, float[] changeParameter) throws XFVRPException;

	/**
	 * This method calls the abstract improve method of this optimization class with
	 * a given model. This method is useful if optimization operators are designed with
	 * several lower optimization operators. The model may switch between the lower operators.
	 *
	 * Currently it is only used by the PathExchange operator.
	 */
	public Quality improve(Solution solution, Quality bestResult, XFVRPModel model) throws XFVRPException {
		this.model = model;

		return improve(solution, bestResult);
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

			NormalizeSolutionService.normalizeRoute(solution);
		}

		// Normalize resulting solution - Remove empty routes
		NormalizeSolutionService.normalizeRouteWithCleanup(solution);

		return solution;
	}

	private Quality improve(final Solution solution, Quality bestResult) throws XFVRPException {
		check(solution);

		Queue<float[]> improvingSteps = search(solution);
		// Find first valid improving change
		while (!improvingSteps.isEmpty()) {
			float[] val = improvingSteps.remove();

			// Variation
			Node[][] oldRoutes = change(solution, val);

			Quality result = check(solution, (int) val[1], (int) val[2]);
			if (isImprovement(result, bestResult, (int) val[7])) {
				solution.fixateQualities();
				return result;
			}

			// Reverse
			reverseChange(solution, val, oldRoutes);
		}

		return null;
	}

	private boolean isImprovement(Quality currentResult, Quality bestResult, int overhangFlag) {
		// Is fitness better OR
		return currentResult.getPenalty() == 0 &&
				(currentResult.getFitness() < bestResult.getFitness() ||
						overhangFlag == XFVRPMoveUtil.IS_OVERHANG);
	}

	private void reverseChange(Solution solution, float[] val, Node[][] oldRoutes) {
		solution.setRoute((int)val[1], oldRoutes[0]);
		if(oldRoutes.length > 1)
			solution.setRoute((int)val[2], oldRoutes[1]);
		solution.resetQualities();
	}

}
