package xf.xfvrp.opt.init;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class PresetSolutionBuilderDataBag {

    private Set<Node> availableCustomers;
    private final Map<String, Integer> idMap = new HashMap<>();
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

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void setModel(XFVRPModel model) {
        this.model = model;
    }

}
