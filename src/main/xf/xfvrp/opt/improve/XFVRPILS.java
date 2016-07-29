package xf.xfvrp.opt.improve;

import java.util.Arrays;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.XFVRPOptBase;

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
			new XFVRPOrOptWithInvert(),
			new XFVRPPathExchange()
	};

	private double[] optPropArr = new double[] {
//			1
			0.4, 0.4, 0.15, 0.05
//			0.7, 0.3
	};

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Node[] execute(Node[] giantTour) {
		Node[] bestRoute = Arrays.copyOf(giantTour, giantTour.length);
		Node[] bestBestTour = Arrays.copyOf(giantTour, giantTour.length);
		Quality bestBestQ = check(giantTour);

		statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" is starting with "+model.getParameter().getILSLoops()+" loops.");

		for (int i = 0; checkTerminationCriteria(i); i++) {
			Node[] gT = Arrays.copyOf(bestRoute, bestRoute.length);

			// Variation
			perturb2(gT, model.getVehicle());
			
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
				bestRoute = Util.normalizeRoute(bestRoute, model);
		}

		return Util.normalizeRoute(bestBestTour, model);
	}

	/**
	 * This perturb routine relocates single nodes iterativly. The nodes are
	 * selected randomly.
	 * 
	 * @param giantRoute
	 * @param vehicle
	 */
	private void perturb2(Node[] giantRoute, Vehicle vehicle) {
		int nbrOfVariations = 5;
		int[] param = new int[3];
		for (int i = 0; i < nbrOfVariations; i++) {
			// Search source node
			// Restriction: no depot
			chooseSrc(param, giantRoute);

			// Search destination
			// Restriction: 
			//   Source is not destination
			//   Solution is not invalid
			int cnt = 0;
			boolean changed = false;
			
			while(true) {
				// Choose
				chooseDst(param, giantRoute);

				// Move
				pathMove(giantRoute, param[0], param[0] + param[1], param[2]);
				
				// Eval
				Quality q = check(giantRoute);
				if(q.getPenalty() == 0) {
					changed = true;
					break;
				}

				// Re-Move
				if(param[2] > param[0])
					pathMove(giantRoute, param[2] - param[1] - 1, param[2] - 1, param[0]);
				else
					pathMove(giantRoute, param[2], param[2] + param[1], param[0] + param[1] + 1);
				
				// Terminate for infinity
				if(cnt > 100)
					break;
				
				cnt++;
			}
			
			if(!changed)
				i--;
		}
	}

	/**
	 * 
	 * @param param
	 * @param giantRoute
	 */
	private void chooseSrc(int[] param, Node[] giantRoute) {
		// Choose a random source node (customer or replenish)
		int src = -1;
		do {
			src = rand.nextInt(giantRoute.length - 2) + 1;
		} while(giantRoute[src].getSiteType() == SiteType.DEPOT);

		// Reset source node at the beginning of a block
		while(src > 1) {
			Node s = giantRoute[src];
			Node l = giantRoute[src - 1];
			if(s.getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX ||
					l.getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX ||
					s.getPresetBlockIdx() != l.getPresetBlockIdx() ||
					l.getSiteType() == SiteType.DEPOT)
				break;

			src--;
		}

		param[0] = src;
		param[1] = 0;

		// Extend source node to source path if next nodes belongs to source node block
		if(giantRoute[src].getPresetBlockPos() >= 0) {
			int srcBlockIdx = giantRoute[src].getPresetBlockIdx();
			for (int i = src + 1; i < giantRoute.length; i++) {
				Node n = giantRoute[i];
				if(n.getPresetBlockIdx() == srcBlockIdx && n.getPresetBlockIdx() >= BlockNameConverter.DEFAULT_BLOCK_IDX)
					param[1]++;
				else
					break;
			}
		}
	}

	/**
	 * 
	 * @param param
	 * @param giantRoute
	 */
	private void chooseDst(int[] param, Node[] giantRoute) {
		int dst = -1;
		do {
			dst = rand.nextInt(giantRoute.length - 1) + 1;
		} while(dst >= param[0] && dst <= param[0] + param[1]);
		
		while(dst > 0) {
			Node d = giantRoute[dst];
			Node l = giantRoute[dst - 1];

			if(d.getSiteType() == SiteType.DEPOT ||
					l.getSiteType() == SiteType.DEPOT)
				break;
			
			if(d.getPresetBlockIdx() != l.getPresetBlockIdx())
				break;

			dst--;
		}

		param[2] = dst;
	}

	/**
	 * 
	 * @param giantRoute
	 * @param vehicle
	 * @return
	 */
	private Node[] localSearch(Node[] giantRoute, Vehicle vehicle) {
		boolean[] processedArr = new boolean[optArr.length];

		Quality q = null;
		int nbrOfProcessed = 0;
		while(nbrOfProcessed < processedArr.length) {
			// Choose
			int optIdx = choose(processedArr);

			// Process
			giantRoute = optArr[optIdx].execute(giantRoute, model, statusManager);

			// Check
			Quality qq = check(giantRoute);
			if(q == null || qq.getFitness() < q.getFitness()) {
				q = qq;
				Arrays.fill(processedArr, false);
				nbrOfProcessed = 0;
			}

			// Mark
			processedArr[optIdx] = true;
			nbrOfProcessed++;
		}

		return giantRoute;
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
