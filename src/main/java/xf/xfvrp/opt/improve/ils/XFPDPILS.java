package xf.xfvrp.opt.improve.ils;

import java.util.Arrays;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.XFPDPRelocate;

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
public class XFPDPILS extends XFVRPOptBase {

	private XFVRPOptBase[] optArr = new XFVRPOptBase[]{
			new XFPDPRelocate()
	};

	private double[] optPropArr = new double[]{
			1
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

		statusManager.fireMessage(StatusCode.RUNNING, this.getClass().getSimpleName()+" is starting with "+model.getParameter().getILSLoops()+" loops.");

		for (int i = 0; i < model.getParameter().getILSLoops(); i++) {
			Solution gT = bestRoute.copy();

			// Variation
			perturbPDP(gT, model.getVehicle());
			
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
	 * This perturb routine relocates single nodes iterativly. The nodes are
	 * selected randomly.
	 * 
	 * @param giantRoute
	 * @param vehicle
	 */
	private void perturbPDP(Solution solution, Vehicle vehicle) {
		Node[] giantRoute = solution.getGiantRoute();
		
		int nbrOfVariations = 5;
		int[] param = new int[4];
		Node[] copy = new Node[giantRoute.length];
		
		for (int i = 0; i < nbrOfVariations; i++) {
			// Search nodes for source shipment
			// Restriction: no depot
			chooseSrcPickup(param, giantRoute);
			chooseSrcDelivery(param, giantRoute);

			// Search destination
			// Restriction: 
			//   Source is not destination
			//   Solution is not invalid
			int cnt = 0;
			boolean changed = false;
			while(true) {
				// Choose
				chooseDstPickup(param, giantRoute);
				chooseDstDelivery(param, giantRoute);
				
				// Move
				System.arraycopy(giantRoute, 0, copy, 0, giantRoute.length);
				move(giantRoute, param[0], param[1], param[2], param[3]);
				
				// Eval
				Solution newSolution = new Solution();
				newSolution.setGiantRoute(giantRoute);
				Quality q = check(newSolution);
				if(q.getPenalty() == 0) {
					changed = true;
					break;
				}

				// Re-Move through copy back
				System.arraycopy(copy, 0, giantRoute, 0, giantRoute.length);

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
	private void chooseSrcPickup(int[] param, Node[] giantRoute) {
		// Choose a random source node (customer or replenish)
		int src = -1;
		do {
			src = rand.nextInt(giantRoute.length - 2) + 1;
		} while(giantRoute[src].getSiteType() == SiteType.DEPOT || giantRoute[src].getDemand()[0] < 0);

		param[0] = src;
	}

	/**
	 * 
	 * @param route
	 * @return
	 */
	private void chooseSrcDelivery(int[] param, Node[] route) {
		Node srcPickup = route[param[0]];
		int shipIdx = srcPickup.getShipmentIdx();
		param[1] = -1;
		
		for (int i = 0; i < route.length; i++) {
			if(route[i].getShipmentIdx() == shipIdx && route[i].getDemand()[0] < 0) {
				param[1] = i;
				return;
			}
		}
	
		if(param[1] == -1)
			throw new IllegalStateException("Structural exception of giant route, where a pickup nopde of a shipment has no delivery node.");
	}

	/**
	 * 
	 * @param param
	 * @param giantRoute
	 */
	private void chooseDstPickup(int[] param, Node[] giantRoute) {
		param[2] = -1;
		do {
			param[2] = rand.nextInt(giantRoute.length - 1);
		} while(param[2] == param[0]);
	}
	
	/**
	 * 
	 * @param param
	 * @param giantRoute
	 */
	private void chooseDstDelivery(int[] param, Node[] giantRoute) {
		int[] routeIdxArr = new int[giantRoute.length];
		int id = 0;
		for (int i = 1; i < giantRoute.length; i++) {
			if(giantRoute[i].getSiteType() == SiteType.DEPOT)
				id++;
			routeIdxArr[i] = id;
		}
		
		int dstPickupRouteIdx = routeIdxArr[param[2]];
		
		param[3] = -1;
		do {
			param[3] = rand.nextInt(giantRoute.length - 1);
		} while(param[2] > param[3] || routeIdxArr[param[3]] != dstPickupRouteIdx);
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
	 * @param giantRoute
	 * @param srcA
	 * @param srcB
	 * @param dstA
	 * @param dstB
	 */
	public static void move(Node[] giantRoute, int srcA, int srcB, int dstA, int dstB) {
		Node nSrcA = giantRoute[srcA];
		Node nSrcB = giantRoute[srcB];
		
		Node[] arr = new Node[giantRoute.length];
		System.arraycopy(giantRoute, 0, arr, 0, giantRoute.length);
		
		int j = 0;
		for (int i = 0; i < giantRoute.length; i++) {
			// Packe einen Knoten nur dann zurück in giant route, 
			// falls der Index nicht auf einem Source-Knoten liegt.
			if(i != srcA && i != srcB) 
				giantRoute[j++] = arr[i];
			
			// Es wird nach einem Knoten i eingefügt.
			if(i == dstA)
				giantRoute[j++] = nSrcA;
			// dstA und dstB k�nnen den gleichen Index haben, wenn A vor B direkt
			// eingef�gt werden soll.
			if(i == dstB)
				giantRoute[j++] = nSrcB;
		}
	}
}
