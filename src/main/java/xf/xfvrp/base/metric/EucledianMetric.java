package xf.xfvrp.base.metric;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;

/** 
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class EucledianMetric implements Metric {

	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getDistance(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float getDistance(Node src, Node dst, Vehicle veh) {
		return calc(src.getXlong(), src.getYlat(), dst.getXlong(), dst.getYlat());
	}

	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getTime(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float getTime(Node src, Node dst, Vehicle veh) {
		return calc(src.getXlong(), src.getYlat(), dst.getXlong(), dst.getYlat());
	}
	
	/**
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	private float calc(float x1, float y1, float x2, float y2) {
		return (float)Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
	}

	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getDistanceAndTime(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float[] getDistanceAndTime(Node src, Node dst, Vehicle veh) {
		float f = calc(src.getXlong(), src.getYlat(), dst.getXlong(), dst.getYlat());
		return new float[]{f, f};
	}

}
