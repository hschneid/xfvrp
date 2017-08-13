package xf.xfvrp.opt.improve;

import java.util.Arrays;

import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.ils.RandomChangeService;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Optimization procedure for iterative local search
 * 
 * Three local search procedures with adaptive randomized variable neighborhood selection.
 * 
 * @author hschneid
 *
 */
public class XFVRPILS extends XFVRPOptBase {

	private XFVRPOptBase[] optArr = new XFVRPOptBase[]{
			new XFVRPRelocate(),
			new XFVRPSwap(),
			new XFVRPPathMove(),
			new XFVRPPathExchange()
	};

	private double[] optPropArr = new double[] {
			0.4, 0.4, 0.15, 0.05
	};
	
	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Solution execute(Solution solution) {
		Solution bestRoute = solution.copy(); 
		Solution bestBestTour = solution.copy();
		Quality bestBestQ = check(solution);
		
		RandomChangeService randomChange = new RandomChangeService();

		statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" is starting with "+model.getParameter().getILSLoops()+" loops.");

		for (int i = 0; checkTerminationCriteria(i); i++) {
			Solution gT = bestRoute.copy();

			// Variation
			gT = randomChange.change(gT, model);
			
			// Intensification
			gT = localSearch(gT, model.getVehicle());

			// Evaluation
			Quality q = check(gT);

			// Selection
			if(q.getFitness() < bestBestQ.getFitness()) {
				statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" loop "+i+"\t last cost : "+bestBestQ.getCost()+"\t new cost : "+q.getCost());

				bestRoute = gT;
				bestBestQ = q;
				bestBestTour = gT;
			} else
				bestRoute = NormalizeSolutionService.normalizeRoute(bestRoute, model);
		}

		return NormalizeSolutionService.normalizeRoute(bestBestTour, model);
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
	 * 
	 * @param loopIdx Current loop index
	 * @return Should continue?
	 */
	private boolean checkTerminationCriteria(int loopIdx) {
		if(loopIdx >= model.getParameter().getILSLoops())
			return false;
		
		if(statusManager.getDurationSinceStartInSec() >= model.getParameter().getMaxRunningTimeInSec())
			return false;
		
		return true;
	}
	
	/**
	 * 
	 * @param giantRoute
	 * @param vehicle
	 * @return
	 */
	private Solution localSearch(Solution solution, Vehicle vehicle) {
		boolean[] processedArr = new boolean[optArr.length];

		Quality q = null;
		int nbrOfProcessed = 0;
		while(nbrOfProcessed < processedArr.length) {
			// Choose
			int optIdx = choose(processedArr);

			// Process
			solution = optArr[optIdx].execute(solution, model, statusManager);

			// Check
			Quality qq = check(solution);
			if(q == null || qq.getFitness() < q.getFitness()) {
				q = qq;
				Arrays.fill(processedArr, false);
				nbrOfProcessed = 0;
			}

			// Mark
			processedArr[optIdx] = true;
			nbrOfProcessed++;
		}

		return solution;
	}
	
	/**
	 * 
	 * @param processedArr
	 * @return
	 */
	private int choose(boolean[] processedArr) {
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

	/**
	 * 
	 * @param nodes
	 * @return
	 */
//	@SuppressWarnings("unused")
//	private boolean checkDups(Node[] nodes) {
//		Set<String> set = new HashSet<String>();
//		for (Node n : nodes) {
//			if(n.getSiteType() == SiteType.CUSTOMER) {
//				if(set.contains(n.getExternID()))
//					return false;
//				set.add(n.getExternID());
//			}
//		}
//
//		return true;
//	}
}
