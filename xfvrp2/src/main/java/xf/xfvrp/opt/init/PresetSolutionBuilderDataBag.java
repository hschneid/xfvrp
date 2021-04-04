package xf.xfvrp.opt.init;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;

public class PresetSolutionBuilderDataBag {

	private Set<Node> availableCustomers;
	private Map<String, Integer> idMap = new HashMap<>();
	private int depotIdx = 0;
	private int depotId = 0;
	private List<Node> nodes;
	private XFVRPModel model;
	
	public void addNodeId(Node node, int idx) {
		idMap.put(node.getExternID(), idx);
	}
	
	public Node getNextDepot(Node node) {
		Node depot = Util.createIdNode(node, depotId++);
		setNextDepotIdx();
		
		return depot;
	}
	
	public Node getNextDepot() {
		Node depot = Util.createIdNode(nodes.get(depotIdx++), depotId++);
		setNextDepotIdx();
		
		return depot;
	}
	
	private void setNextDepotIdx() {
		depotIdx = depotIdx % model.getNbrOfDepots();
	}
	
	public boolean containsNode(String externId) {
		return idMap.containsKey(externId);
	}
	
	public Node getNode(String externId) {
		return nodes.get(idMap.get(externId));
	}
	
	public Set<Node> getAvailableCustomers() {
		return availableCustomers;
	}
	public void setAvailableCustomers(Set<Node> availableCustomers) {
		this.availableCustomers = availableCustomers;
	}
	public Map<String, Integer> getIdMap() {
		return idMap;
	}
	public void setIdMap(Map<String, Integer> idMap) {
		this.idMap = idMap;
	}
	public int getDepotIdx() {
		return depotIdx;
	}
	public void setDepotIdx(int depotIdx) {
		this.depotIdx = depotIdx;
	}
	public int getDepotId() {
		return depotId;
	}
	public void setDepotId(int depotId) {
		this.depotId = depotId;
	}
	public List<Node> getNodes() {
		return nodes;
	}
	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

	public XFVRPModel getModel() {
		return model;
	}

	public void setModel(XFVRPModel model) {
		this.model = model;
	}	
	
}
