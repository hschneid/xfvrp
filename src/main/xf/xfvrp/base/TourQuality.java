package xf.xfvrp.base;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Tour qualities are used for acceleration
 * of check process, where for each tour on
 * the giant tour a associated tour quality
 * holds the costs and penalties.
 * 
 * In checking it is used for delta calculation
 * by removing the costs of original tours and
 * adding costs for the changed tour.
 * 
 * ATTENTION: The tour quality is currently not in use.
 * 
 * @author hschneid
 *
 */
public class TourQuality {
	
	private float fitness = 0, penalty = 0;
	
	/**
	 * 
	 * @param f
	 */
	public void setFitness(float f){
		fitness = f;
	}
	
	/**
	 * 
	 * @param p
	 */
	public void setPenalty(float p){
		penalty = p;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getFitness(){
		return fitness;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getPenalty(){
		return penalty;
	}

}
