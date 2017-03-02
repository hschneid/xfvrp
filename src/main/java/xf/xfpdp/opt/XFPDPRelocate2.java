package xf.xfpdp.opt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import xf.xfpdp.XFPDPUtils;
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
public class XFPDPRelocate2 extends XFVRPOptImpBase {

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) {
		final Set<String> loadingFootprint = getLoadingFootprint(solution);
		Node[] giantTour = solution.getGiantRoute();

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
			XFPDPUtils.move2(giantTour, srcA, srcB, dstA, dstB);

			Solution newSolution = new Solution();
			newSolution.setGiantRoute(giantTour);
			Quality result = check(newSolution, loadingFootprint);
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
		// Split giant route into single route informations
		List<int[]> routeList = new ArrayList<>();
		{
			int[] arr = new int[] {0,-1};
			for (int i = 1; i < route.length; i++) 
				if(route[i].getSiteType() == SiteType.DEPOT) {
					arr[1] = i;
					routeList.add(arr);
					arr = new int[] {i,-1};
				}
		}

		// Collect the nodes for each shipment
		int nbrOfShipments = 0;
		int[][] shipmentsArr = new int[route.length][2];
		{
			for (int i = 1; i < route.length; i++) {
				if(route[i].getSiteType() == SiteType.DEPOT)
					continue;

				int loadTypeIdx = (route[i].getDemand()[0] > 0) ? 0 : 1;

				shipmentsArr[route[i].getShipmentIdx()][loadTypeIdx] = i;

				nbrOfShipments = Math.max(nbrOfShipments, route[i].getShipmentIdx());
			}
			nbrOfShipments++;
		}

		// For all routes
		PreCheckMethod preCheck = new PreCheckMethod(model);
		for (int i = 0; i < routeList.size(); i++) {
			// Starting Depot position
			int startPos = routeList.get(i)[0];
			// Ending Depot position
			int endPos = routeList.get(i)[1];

			// Add depot into PreCheckMethod
			preCheck.addDepot(route[startPos]);

			// For all shipments
			for (int j = 0; j < nbrOfShipments; j++) {
				if(shipmentsArr[j] == null)
					continue;

				int srcA = shipmentsArr[j][0];
				int srcB = shipmentsArr[j][1];
				Node a = route[srcA];
				Node b = route[srcB];

				// Calculate removal cost of srcA and srcB
				float removalCost = getRemovalCost(route, srcA, srcB);

				// For pos A
				for (int dstA = startPos + 1; dstA < endPos; dstA++) {
					// Skip if next node is a
					if(route[dstA] == a)
						dstA++;
					// Skip if next node of next node is b
					if(route[dstA] == b)
						dstA++;
					
					// Check primary constraints for A
					if(!preCheck.addVirtual(a, route[dstA]))
						continue;

					// Store context in PreCheckMethod
					preCheck.store();

					// Add A into PreCheckMethod
					preCheck.add(a);

					// For pos B 
					for (int dstB = dstA; dstB < endPos; dstB++) {
						// Skip if next node is a
						if(route[dstB] == a)
							dstB++;
						// Skip if next node of next node is b
						if(route[dstB] == b)
							dstB++;

						// Check primary constraints for B
						if(!preCheck.addVirtual(b, route[dstB]))
							continue;

						// Calculate insertion cost of A at dstA and B at dstB
						float insertionCost = getInsertionCost(route, dstA, dstB, srcA, srcB);

						if(removalCost + insertionCost < -epsilon)
							improvingStepList.add(new float[]{srcA, srcB, dstA, dstB, -(removalCost + insertionCost)});

						// Add next regular node of route (except shipment nodes)
						if(!preCheck.add(route[dstB]))
							break;
					}

					// Reload context in PreCheckMethod
					preCheck.reload();

					// Add next regular node of route
					preCheck.add(route[dstA]);
				}

				preCheck.reset();

				// Add depot into PreCheckMethod
				preCheck.addDepot(route[startPos]);
			}
		}
	}

	/**
	 * Returns the resulting distance, if source is removed.
	 * 
	 * Attention: That does not mean, that all relevant costs are evaluated,
	 * because the removal is dependent on the destinations, if source nodes
	 * are on the same route like the destinations.
	 * 
	 * @param route
	 * @param srcA
	 * @param srcB
	 * @return
	 */
	private float getRemovalCost(Node[] route, int srcA, int srcB) {
		float val = 0;

		val -= getDistanceForOptimization(route[srcA - 1], route[srcA]);
		val -= getDistanceForOptimization(route[srcA], route[srcA + 1]);
		val -= getDistanceForOptimization(route[srcB], route[srcB + 1]);
		if(srcB - srcA != 1)
			val -= getDistanceForOptimization(route[srcB - 1], route[srcB]);

		return val;
	}

	/**
	 * Returns the resulting distance, if source nodes are inserted at destintations.
	 * 
	 * @param route
	 * @param dstA
	 * @param dstB
	 * @param srcA
	 * @param srcB
	 * @return
	 */
	private float getInsertionCost(Node[] route, int dstA, int dstB, int srcA, int srcB) {
		float val = 0;

		boolean srcAB = (srcB - srcA == 1);

		// Ist dstA = dstB sollen die beiden Knoten direkt hintereinander liegen
		if(dstA == dstB) {
			val += getDistanceForOptimization(route[srcA], route[srcB]);
			val += getDistanceForOptimization(route[srcB], route[dstB]);
		}

		// Ein Source-Knoten ist direkt vor dstA
		if(!srcAB && (dstA - srcA == 1 || dstA - srcB == 1)) {
			val += getDistanceForOptimization(route[dstA - 2], route[srcA]);
			val += getDistanceForOptimization(route[srcA], route[dstA]);
		}

		// Ein Source-Knoten ist direkt vor dstB
		if(!srcAB && (dstB - srcA == 1 || dstB - srcB == 1)) {
			val += getDistanceForOptimization(route[dstB - 2], route[srcB]);
			val += getDistanceForOptimization(route[srcB], route[dstB]);
		}

		// Alle beide Source-Knoten liegen vor dstA
		if(dstA - srcA == 2 && dstA - srcB == 1) {
			val += getDistanceForOptimization(route[dstA - 3], route[srcA]);
			if(dstA != dstB)
				val += getDistanceForOptimization(route[srcA], route[dstA]);
		} else
			// Alle beide Source-Knoten liegen vor dstB
			if(dstB - srcA == 2 && dstB - srcB == 1) {
				val += getDistanceForOptimization(route[dstB - 3], route[srcB]);
				if(dstA != dstB)
					val += getDistanceForOptimization(route[srcB], route[dstB]);
			}

		// Vor dstA liegt kein Source-Knoten und nur einer soll eingefügt werden
		if(dstA != dstB && dstA - srcA != 1 && dstA - srcB != 1) {
			val -= getDistanceForOptimization(route[dstA - 1], route[dstA]);
			val += getDistanceForOptimization(route[dstA - 1], route[srcA]);
			val += getDistanceForOptimization(route[srcA], route[dstA]);
		}

		// Vor dstB liegt kein Source-Knoten und nur einer soll eingefügt werden
		if(dstA != dstB && dstB - srcA != 1 && dstB - srcB != 1) {
			val -= getDistanceForOptimization(route[dstB - 1], route[dstB]);
			val += getDistanceForOptimization(route[dstB - 1], route[srcB]);
			val += getDistanceForOptimization(route[srcB], route[dstB]);
		}
		
		// Vor dstA liegt kein Source-Knoten und beide sollen eingefügt werden
		if(dstA == dstB && dstA - srcA != 1 && dstA - srcB != 1) {
			val -= getDistanceForOptimization(route[dstA - 1], route[dstA]);
			val += getDistanceForOptimization(route[dstA - 1], route[srcA]);
		}

		// srcA liegt vor keinem Einfügepunkt und ist einzeln
		if(!srcAB) {
			if(dstA - srcA != 1 && dstB - srcA != 1)
				val += getDistanceForOptimization(route[srcA - 1], route[srcA + 1]);
			// srcB liegt vor keinem Einfügepunkt und ist einzeln
			if(dstA - srcB != 1 && dstB - srcB != 1)
				val += getDistanceForOptimization(route[srcB - 1], route[srcB + 1]);
		} else { 
			// srcA und srcB liegen zusammen vor keinem Einfügepunkt
			if(dstA - srcB != 1 && dstB - srcB != 1)
				val += getDistanceForOptimization(route[srcA - 1], route[srcB + 1]);
		}

		return val;
	}
}
