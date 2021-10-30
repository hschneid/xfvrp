package xf.xfvrp.opt.construct;

import xf.xfvrp.base.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class SavingsDataBag {

	private List<Node> nodeList = new ArrayList<>();
	private int[] routeIdxForStartNode;
	private int[] routeIdxForEndNode;
	private List<float[]> savingsMatrix = new ArrayList<>();
	private final Node[][] routes;

	public SavingsDataBag(Node[][] routes) {
		this.routes = routes;
	}

	public void addSaving(int srcIdx, int dstIdx, float potential) {
		savingsMatrix.add(new float[] {srcIdx, dstIdx, 1000f / potential});
	}
	
	public List<Node> getNodeList() {
		return nodeList;
	}
	public void setNodeList(List<Node> nodeList) {
		this.nodeList = nodeList;
	}
	public int[] getRouteIdxForStartNode() {
		return routeIdxForStartNode;
	}
	public void setRouteIdxForStartNode(int[] routeIdxForStartNode) {
		this.routeIdxForStartNode = routeIdxForStartNode;
	}
	public int[] getRouteIdxForEndNode() {
		return routeIdxForEndNode;
	}
	public void setRouteIdxForEndNode(int[] routeIdxForEndNode) {
		this.routeIdxForEndNode = routeIdxForEndNode;
	}
	public List<float[]> getSavingsMatrix() {
		return savingsMatrix;
	}

	public Node[][] getRoutes() {
		return routes;
	}

}
