package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.Arrays;

public abstract class XFILS extends XFVRPOptBase {

	protected XFVRPOptBase[] optArr;
	protected double[] optPropArr;
	protected XFRandomChangeService randomChangeService;
	
	public Solution execute(Solution solution) {
		Solution bestRoute = solution.copy(); 
		Solution bestBestTour = solution.copy();
		Quality bestBestQ = check(solution);
		
		statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" is starting with "+model.getParameter().getILSLoops()+" loops.");

		for (int i = 0; checkTerminationCriteria(i); i++) {
			Solution gT = bestRoute.copy();

			// Variation
			gT = randomChangeService.change(gT, model);
			
			// Intensification
			gT = localSearch(gT);

			// Evaluation
			Quality q = check(gT);

			// Selection
			if(q.getFitness() < bestBestQ.getFitness()) {
				statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" loop "+i+"\t last cost : "+bestBestQ.getCost()+"\t new cost : "+q.getCost());

				bestRoute = gT;
				bestBestQ = q;
				bestBestTour = gT;
			} else {
				statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" loop "+i+"\t with cost : "+q.getCost());
				bestRoute = NormalizeSolutionService.normalizeRoute(bestRoute, model);
			}
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
	 */
	protected boolean checkTerminationCriteria(int loopIdx) {
		if(loopIdx >= model.getParameter().getILSLoops())
			return false;
		
		if(statusManager.getDurationSinceStartInSec() >= model.getParameter().getMaxRunningTimeInSec())
			return false;
		
		return true;
	}
	
	protected Solution localSearch(Solution solution) {
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
