package xf.xfvrp.base.metric.internal;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.metric.InternalMetric;

/** 
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This class is a specialisation of an internal metric and is used
 * by all optimization procedures. 
 * 
 * This metric holds only distance information in an one-dimensional array for faster access.
 * 
 * @author hschneid
 *
 */
public class InternalOptMetric implements InternalMetric {

	private final int size;
	private final float[] matrix;

	/**
	 * 
	 * @param size
	 */
	public InternalOptMetric(int size) {
		this.size = size;
		matrix = new float[size * size];
	}

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xfvrp.base.metric.InternalMetric#getDistance(de.fhg.iml.vlog.xfvrp.base.Node, de.fhg.iml.vlog.xfvrp.base.Node)
	 */
	@Override
	public float getDistance(Node src, Node dst) {
		return matrix[src.getIdx() * size + dst.getIdx()];
	}
	
	/**
	 * Adds a distance for a src and a dst node. The index variable of
	 * the node has to be set. This is done by initialisation in XFVRP 
	 * execute methods.
	 * 
	 * @param src Source
	 * @param dst Destination
	 * @param distance Distance
	 */
	public void setDistance(Node src, Node dst, float distance) {
		matrix[src.getIdx() * size + dst.getIdx()] = distance;
	}

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xfvrp.base.metric.InternalMetric#getDistanceAndTime(de.fhg.iml.vlog.xfvrp.base.Node, de.fhg.iml.vlog.xfvrp.base.Node)
	 */
	@Override
	public float[] getDistanceAndTime(Node src, Node dst) {
		throw new UnsupportedOperationException("Method cannot be used.");
	}

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xfvrp.base.metric.InternalMetric#getTime(de.fhg.iml.vlog.xfvrp.base.Node, de.fhg.iml.vlog.xfvrp.base.Node)
	 */
	@Override
	public float getTime(Node src, Node dst) {
		throw new UnsupportedOperationException("Method cannot be used.");	
	}
}
