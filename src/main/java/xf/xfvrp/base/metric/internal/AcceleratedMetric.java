package xf.xfvrp.base.metric.internal;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.metric.InternalMetric;

/** 
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class AcceleratedMetric implements InternalMetric {

	private final float[][][] matrix;
	
	/**
	 * 
	 * @param size
	 */
	public AcceleratedMetric(int size) {
		matrix = new float[size][size][];
	}
	
	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xfvrp.base.metric.InternalMetric#getDistance(de.fhg.iml.vlog.xfvrp.base.Node, de.fhg.iml.vlog.xfvrp.base.Node)
	 */
	@Override
	public float getDistance(Node src, Node dst) {
		return matrix[src.getIdx()][dst.getIdx()][0];
	}

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xfvrp.base.metric.InternalMetric#getTime(de.fhg.iml.vlog.xfvrp.base.Node, de.fhg.iml.vlog.xfvrp.base.Node)
	 */
	@Override
	public float getTime(Node src, Node dst) {
		return matrix[src.getIdx()][dst.getIdx()][1];
	}
	
	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xfvrp.base.metric.InternalMetric#getDistanceAndTime(de.fhg.iml.vlog.xfvrp.base.Node, de.fhg.iml.vlog.xfvrp.base.Node)
	 */
	@Override
	public float[] getDistanceAndTime(Node src, Node dst) {
		return matrix[src.getIdx()][dst.getIdx()];
	}
	
	public void add(int srcIdx, int dstIdx, float dist, float time) {
		matrix[srcIdx][dstIdx] = new float[]{dist, time};
	}
}
