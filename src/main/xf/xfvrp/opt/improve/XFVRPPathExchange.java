package xf.xfvrp.opt.improve;

import java.util.Arrays;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;

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
	private final XFVRPOrOptWithInvert or = new XFVRPOrOptWithInvert();
	private final XFVRPSwapSegmentWithInvert bod = new XFVRPSwapSegmentWithInvert();

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	protected Quality improve(Node[] giantTour, Quality bestResult) {
		Quality bestQ = bestResult;

		Node[] best = null;

		Object[] o = null;
		{
			o = opt(giantTour, bestResult, bestQ, rel);
			if(o != null) {best = (Node[]) o[0]; bestQ = (Quality) o[1];}
			o = opt(giantTour, bestResult, bestQ, swa);
			if(o != null) {best = (Node[]) o[0]; bestQ = (Quality) o[1];}
			o = opt(giantTour, bestResult, bestQ, or);
			if(o != null) {best = (Node[]) o[0]; bestQ = (Quality) o[1];}
			o = opt(giantTour, bestResult, bestQ, bod);
			if(o != null) {best = (Node[]) o[0]; bestQ = (Quality) o[1];}
		}

		if(best != null) {
			System.arraycopy(best, 0, giantTour, 0, giantTour.length);
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
	private Object[] opt(Node[] giantTour, Quality bestResult, Quality bestQ, XFVRPOptImpBase opt) {
		try {
			Node[] gT = Arrays.copyOf(giantTour, giantTour.length);
			Quality result = opt.improve(gT, bestResult, model);
			if(result != null && result.getFitness() < bestQ.getFitness())
				return new Object[]{gT, result};
		} catch (Exception e) {}
		return null;
	}

}
