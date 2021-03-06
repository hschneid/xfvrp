package xf.xfvrp.opt.improve;

import xf.xfvrp.base.Quality;
import xf.xfvrp.opt.Solution;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Path Exchange neighborhood search is a union of several neighborhoods
 * - XFVRPRelocate
 * - XFVRPSwap
 * - XFVRPOrOpt2
 * - XFVRPSwapBody2
 * 
 * The neighborhoods are sequentially searched for the best improving step.
 * 
 * @author hschneid
 *
 */
public class XFVRPPathExchange extends XFVRPOptImpBase {

	private final XFVRPRelocate rel = new XFVRPRelocate();
	private final XFVRPSwap swa = new XFVRPSwap();
	private final XFVRPPathMove or = new XFVRPPathMove();
	private final XFVRPSwapSegmentWithInvert bod = new XFVRPSwapSegmentWithInvert();

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	protected Quality improve(Solution solution, Quality bestResult) {
		Quality bestQ = bestResult;

		Solution best = null;

		Object[] o = null;
		{
			o = opt(solution, bestResult, bestQ, rel);
			if(o != null) {best = (Solution) o[0]; bestQ = (Quality) o[1];}
			o = opt(solution, bestResult, bestQ, swa);
			if(o != null) {best = (Solution) o[0]; bestQ = (Quality) o[1];}
			o = opt(solution, bestResult, bestQ, or);
			if(o != null) {best = (Solution) o[0]; bestQ = (Quality) o[1];}
			o = opt(solution, bestResult, bestQ, bod);
			if(o != null) {best = (Solution) o[0]; bestQ = (Quality) o[1];}
		}

		if(best != null) {
			solution.setGiantRoute(best.getGiantRoute());
			return bestQ;
		}
		
		return null;
	}

	/**
	 * 
	 * @param giantRoute
	 * @param bestResult
	 * @param bestQ
	 * @param opt
	 * @return
	 */
	private Object[] opt(Solution solution, Quality bestResult, Quality bestQ, XFVRPOptImpBase opt) {
		try {
			Solution newSolution = solution.copy(); 
			Quality result = opt.improve(newSolution, bestResult, model);
			if(result != null && result.getFitness() < bestQ.getFitness())
				return new Object[]{newSolution, result};
		} catch (Exception e) {}
		return null;
	}

}
