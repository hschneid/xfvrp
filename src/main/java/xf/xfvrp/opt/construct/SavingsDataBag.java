package xf.xfvrp.opt.construct;

import java.util.ArrayList;
import java.util.List;

import xf.xfvrp.base.Node;

public class SavingsDataBag {

	private List<Node> nodeList = new ArrayList<>();
	private int[] routeIdxForStartNode;
	private int[] routeIdxForEndNode;
	private List<float[]> savingsMatrix = new ArrayList<>();
	private Node[][] routes;
	
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

	public void setRoutes(Node[][] routes) {
		this.routes = routes;
	}
	
	
}
