package xf.xfpdp.opt;

import java.util.ArrayList;
import java.util.List;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.opt.Solution;
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
	public Quality improve(final Solution solution, Quality bestResult) {
		Node[] giantTour = solution.getGiantRoute();
		if(model.getNbrOfDepots() != 1)
			throw new IllegalArgumentException("Multi depot is not applicable for PDP optimization.");

		List<float[]> improvements = search(giantTour);

		// Sortiere absteigend nach Potenzial
		sort(improvements, 4);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvements) {
			change(solution, val);

			Quality result = checkIt(solution);
			if(result != null && result.getFitness() < bestResult.getFitness()) {
				return result;
			}

			reverseChange(solution, val);
		}

		return null;
	}
	
	private void change(Solution solution, float[] val) {
		int srcA = (int) val[0];
		int srcB = (int) val[1];
		int dstA = (int) val[2];
		int dstB = (int) val[3];

		shipmentMove(solution, srcA, srcB, dstA, dstB);
	}

	private void reverseChange(Solution solution, float[] val) {
		int srcA = (int) val[0];
		int srcB = (int) val[1];
		int dstA = (int) val[2];
		int dstB = (int) val[3];
		
		int srcAOffset = 0;
		int srcBOffset = 0;
		int dstAOffset = 0;
		int dstBOffset = 0;
		
		dstAOffset += (dstA > srcB) ? -1 : 0;
		dstAOffset += (dstA > srcA) ? -1 : 0;
		dstBOffset += (dstB > srcA && dstB > srcB) ? -1 : 0;
		dstBOffset += (dstB < srcA && dstB < srcB) ? 1 : 0;
		srcAOffset += (srcA > dstB) ? 2 : 0;
		srcAOffset += (srcA > dstA && srcA < dstB) ? 1 : 0;
		srcBOffset += (srcB > dstA) ? 1 : 0;
		srcBOffset += (srcB > dstB) ? 0 : -1;
		
		srcA += srcAOffset;
		srcB += srcBOffset;
		dstA += dstAOffset;
		dstB += dstBOffset;
		
		shipmentMove(solution, dstA, dstB, srcA, srcB);
	}

	/**
	 * Searches all improving valid steps in search space for a VRP with one depot.
	 * 
	 * @param giantRoute
	 * @param improvingStepList
	 */
	private List<float[]> search(Node[] route) {
		List<float[]> improvements = new ArrayList<>();
		
		int[] routeIdx = getRouteIndex(route);

		int[] shipmentPositions = getShipmentPositions(route);

		// Search all improving steps in neighborhood
		for (int srcA = 1; srcA < route.length - 1; srcA++) {
			// Source must not be a depot
			if(route[srcA].getSiteType() == SiteType.DEPOT)
				continue;
			if(route[srcA].getDemand()[0] < 0)
				continue;

			int srcB = shipmentPositions[srcA];

			for (int dstA = 0; dstA < route.length - 1; dstA++) {
				for (int dstB = dstA; dstB < route.length - 1; dstB++) {
					if(routeIdx[dstA] != routeIdx[dstB])
						break;

					// Destination pointer must not be at Source pointer
					if((srcA - dstA) * (srcB - dstB) * (srcA - dstB) * (srcB - dstA) == 0)
						continue;

					float val = getPotential(route, srcA, srcB, dstA, dstB);

					if(val < -epsilon)
						improvements.add(new float[]{srcA, srcB, dstA, dstB, -val});
				}
			}
		}
		
		return improvements;
	}

	private float getPotential(Node[] route, int srcA, int srcB, int dstA, int dstB) {
		float val = 0;

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
		// Wenn A und B nicht direkt nebeneinander liegen.
		if(srcB - srcA != 1)
			val -= getDistanceForOptimization(route[srcB - 1], route[srcB]);
		// Wenn A und B nicht direkt vor dstA weggeschoben werden
		if(dstA - srcA != 1 && dstA - srcB != 1)
			val -= getDistanceForOptimization(route[dstA - 1], route[dstA]);
		// Wenn A und B nicht direkt vor dstB weggeschoben werden UND dstA und dstB nicht gleich sind
		if(dstB - srcA != 1 && dstB - srcB != 1 && dstA != dstB)
			val -= getDistanceForOptimization(route[dstB - 1], route[dstB]);

		int offset;
		
		offset = (dstA - srcB == 1 || dstA - srcA == 1) ? (srcB - srcA == 1) ? 3 : 2 : 1;
		val += getDistanceForOptimization(route[dstA - offset], route[srcA]);
		if(dstA != dstB) {
			val += getDistanceForOptimization(route[srcA], route[dstA]);
			val += getDistanceForOptimization(route[dstB - 1], route[srcB]);
		} else
			val += getDistanceForOptimization(route[srcA], route[srcB]);

		val += getDistanceForOptimization(route[srcB], route[dstB]);

		// Wenn vor dstB NICHT irgend etwas weggeschoben wird
		offset = (srcB - srcA == 1) ? 2 : 1;
		if(dstB - srcA != offset && dstA - srcA != offset) {
			// Wenn srcA und srcB direkt nebeneinander liegen, dann �berspringe srcB
			offset = (srcB - srcA == 1) ? 2 : 1;
			val += getDistanceForOptimization(route[srcA - 1], route[srcA + offset]);
		}
		// Wenn nicht gilt, dass srcB vor dstA eingefügt wird oder
		// srcA vor dstA eingefügt wird und srcA und srcB hintereinander liegen
		if(srcB - srcA != 1 && ! (dstA - srcB == 1 || (dstA - srcA == 1 && srcB - srcA == 1)))
			val += getDistanceForOptimization(route[srcB - 1], route[srcB + 1]);

		return val;
	}

	private int[] getRouteIndex(Node[] route) {
		int[] routeIdxArr = new int[route.length];
		int id = 0;
		for (int i = 1; i < route.length; i++) {
			if(route[i].getSiteType() == SiteType.DEPOT)
				id++;
			routeIdxArr[i] = id;
		}
		return routeIdxArr;
	}

	private int[] getShipmentPositions(Node[] route) {
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
		return shipmentPairIdxArr;
	}
}
