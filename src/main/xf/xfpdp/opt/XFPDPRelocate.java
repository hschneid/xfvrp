package xf.xfpdp.opt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import xf.xfpdp.XFPDPUtils;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.opt.improve.XFVRPOptImpBase;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Contains the optimization algorithms
 * for 2-opt
 * 
 * @author hschneid
 *
 */
public class XFPDPRelocate extends XFVRPOptImpBase {

//	private float epsilon = 5;
	
	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Node[] giantTour, Quality bestResult) {
		final Set<String> loadingFootprint = getLoadingFootprint(giantTour);

		List<float[]> improvingStepList = new ArrayList<>();

		if(model.getNbrOfDepots() == 1)
			searchSingleDepot(giantTour, improvingStepList);
		else
			throw new IllegalArgumentException("Multi depot is not applicable for PDP optimization.");

		// Sortiere absteigend nach Potenzial
		sort(improvingStepList, 4);

		// Finde die erste valide verbessernde L�sung
		Node[] copy = new Node[giantTour.length];
		for (float[] val : improvingStepList) {
			int srcA = (int) val[0];
			int srcB = (int) val[1];
			int dstA = (int) val[2];
			int dstB = (int) val[3];
			
			System.arraycopy(giantTour, 0, copy, 0, giantTour.length);
			XFPDPUtils.move(giantTour, srcA, srcB, dstA, dstB);

			Quality result = check(giantTour, loadingFootprint);
			if(result != null && result.getFitness() < bestResult.getFitness()) {
				return result;
			}

			System.arraycopy(copy, 0, giantTour, 0, giantTour.length);
		}

		return null;
	}

	

	/**
	 * Searches all improving valid steps in search space for a VRP with one depot.
	 * 
	 * @param giantRoute
	 * @param improvingStepList
	 */
	private void searchSingleDepot(Node[] route, List<float[]> improvingStepList) {
		int[] routeIdxArr = new int[route.length];
		int id = 0;
		for (int i = 1; i < route.length; i++) {
			if(route[i].getSiteType() == SiteType.DEPOT)
				id++;
			routeIdxArr[i] = id;
		}

		int[] shipmentPairIdxArr = new int[route.length];
		{
			int[][] shipmentsArr = new int[route.length][2];
			for (int i = 1; i < route.length; i++) {
				if(route[i].getSiteType() == SiteType.DEPOT)
					continue;
				
				int loadTypeIdx = (route[i].getDemand()[0] > 0) ? 0 : 1;
				
				shipmentsArr[route[i].getShipmentIdx()][loadTypeIdx] = i;
			}
			for (int i = 1; i < route.length; i++) {
				if(route[i].getSiteType() == SiteType.DEPOT)
					continue;

				int loadTypeIdx = (route[i].getDemand()[0] < 0) ? 0 : 1;
				
				shipmentPairIdxArr[i] = shipmentsArr[route[i].getShipmentIdx()][loadTypeIdx];
			}
		}

		// Search all improving steps in neighborhood
		for (int srcA = 1; srcA < route.length - 1; srcA++) {
			// Source must not be a depot
			if(route[srcA].getSiteType() == SiteType.DEPOT)
				continue;
			if(route[srcA].getDemand()[0] < 0)
				continue;
			
			int srcB = shipmentPairIdxArr[srcA];
			
			for (int dstA = 0; dstA < route.length - 1; dstA++) {
				for (int dstB = dstA; dstB < route.length - 1; dstB++) {
					if(routeIdxArr[dstA] != routeIdxArr[dstB])
						break;

					float val = 0;
					
					// Destination pointer must not be at Source pointer
					if((srcA - dstA) * (srcB - dstB) * (srcA - dstB) * (srcB - dstA) == 0)
						continue;
					
					// Einfache Opteration:
					// Schiebe eine Sendung (P & D) nach rechts. Es wird immer nach einem
					// Zielpunkt eingef�gt. Daher soll srcA ungleich (dstA & dstA - 1) sein UND srcB ungleich (dstB & dstB - 1).
					// Da B immer nach A sein muss, muss dstB gr��er dstA sein.
					// Verschiedene Sachen noch ungel�st:
					//  - Verschieben nach links
					//  - srcA und srcB werden mit dstA und dstB ineinander gewurschtelt
					//  - A und B liegen jetzt direkt nebeneinander
					
					val -= getDistanceForOptimization(route[srcA - 1], route[srcA]);
					val -= getDistanceForOptimization(route[srcA], route[srcA + 1]);
					val -= getDistanceForOptimization(route[srcB], route[srcB + 1]);
					if(srcB - srcA != 1)
						val -= getDistanceForOptimization(route[srcB - 1], route[srcB]);
					if(srcA - dstA != 1 && srcB - dstA != 1)
						val -= getDistanceForOptimization(route[dstA], route[dstA + 1]);
					if(srcA - dstB != 1 && srcB - dstB != 1 && dstA != dstB)
						val -= getDistanceForOptimization(route[dstB], route[dstB + 1]);
					
					
					int offset = 1;
					val += getDistanceForOptimization(route[dstA], route[srcA]);
					if(dstA != dstB) {
						offset = (srcB - dstA == 1) ? 2 : (srcA - dstA == 1 && srcB - srcA == 1) ? 3 : 1;
						val += getDistanceForOptimization(route[srcA], route[dstA + offset]);
						val += getDistanceForOptimization(route[dstB], route[srcB]);
					} else
						val += getDistanceForOptimization(route[srcA], route[srcB]);
					
					offset = (srcA - dstB == 1 || srcB - dstB == 1) ? (srcA - dstB == 1 && srcB - srcA == 1) ? 3 : 2: 1;
					val += getDistanceForOptimization(route[srcB], route[dstB + offset]);
					
					// Wenn srcA wirklich verschoben wird (dstA also nicht dahinter ist)
					if(srcA - dstA != 1) {
						// Wenn srcA und srcB direkt nebeneinander liegen, dann �berspringe srcB
						offset = (srcB - srcA == 1) ? 2 : 1;
						val += getDistanceForOptimization(route[srcA - 1], route[srcA + offset]);
					}
					// Wenn nicht gilt, dass srcB vor dstA eingef�gt wird oder
					// srcA vor dstA eingef�gt wird und srcA und srcB hintereinander liegen
					if(srcB - srcA != 1 && ! (srcB - dstA == 1 || (srcA - dstA == 1 && srcB - srcA == 1)))
						val += getDistanceForOptimization(route[srcB - 1], route[srcB + 1]);
					
					if(val < -epsilon)
						improvingStepList.add(new float[]{srcA, srcB, dstA, dstB, -val});
				}
			}
		}
	}
}
