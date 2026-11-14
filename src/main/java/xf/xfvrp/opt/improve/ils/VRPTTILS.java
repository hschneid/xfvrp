package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.evaluation.EvaluationService;
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSingleSwap;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Iterated local search for the Vehicle Routing Problem with Trailers and
 * Transshipments (VRPTT).
 * <p>
 * Only route-based neighborhood moves, which keep the transshipment blocks
 * (TL visit followed by the served customer) structurally valid, are applied
 * (spec §2.4). The synchronization feasibility is checked by the
 * SynchronizationAnalyzer during evaluation, so that only synchronization-safe
 * solutions are accepted.
 *
 * @author hschneid
 */
public class VRPTTILS extends XFILS {

	/*
	 * Record context of the joint multi-vehicle plan (Phase 3): if set, the
	 * evaluation includes the routes of other vehicles, so that
	 * cross-vehicle transshipment records stay synchronization-safe during
	 * the re-planning.
	 */
	private Node[][] recordContextRoutes = new Node[0][];
	private XFVRPModel[] recordContextModels = new XFVRPModel[0];

	/*
	 * (non-Javadoc)
	 * @see xf.xfvrp.opt.improve.ils.XFILS#execute(xf.xfvrp.opt.Solution)
	 */
	@Override
	public Solution execute(Solution solution) throws XFVRPException {
		optArr = new XFVRPOptBase[]{
				new XFVRPSingleMove(),
				new XFVRPSingleSwap()
		};
		optPropArr = new double[]{
				0.5, 0.5
		};
		randomChangeService = new VRPTRandomChangeService();

		// Joint multi-vehicle plan (Phase 3): the route-based neighborhood
		// moves evaluate with the record context, so that cross-vehicle
		// transshipment records are not penalized as unmatched and moves keep
		// the records synchronization-safe.
		if (recordContextRoutes.length > 0) {
			EvaluationService jointEvaluationService = new EvaluationService();
			jointEvaluationService.setRecordContext(recordContextRoutes, recordContextModels);
			evaluationService = jointEvaluationService;
			for (XFVRPOptBase opt : optArr)
				opt.setEvaluationService(jointEvaluationService);
		}

		return super.execute(solution);
	}

	/**
	 * Sets the routes of the other vehicles of the joint plan, which are
	 * visible during the evaluation of transshipment records (Phase 3).
	 */
	public void setRecordContext(Node[][] recordContextRoutes, XFVRPModel[] recordContextModels) {
		this.recordContextRoutes = recordContextRoutes;
		this.recordContextModels = recordContextModels;
	}

	/**
	 * Evaluates the solution with the record context of the joint plan, if
	 * one is set, so that cross-vehicle records are not penalized as
	 * unmatched.
	 */
	@Override
	public Quality check(Solution solution) throws XFVRPException {
		if (recordContextRoutes.length == 0)
			return super.check(solution);

		return evaluationService.check(solution, recordContextRoutes, recordContextModels);
	}
}
