package xf.xfvrp.opt.init;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.init.solution.vrp.VRPInitialSolutionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class PresetSolutionBuilder {

    private final VRPInitialSolutionBuilder initialSolutionBuilder = new VRPInitialSolutionBuilder();

    /**
     * Creates a first/initial solution from given preset information.
     * <p>
     * This builder is used, when user enters own solution data in input.
     */
    public Solution build(List<Node> nodes, XFVRPModel model, StatusManager statusManager) throws XFVRPException {
        String predefinedSolutionString = model.getParameter().getPredefinedSolutionString();

        if (!checkPredefinedSolutionString(predefinedSolutionString))
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "The predefined solution string " + predefinedSolutionString + " is not valid.");

        PresetSolutionBuilderDataBag dataBag = prepare(nodes, model);

        // Separate the solution string into the blocks
        Solution solution = new Solution(model);
        for (String presetBlock : split(predefinedSolutionString)) {
            readPresetBlock(presetBlock, solution, dataBag, statusManager);
        }

        // Put the unassigned customers with single routes into the solution
        if (dataBag.getAvailableCustomers().size() > 0) {
            Solution unassigedSolution = initialSolutionBuilder.generateSolution(
                    new ArrayList<>(dataBag.getAvailableCustomers()),
                    model
            );
            solution.addRoutes(unassigedSolution.getRoutes());
        }

        return solution;
    }

    private PresetSolutionBuilderDataBag prepare(List<Node> nodes, XFVRPModel model) {
        PresetSolutionBuilderDataBag dataBag = new PresetSolutionBuilderDataBag();

        dataBag.setModel(model);
        dataBag.setNodes(Arrays.asList(model.getNodes()));

        // Generate a map for each extern id to a node index
        for (Node node : model.getNodes()) {
            dataBag.addNodeId(node, node.getIdx());
        }

        dataBag.setAvailableCustomers(new HashSet<>(nodes));
        return dataBag;
    }

    private String[] split(String predefinedSolutionString) {
        predefinedSolutionString = predefinedSolutionString.substring(1, predefinedSolutionString.length() - 1);
        String[] predefinedBlocks = predefinedSolutionString.split("\\),\\(");
        predefinedBlocks[0] = predefinedBlocks[0].substring(1);
        predefinedBlocks[predefinedBlocks.length - 1] = predefinedBlocks[predefinedBlocks.length - 1].substring(0, predefinedBlocks[predefinedBlocks.length - 1].length() - 1);
        return predefinedBlocks;
    }

    private void readPresetBlock(String block, Solution solution, PresetSolutionBuilderDataBag dataBag, StatusManager statusManager) {
        String[] entries = block.split(",");

        if (entries.length == 0)
            return;

        // Every block has to start with a any depot
        List<Node> route = new ArrayList<>();
        route.add(dataBag.getNextDepot());

        // A block can hold customers and depots
        for (int i = 0; i < entries.length; i++) {
            if (dataBag.containsNode(entries[i].trim())) {
                addEntry(route, dataBag, entries[i], solution, statusManager);
            } else {
                statusManager.fireMessage(StatusCode.RUNNING, " Init warning - Node " + entries[i] + " is no valid customer (unknown).");
            }
        }

        // Every block has to end with a depot
        route.add(dataBag.getNextDepot(route.get(0)));

        solution.addRoute(route.toArray(new Node[0]));
    }

    /**
     * Adds a preset block to the solution
     * <p>
     * Each block is placed on one route. If block contains depots, if block is splited
     * into further single routes.
     */
    private void addEntry(List<Node> route, PresetSolutionBuilderDataBag dataBag, String entry, Solution solution, StatusManager statusManager) {
        Node n = dataBag.getNode(entry);

        if (n.getSiteType() == SiteType.DEPOT) {
            route.add(dataBag.getNextDepot(route.get(0)));
            solution.addRoute(route.toArray(new Node[0]));
            route.clear();
            route.add(dataBag.getNextDepot(n));
        } else if (dataBag.getAvailableCustomers().contains(n)) {
            route.add(n);
            dataBag.getAvailableCustomers().remove(n);
        } else {
            statusManager.fireMessage(StatusCode.RUNNING, " Init warning - Node " + entry + " is already in the solution.");
        }
    }

    private boolean checkPredefinedSolutionString(String predefinedSolutionString) {
        return predefinedSolutionString.matches("\\{(\\([^()]+\\),)*(\\([^()]+\\))+}");
    }
}
