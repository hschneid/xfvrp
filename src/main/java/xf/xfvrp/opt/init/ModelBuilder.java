package xf.xfvrp.opt.init;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.compartment.CompartmentType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator;
import xf.xfvrp.base.metric.internal.FixCostMetricTransformator;
import xf.xfvrp.base.metric.internal.OpenRouteMetricTransformator;
import xf.xfvrp.base.metric.internal.PresetMetricTransformator;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.BlockNameConverter;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class ModelBuilder {

    /**
     * Transforms the read data into a model, which can be used
     * for optimization.
     *
     * @param veh Contains parameters for capacity, max route duration and others.
     * @return Returns a model, which can be used for optimization procedures.
     */
    public XFVRPModel build(Node[] nodes, CompartmentType[] compartmentTypes, Vehicle veh, Metric externalMetric, XFVRPParameter parameter, StatusManager statusManager) throws XFVRPException {
        statusManager.fireMessage(StatusCode.RUNNING, "Initialisation of instance for vehicle " + veh.getName());

        // Set local node index
        indexNodes(nodes);

        // Metric transformations
        InternalMetric internalMetric = buildInternalMetric(nodes, veh, externalMetric, parameter);

        // Metric transformations for optimization
        InternalMetric optMetric = buildOptimizationMetric(nodes, veh, internalMetric);

        statusManager.fireMessage(StatusCode.RUNNING, "Nbr of nodes : " + nodes.length);

        XFVRPModel model = new XFVRPModel(nodes, compartmentTypes, internalMetric, optMetric, veh, parameter);

        countNbrOfNodesInBlocks(nodes, model);

        return model;
    }

    private void countNbrOfNodesInBlocks(Node[] nodes, XFVRPModel model) {
        int[] nbrOfNodesInBlocks = model.getBlockPresetCountList();
        for (int i = nodes.length - 1; i >= 0; i--)
            if (nodes[i].getPresetBlockIdx() >= BlockNameConverter.DEFAULT_BLOCK_IDX)
                nbrOfNodesInBlocks[nodes[i].getPresetBlockIdx()]++;
    }

    private InternalMetric buildInternalMetric(Node[] nodes, Vehicle veh, Metric metric, XFVRPParameter parameter) throws XFVRPException {
        InternalMetric internalMetric = AcceleratedMetricTransformator.transform(metric, nodes, veh);
        if (parameter.isOpenRouteAtStart() || parameter.isOpenRouteAtEnd())
            internalMetric = OpenRouteMetricTransformator.transform(internalMetric, nodes, parameter);
        return internalMetric;
    }

    private void indexNodes(Node[] nodes) {
        for (int i = 0; i < nodes.length; i++)
            nodes[i].setIdx(i);
    }

    private InternalMetric buildOptimizationMetric(Node[] nodes, Vehicle veh, InternalMetric internalMetric) {
        InternalMetric optMetric = FixCostMetricTransformator.transform(internalMetric, nodes, veh);
        return PresetMetricTransformator.transform(optMetric, nodes);
    }
}
