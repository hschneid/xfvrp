package xf.xfvrp.base;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Quality objects holds the quality of a routing solution.
 * Three values are stored:
 * 	- fitness is the cost of a solution, e.g. the distance or travel time
 * 	- penalty is the sum of all violations of constraints (overload, time window restrictions, route length, ...)
 *  - singe route qualities (TourQuality) for each route in the solution
 * 
 * @author hschneid
 *
 */
public class Quality {

	private float cost = 0;
	private float penalty = 0;

	private static int[] reasons = new int[10];
	
	public static final int PENALTY_REASON_CAPACITY = 0;
	public static final int PENALTY_REASON_PRESETTING = 1;
	public static final int PENALTY_REASON_DURATION = 2;
	public static final int PENALTY_REASON_BLACKLIST = 3;
	public static final int PENALTY_REASON_EFFLOAD = 4;
	public static final int PENALTY_REASON_DELAY = 5;
	public static final int PENALTY_REASON_STOPCOUNT = 6;

	public Quality() {
	}
	
	/**
	 * Initializes a new Quality object by cloneing the information
	 * from an existing Quality object.
	 * 
	 * @param q
	 */
	public Quality(Quality q) {
		if(q != null) {
			cost = q.getCost();
			penalty = q.getPenalty();
		}
	}

	/**
	 * 
	 * @return cost of solution (mostly the distance)
	 */
	public float getCost() {
		return cost;
	}

	/**
	 * The fitness is the weighted expression of the cost and the 
	 * penalty multiplied with 1000. This can have severe influence on the
	 * optimization process, if cost values are very high (e.g. optimizing in millimeters)
	 * 
	 * @return fitness (weighted factor of cost and 1000 * fitness)
	 */
	public float getFitness() {
		return cost + 1000 * penalty;
	}

	/**
	 * 
	 * @return
	 */
	public float getPenalty() {
		return penalty;
	}
	
	/**
	 * Adds a given value to the penalty value of this
	 * quality instance.
	 * 
	 * @param penalty
	 */
	public void addPenalty(float penalty, int reason) {
		this.penalty += penalty;
		if(penalty != 0)
			reasons[reason]++;
	}
	
	/**
	 * 
	 * @param cost
	 */
	public void addCost(float cost) {
		this.cost += cost;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return cost+" "+penalty;
	}
}
