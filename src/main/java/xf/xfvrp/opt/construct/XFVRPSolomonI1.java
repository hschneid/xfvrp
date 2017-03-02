package xf.xfvrp.opt.construct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.Util;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPLPBridge;
import xf.xfvrp.opt.XFVRPOptBase;


/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class XFVRPSolomonI1 extends XFVRPOptBase {

	public static float alpha = 0.5f, lamda = 1, mueh = 1, seed = 1;

	/**
	 * 
	 * @param unrecognized may be null - only for interface reasons
	 * 
	 * @return giant tour
	 */
	@Override
	public Solution execute(Solution solution) {
		final Node[] nodeArr = model.getNodeArr();
		if(model.getNbrOfDepots() > 1)
			throw new UnsupportedOperationException(XFVRPSolomonI1.class.getName()+" supports no multi depot");
		if(model.getNbrOfReplenish() > 0)
			throw new UnsupportedOperationException(XFVRPSolomonI1.class.getName()+" supports no replenishing depot");

		final Node depot = nodeArr[0];
		final float[] capacity = model.getVehicle().capacity;
		final float maxRouteDuration = model.getVehicle().maxRouteDuration;

		List<Node> customerList = new ArrayList<>(
				Arrays.asList(
						Arrays.copyOfRange(nodeArr, 1, nodeArr.length)
				)
		);

		int depotId = 0;

		// Erzeuge Giant-Tour
		List<Node> giantTour = new ArrayList<>();
		depotId = addDepot(giantTour, depot, depotId);

		// Erzeuge Tour
		List<Node> tour = new ArrayList<>();
		depotId = addDepot(tour, depot, depotId);

		// Er�ffne eine Tour
		Node seedNode = customerList.remove(getSeed(depot, customerList)); 
		tour.add(seedNode);
		depotId = addDepot(tour, depot, depotId);

		while(customerList.size() > 0) {
			// Vorbereitung - Aufbau der bMap
			Map<Integer, Float>[] mapArr = createMaps(nodeArr, tour);
			Map<Integer, Float> bMap = mapArr[0];
			Map<Integer, Float> wMap = mapArr[1];

			// Bestimme neu einzuf�genden Kunden
			int[] nextCustomer = getNext(depot, customerList, tour, bMap, wMap, capacity, maxRouteDuration);

			// Ist diese Einf�gung ung�ltig, er�ffne eine neue Tour, schlie�e bisherige Tour ab
			if(nextCustomer != null 
					&& checkTour(tour, customerList, bMap, nextCustomer, capacity, maxRouteDuration)) {
				// F�ge n�chsten Kunden [0] an Position [1] hinzu
				tour.add(nextCustomer[1], customerList.get(nextCustomer[0]));

				customerList.remove(nextCustomer[0]);
			} else {
				if(nextCustomer != null)
					throw new IllegalStateException("next customer is not null!");

				// Setze bisherige Tour in GiantTour ein
				Node lastDepot = tour.remove(0);
				giantTour.addAll(tour);

				// Mache neue Tour auf
				tour.clear();

				tour.add(lastDepot);
				seedNode = customerList.remove(getSeed(depot, customerList));
				tour.add(seedNode);
				depotId = addDepot(tour, depot, depotId);
			}
		}

		// Den letzten Rest der Tour in GiantTour eintragen
		tour.remove(0);
		giantTour.addAll(tour);

		Solution newSolution = new Solution();
		newSolution.setGiantRoute(giantTour.toArray(new Node[0]));
		return newSolution;
	}

	/**
	 * 
	 * @param route
	 * @param customerList
	 * @param bMap
	 * @param nextCustomer
	 * @param capacity
	 * @param capacity2
	 * @param capacity3
	 * @param maxRouteDuration
	 * @return
	 */
	private boolean checkTour(
			List<Node> route,
			List<Node> customerList,
			Map<Integer, Float> bMap,
			int[] nextCustomer,
			float[] capacity,
			float maxRouteDuration
	) {
		// Gewichtsschranke pr�fen
		final Node insertedNode = customerList.get(nextCustomer[0]);
		if(!checkAmount(route, insertedNode, nextCustomer[1], capacity))
			return false;

		// Maximale Tourl�nge pr�fen
		{
			float duration = 0;
			for (int i = 0; i < route.size() - 1; i++)
				duration += getTime(route.get(i), route.get(i+1));
			if(duration > maxRouteDuration)
				return false;
		}

		// Versp�tung pr�fen
		float time = bMap.get(nextCustomer[1] - 1) + route.get(nextCustomer[1] - 1).getServiceTime();
		float[] tw = insertedNode.getTimeWindow(time);
		time = Math.max(tw[0], time + getTime(route.get(nextCustomer[1] - 1), insertedNode));
		if(time > tw[1])
			return false;
		time += insertedNode.getServiceTime();

		// Versp�tung pr�fen - Rest der Tour
		Node lastNode = insertedNode;
		for (int i = nextCustomer[1]; i < route.size(); i++) {
			tw = route.get(i).getTimeWindow(time);
			time = Math.max(tw[0], time + getTime(lastNode, route.get(i)));
			if(time > tw[1])
				return false;
			time += route.get(i).getServiceTime();

			lastNode = route.get(i);
		}
		return true;
	}

	/**
	 * 
	 * @param route
	 * @param insertedNode
	 * @param insertPos
	 * @param capacity
	 * @param capacity2
	 * @param capacity3
	 * @return
	 */
	private boolean checkAmount(List<Node> route, Node insertedNode, int insertPos, float[] capacity) {
		float[] amountArr = new float[9];

		for (int i = 0; i < insertPos; i++)
			addAmount(route.get(i), amountArr);
		addAmount(insertedNode, amountArr);
		for (int i = insertPos; i < route.size(); i++)
			addAmount(route.get(i), amountArr);

		for (int i = 0; i < amountArr.length; i++)
			if (amountArr[i] > capacity[i])	return false;
		return true;
	}

	/**
	 * 
	 * @param n
	 * @param amountArr
	 */
	private void addAmount(Node n, float[] amountArr) {
		for (int j = 0; j < amountArr.length; j++) {
			float delivery = (n.getLoadType() == LoadType.DELIVERY) ? n.getDemand()[j] : 0;
			float pickup = (n.getLoadType() == LoadType.PICKUP) ? n.getDemand()[j] : 0;
			amountArr[j] -= delivery;
			amountArr[j] += pickup;
		}
	}

	/**
	 * 
	 * @param n
	 * @param amountArr
	 */
	private boolean checkAmount(Node n, float[] amountArr, float[] capacity) {
		for (int j = 0; j < amountArr.length; j++) {
			float delivery = (n.getLoadType() == LoadType.DELIVERY) ? n.getDemand()[j] : 0;
			float pickup = (n.getLoadType() == LoadType.PICKUP) ? n.getDemand()[j] : 0;
			if (amountArr[j] - delivery < 0) return false;
			if (amountArr[j] + pickup > capacity[j]) return false;
		}
		return true;
	}

	/**
	 * @param nodeArr
	 * @param tour
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Map<Integer, Float>[] createMaps(Node[] nodeArr, List<Node> tour) {
		Map<Integer, Float> bMap = new HashMap<>();
		Map<Integer, Float> wMap = new HashMap<>();
		float time = nodeArr[0].getTimeWindow(0)[0];
		bMap.put(0, time);
		wMap.put(0, 0f);
		for (int j = 1; j < tour.size(); j++) {
			float[] tw = tour.get(j).getTimeWindow(time);

			float nextArrival = time + getTime(tour.get(j-1), tour.get(j));
			time = Math.max(nextArrival, tw[0]);
			bMap.put(j, time);
			wMap.put(j, Math.max(0, tw[0] - nextArrival));
			time += tour.get(j).getServiceTime();
		}
		return new Map[]{bMap, wMap};
	}

	/**
	 * 
	 * @param depot
	 * @param customerList
	 * @param route
	 * @param bMap
	 * @param wMap
	 * @param currentLoad
	 * @param capacity1
	 * @param capacity2
	 * @param capacity3
	 * @param maxRouteDuration
	 * @return
	 */
	private int[] getNext(
			Node depot,
			List<Node> customerList,
			List<Node> route,
			Map<Integer, Float> bMap,
			Map<Integer, Float> wMap,
			float[] capacity, float maxRouteDuration
	) {

		// Ermittlung der aktuellen Tourl�nge
		float currentDuration = 0;
		float[] currentAmount = new float[9];
		for (int i = 0; i < route.size() - 1; i++) {
			addAmount(route.get(i), currentAmount);
			currentDuration += getTime(route.get(i), route.get(i+1));
		}

		int bestCustomer = -1;
		int bestPosition2 = -1;
		float bestValue2 = -Float.MAX_VALUE;
		for (int i = 0; i < customerList.size(); i++) {
			Node cust = customerList.get(i);

			// Kapazit�tspr�fung
			if(!checkAmount(cust, currentAmount, capacity))
				continue;

			// Auswahl der besten Einf�geposition
			boolean found = false;
			int bestPosition = -1;
			float bestValue = Float.MAX_VALUE;
			for (int j = 0; j < route.size() - 1; j++) {
				float[] tw = cust.getTimeWindow(0);

				// Pr�fung, der Validit�t der Einf�geposition (Berechnung der Entladezeit an cust)
				final float bu = Math.max(bMap.get(j) + route.get(j).getServiceTime() + getTime(route.get(j), cust), tw[0]);
				if(bu > tw[1])
					break;

				float c11 = 
					getDistanceForOptimization(route.get(j), cust) +
					getDistanceForOptimization(cust, route.get(j+1)) -
					mueh * getDistanceForOptimization(route.get(j), route.get(j+1));

				// Neue Entladezeit des Nachfolgers von Knoten u
				float c12 = getNewBj(route.get(j), cust, route.get(j+1), bMap.get(j)) - bMap.get(j+1);
				float v = (alpha * c11 + (1-alpha)* c12);

				// Pr�fung der g�ltigen Tourl�nge
				float addedDuration = 
					getTime(route.get(j), cust) +
					getTime(cust, route.get(j+1)) -
					getTime(route.get(j), route.get(j+1));

				if(currentDuration + addedDuration > maxRouteDuration)
					continue;

				// Zeitliche G�ltigkeitspr�fung
				if(!check(route, j + 1, c12, bMap, wMap))
					continue;

				// Efficient Load - Laderaumplanung
				// Komplexe Kapazit�tspr�fung
				if(model.getParameter().isWithLoadPlanning()) {
					Quality q = new Quality(null);
					List<Node> newTour = new ArrayList<>(route);
					newTour.add(cust);
					Node[] newGiantTour = newTour.toArray(new Node[0]);
					
					Solution solution = new Solution();
					solution.setGiantRoute(newGiantTour);
					XFVRPLPBridge.check(solution, null, model, q);

					if(q.getPenalty() > 0)
						continue;
				}

				if(v < bestValue) {
					bestValue = v;
					bestPosition = j+1;
					found = true;
				}	
			}

			// Auswahl des besten Kundens
			if(found) {
				float v = (lamda * getDistanceForOptimization(depot, cust)) - bestValue;
				if(v > bestValue2) {
					bestCustomer = i;
					bestPosition2 = bestPosition;
					bestValue2 = v;
				}
			}
		}

		if(bestCustomer == -1)
			return null;
		return new int[]{bestCustomer, bestPosition2};
	}

	/**
	 * 
	 * @param tour
	 * @param idx
	 * @param pf
	 * @param bMap
	 * @param wMap
	 * @return
	 */
	private boolean check(List<Node> tour, int idx, float pf, Map<Integer, Float> bMap, Map<Integer, Float> wMap) {
		float lastPF = pf;
		for (int i = idx; i < tour.size(); i++) {
			float nextPF = Math.max(0, lastPF - wMap.get(i));

			if(bMap.get(i) + nextPF > tour.get(i).getTimeWindow(0)[1])
				return false;
			if(nextPF <= 0)
				return true;

			lastPF = nextPF;
		}
		return true;
	}

	/**
	 * 
	 * @param i
	 * @param u
	 * @param j
	 * @param bi
	 * @return
	 */
	private float getNewBj(Node i, Node u, Node j, float bi) {
		float bu = Math.max(bi + i.getServiceTime() + getTime(i, u), u.getTimeWindow(0)[0]);
		return Math.max(bu + u.getServiceTime() + getTime(u, j), j.getTimeWindow(0)[0]);
	}

	/**
	 * 
	 * @param depot
	 * @param customerList
	 * @return
	 */
	private int getSeed(Node depot, List<Node> customerList) {
		if(seed == 1)
			return getSeed1(depot, customerList);

		return getSeed2(customerList);
	}

	/**
	 * 
	 * @param depot
	 * @param customerList
	 * @return
	 */
	private int getSeed1(Node depot, List<Node> customerList) {
		int bestSeed = -1;
		float bestValue = -1;
		for (int i = 0; i < customerList.size(); i++) {
			Node cust = customerList.get(i);
			float d = getDistance(depot, cust);
			if(d > bestValue) {
				bestValue = d;
				bestSeed = i;
			}
		}
		return bestSeed;
	}

	/**
	 * 
	 * @param customerList
	 * @return
	 */
	private int getSeed2(List<Node> customerList) {
		int bestSeed = -1;
		float bestValue = Float.MAX_VALUE;
		for (int i = 0; i < customerList.size(); i++) {
			Node cust = customerList.get(i);
			if(cust.getTimeWindow(0)[1] < bestValue) {
				bestValue = cust.getTimeWindow(0)[1];
				bestSeed = i;
			}
		}
		return bestSeed;
	}

	/**
	 * 
	 * @param tour
	 * @param depot
	 * @param depotId
	 * @return
	 */
	private int addDepot(List<Node> tour, Node depot, int depotId) {
		tour.add(Util.createIdNode(depot, depotId));

		return depotId+1;
	}
}
