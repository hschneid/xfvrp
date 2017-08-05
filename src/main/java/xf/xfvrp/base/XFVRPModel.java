package xf.xfvrp.base;

import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.preset.BlockNameConverter;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * The XFVRPModel holds all necessary input data. The
 * nodeArr contains all read nodes with their attitudes.
 * The metric can only be reached through this model for
 * getting distances or times between nodes. 
 * 
 * @author hschneid
 *
 */
public class XFVRPModel {

	protected final int nbrOfDepots;
	protected final int nbrOfReplenish;

	protected final Node[] nodeArr;
	protected final InternalMetric metric;
	protected final InternalMetric optMetric;

	protected final Vehicle vehicle;

	protected final XFVRPParameter parameter;
	
	/* Number of customer nodes for each preset block */
	protected final int[] blockCountArr;

	protected final int maxGlobalNodeIdx;

	/**
	 * Initialize an optimization model object with the given input data. It contains the general
	 * parameter for all optimization procedures. It holds no solution information.
	 *
	 * @param nodeArr
	 * @param metric Metric that will be used to evaluate solutions
	 * @param optMetric Metric that will be used for optimization processes
	 * @param vehicle
	 * @param parameter
	 */
	public XFVRPModel(Node[] nodeArr, InternalMetric metric, InternalMetric optMetric, Vehicle vehicle, XFVRPParameter parameter) {
		this.nodeArr = nodeArr;
		this.metric = metric;
		this.optMetric = optMetric;
		this.vehicle = vehicle;
		this.parameter = parameter;

		// Counts the number of different depots and max block number
		int nbrOfDepots = 0;
		int nbrOfReplenish = 0;
		int nbrOfBlocks = BlockNameConverter.UNDEF_BLOCK_IDX;
		int maxGlobalNodeIdx = 0;
		for (int i = 0; i < nodeArr.length; i++) {
			if(nodeArr[i].getSiteType() == SiteType.DEPOT)
				nbrOfDepots++;
			
			if(nodeArr[i].getSiteType() == SiteType.REPLENISH)
				nbrOfReplenish++;
			
			nbrOfBlocks = Math.max(nbrOfBlocks, nodeArr[i].getPresetBlockIdx());
			maxGlobalNodeIdx = Math.max(maxGlobalNodeIdx, nodeArr[i].getGlobalIdx());
		}
		
		// Counts for each block idx the number of nodes in it.
		blockCountArr = new int[Math.max(0, nbrOfBlocks) + 1];
		
		this.maxGlobalNodeIdx = maxGlobalNodeIdx;
		this.nbrOfDepots = nbrOfDepots;
		this.nbrOfReplenish = nbrOfReplenish;
	}

	/**
	 * Returns the travel time between two given XFNodes
	 * 
	 * @param n1 source node
	 * @param n2 destination node
	 * @return travel time
	 */
	public float getTime(Node n1, Node n2) {
		return metric.getTime(n1, n2);
	}

	/**
	 * Returns the distance between two given Nodes
	 * 
	 * @param n1 source node
	 * @param n2 destination node
	 * @return distance
	 */
	public float getDistance(Node n1, Node n2) {
		return metric.getDistance(n1, n2);
	}
	
	/**
	 * Returns the distance between two given Nodes
	 * for the purposes of optimization. This value
	 * may differ from real distances.
	 * 
	 * @param n1 source node
	 * @param n2 destination node
	 * @return distance for optimization
	 */
	public float getDistanceForOptimization(Node n1, Node n2) {
		return optMetric.getDistance(n1, n2);
	}

	/**
	 * Returns the distance and time between two given XFNodes
	 * 
	 * @param n1 source node
	 * @param n2 destination node
	 * @return a tupel of distance (0) and travel time (1)
	 */
	public float[] getDistanceAndTime(Node n1, Node n2) {
		return metric.getDistanceAndTime(n1, n2);
	}

	/**
	 * 
	 * @return
	 */
	public int getNbrOfDepots() {
		return nbrOfDepots;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getNbrOfReplenish() {
		return nbrOfReplenish;
	}

	/**
	 * 
	 * @return Number of Nodes in the current model (inclusive depots)
	 */
	public int getNbrOfNodes() {
		return nodeArr.length;
	}

	/**
	 * 
	 * @return
	 */
	public Vehicle getVehicle() {
		return vehicle;
	}

	/**
	 * 
	 * @return
	 */
	public XFVRPParameter getParameter() {
		return parameter;
	}

	/**
	 * 
	 * @return
	 */
	public Node[] getNodes() {
		return nodeArr;
	}

	/**
	 * 
	 * @return
	 */
	public int getNbrOfBlocks() {
		return blockCountArr.length;
	}

	/**
	 * 
	 * @return
	 */
	public int[] getBlockPresetCountList() {
		return blockCountArr;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getMaxGlobalNodeIdx() {
		return maxGlobalNodeIdx;
	}
}
