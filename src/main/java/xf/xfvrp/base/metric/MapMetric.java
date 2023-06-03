package xf.xfvrp.base.metric;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;

import java.util.HashMap;
import java.util.Map;


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
public class MapMetric implements Metric {

	private static final int DEFAULT_VEHICLE_METRIC_ID = 0;
	private Map<String, float[]> map = new HashMap<>();
	
	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getDistance(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float getDistance(Node src, Node dst, Vehicle veh) {
		return map.get(genKey(src.getGeoId(), dst.getGeoId(), veh.getVehicleMetricId()))[0];
	}

	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getTime(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float getTime(Node src, Node dst, Vehicle veh) {
		return map.get(genKey(src.getGeoId(), dst.getGeoId(), veh.getVehicleMetricId()))[1];
	}
	
	/*
	 * (non-Javadoc)
	 * @see xftour.model.metric.Metric#getDistanceAndTime(xftour.model.XFNode, xftour.model.XFNode)
	 */
	@Override
	public float[] getDistanceAndTime(Node src, Node dst, Vehicle veh) {
		return map.get(genKey(src.getGeoId(), dst.getGeoId(), veh.getVehicleMetricId()));
	}
	
	/**
	 * 
	 * @param srcId
	 * @param dstId
	 * @param dist
	 */
	public void addDist(int srcId, int dstId, float dist) {
		String key = genKey(srcId, dstId, DEFAULT_VEHICLE_METRIC_ID);
		if(!map.containsKey(key))
			map.put(key, new float[]{dist, -1});
		else
			map.get(key)[0] = dist;
	}
	
	/**
	 * 
	 * @param srcId
	 * @param dstId
	 * @param time
	 */
	public void addTime(int srcId, int dstId, float time) {
		String key = genKey(srcId, dstId, DEFAULT_VEHICLE_METRIC_ID);
		if(!map.containsKey(key))
			map.put(key, new float[]{-1, time});
		else
			map.get(key)[1] = time;
	}
	
	/**
	 * 
	 * @param srcId
	 * @param dstId
	 * @param dist
	 * @param time
	 */
	public void add(int srcId, int dstId, float dist, float time) {
		map.put(genKey(srcId, dstId, DEFAULT_VEHICLE_METRIC_ID), new float[]{dist, time});
	}
	
	/**
	 * 
	 * @param srcId
	 * @param dstId
	 * @param vehicleMetricId
	 * @param dist
	 * @param time
	 */
	public void add(int srcId, int dstId, int vehicleMetricId, float dist, float time) {
		map.put(genKey(srcId, dstId, vehicleMetricId), new float[]{dist, time});
	}

	/**
	 * Generates an unique key for the two given ids.
	 * 
	 * @param srcId
	 * @param dstId
	 * @param vehicleMetricId
	 * @return a unique key for this pair of ids
	 */
	private String genKey(int srcId, int dstId, int vehicleMetricId) {
		return srcId+"#"+dstId+"#"+vehicleMetricId;
	}
}
