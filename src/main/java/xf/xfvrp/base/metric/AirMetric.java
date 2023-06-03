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
 * Air metric of DISMOD1 where the air distance for two nodes
 * are calculated. There is no memory or caching procdure in this
 * metric. This class ignores the vehicle parameter. So it is not
 * dependent to the kind of vehicle.
 * 
 * @author hschneid
 *
 */
public class AirMetric implements Metric {

	public static final double EARTH_RANGE = 40076.592;

	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getDistance(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float getDistance(Node src, Node dst, Vehicle veh) {
		return getDistance(src.getXlong(), src.getYlat(), dst.getXlong(), dst.getYlat());
	}

	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getTime(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float getTime(Node src, Node dst, Vehicle veh) {
		return (getDistance(src, dst, veh)/60f)*60f;
	}

	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getDistanceAndTime(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float[] getDistanceAndTime(Node src, Node dst, Vehicle veh) {
		return new float[]{getDistance(src, dst, veh), getTime(src, dst, veh)};
	}	
	
	/**
	 * 
	 * @param xlong1
	 * @param ylat1
	 * @param xlong2
	 * @param ylat2
	 * @return
	 */
	private float getDistance(float xlong1, float ylat1, float xlong2, float ylat2) {
		if (xlong1 == xlong2 && ylat1 == ylat2)
			return 0.0f;

		double dif1 = ToRadians(90 - ylat1);
		double dif2 = ToRadians(90 - ylat2);
		double diffLon = ToRadians(Math.abs(xlong2 - xlong1));
		double alpha = ToDegree(
				Math.acos(
						(Math.cos(dif1) * Math.cos(dif2)) + (Math.sin(dif1) * Math.sin(dif2) * Math.cos(diffLon) )
				)
			);
		double distance = (alpha * EARTH_RANGE) / 360.0d;
		return (float)distance;	
	}

	/**
	 *         
	 * @param angle
	 * @return
	 */
	private double ToDegree(double angle) {
		return angle * (180.0d / Math.PI);
	}

	/**
	 * 
	 * @param degrees
	 * @return
	 */
	private double ToRadians(double degrees) {
		return (Math.PI / 180.0d) * degrees;
	}
}
