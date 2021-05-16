package xf.xfvrp.base.metric;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class DirectMatrixMetric implements Metric {

	private final float[][][][] matrix;
	
	/**
	 * 
	 * @param size
	 */
	public DirectMatrixMetric(int size) {
		matrix = new float[size][size][1][];
	}
	
	/**
	 * 
	 * @param size
	 * @param nbrOfVehicleMetricIds
	 */
	public DirectMatrixMetric(int size, int nbrOfVehicleMetricIds) {
		matrix = new float[nbrOfVehicleMetricIds][size][size][];
	}
	
	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getDistance(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float getDistance(Node src, Node dst, Vehicle veh) {
		return matrix[veh.vehicleMetricId][src.getIdx()][dst.getIdx()][0];
	}

	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getTime(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float getTime(Node src, Node dst, Vehicle veh) {
		return matrix[veh.vehicleMetricId][src.getIdx()][dst.getIdx()][1];
	}
	
	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getDistanceAndTime(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float[] getDistanceAndTime(Node src, Node dst, Vehicle veh) {
		return matrix[veh.vehicleMetricId][src.getIdx()][dst.getIdx()];
	}
	
	/**
	 * 
	 * @param srcIdx
	 * @param dstIdx
	 * @param dist
	 * @param time
	 */
	public void add(int srcIdx, int dstIdx, float dist, float time) {
		matrix[0][srcIdx][dstIdx] = new float[]{dist, time};
	}
	
	/**
	 * 
	 * @param srcIdx
	 * @param dstIdx
	 * @param vehicleMetricId
	 * @param dist
	 * @param time
	 */
	public void add(int srcIdx, int dstIdx, int vehicleMetricId, float dist, float time) {
		matrix[vehicleMetricId][srcIdx][dstIdx] = new float[]{dist, time};
	}
}
