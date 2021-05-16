package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.Arrays;

/**
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public abstract class XFILS extends XFVRPOptBase {

	protected XFVRPOptBase[] optArr;
	protected double[] optPropArr;
	protected XFRandomChangeService randomChangeService;

	public Solution execute(Solution solution) throws XFVRPException {
		Solution currentSolution = solution.copy();
		Solution bestSolution = solution.copy();
		Quality bestQuality = check(bestSolution);

		statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" is starting with "+model.getParameter().getILSLoops()+" loops. Start quality " + bestQuality.getCost());

		for (int i = 0; checkTerminationCriteria(i); i++) {
			Solution newSolution = currentSolution.copy();

			// Variation
			newSolution = randomChangeService.change(newSolution, model);

			// Intensification
			newSolution = localSearch(newSolution);

			// Evaluation
			Quality newQuality = check(newSolution);

			// Selection
			if(newQuality.getFitness() < bestQuality.getFitness()) {
				statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" loop "+i+"\t last cost : "+bestQuality.getCost()+"\t new cost : "+newQuality.getCost());

				currentSolution = newSolution;
				bestQuality = newQuality;
				bestSolution = newSolution;
			} else {
				statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" loop "+i+"\t with cost : "+newQuality.getCost()+"\t best cost : "+bestQuality.getCost());
			}
		}

		return NormalizeSolutionService.normalizeRoute(bestSolution, model);
	}

	/**
	 * Checks if a certain termination criteria is reached.
	 *
	 * True - Loop can go on
	 * False - Terminate loop
	 *
	 * Criteria are:
	 *  - Max number of loops
	 *  - Max running time
	 */
	protected boolean checkTerminationCriteria(int loopIdx) {
		if(loopIdx >= model.getParameter().getILSLoops())
			return false;

		return statusManager.getDurationSinceStartInSec() < model.getParameter().getMaxRunningTimeInSec();
	}

	protected Solution localSearch(Solution solution) throws XFVRPException {
		boolean[] processedNS = new boolean[optArr.length];

		Quality bestQuality = null;
		int nbrOfProcessed = 0;
		while(nbrOfProcessed < processedNS.length) {
			// Choose
			int optIdx = choose(processedNS);

			// Process
			solution = optArr[optIdx].execute(solution, model, statusManager);

			// Check
			Quality currentQuality = check(solution);
			if(bestQuality == null || currentQuality.getFitness() < bestQuality.getFitness()) {
				bestQuality = currentQuality;
				Arrays.fill(processedNS, false);
				nbrOfProcessed = 0;
			}

			// Mark
			processedNS[optIdx] = true;
			nbrOfProcessed++;
		}

		return solution;
	}

	protected int choose(boolean[] processedArr) {
		int idx = -1;
		do {
			double sum = 0;
			double r = rand.nextDouble();
			for (int j = 0; j < processedArr.length; j++) {
				sum += optPropArr[j];
				if(sum > r) {
					idx = j;
					break;
				}
			}
		} while(processedArr[idx]);

		return idx;
	}
}
