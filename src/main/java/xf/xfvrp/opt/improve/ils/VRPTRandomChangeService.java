package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Random change service of the VRPTT ILS (spec §2.4). In addition to the
 * segment-based perturbation of the base service, VRPTT specific moves are
 * applied with a certain probability:
 * <ul>
 * <li>adding a transshipment record (mode change DIRECT to VIA_TDP),</li>
 * <li>removing a transshipment record (mode change to direct service),</li>
 * <li>re-assigning a record to another transshipment location.</li>
 * </ul>
 * A newly added record first worsens the costs (detours), but frees load
 * capacity of the supplier, which the following local search can use for
 * relocations. Whether the variation as a whole improves the solution is
 * decided by the ILS acceptance.
 *
 * @author hschneid
 */
public class VRPTRandomChangeService extends XFVRPRandomChangeService {

	private final VRPTTTransshipmentMover mover = new VRPTTTransshipmentMover();

	/** Probabilities of the VRPTT moves (add, remove, reassign) */
	private static final double[] RECORD_MOVE_PROBABILITY = {0.2, 0.35, 0.5};

	/*
	 * (non-Javadoc)
	 * @see xf.xfvrp.opt.improve.ils.XFRandomChangeService#change(xf.xfvrp.opt.Solution)
	 */
	@Override
	public Solution change(Solution solution) throws XFVRPException {
		setModel(solution.getModel());
		mover.setModel(solution.getModel());

		double r = rand.nextDouble();
		if (r < RECORD_MOVE_PROBABILITY[0]) {
			if (mover.randomAddRecord(solution))
				return solution;
		} else if (r < RECORD_MOVE_PROBABILITY[0] + RECORD_MOVE_PROBABILITY[1]) {
			if (mover.randomRemoveRecord(solution))
				return solution;
		} else if (r < RECORD_MOVE_PROBABILITY[0] + RECORD_MOVE_PROBABILITY[1] + RECORD_MOVE_PROBABILITY[2]) {
			if (mover.randomReassignTL(solution))
				return solution;
		}

		return super.change(solution);
	}
}
