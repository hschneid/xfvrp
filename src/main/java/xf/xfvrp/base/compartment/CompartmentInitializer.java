package xf.xfvrp.base.compartment;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 */
public class CompartmentInitializer {

    /**
     * Check compartment types, ensure that all entities (node or vehicle) have the same number of compartments
     * Maybe automatically detect compartments
     */
    public static void check(Node[] nodes, List<CompartmentType> compartmentTypes, Vehicle[] vehicles) {
        int nbrOfCompartments = getMaxNbrOfCompartments(nodes, vehicles);

        adjustNodes(nodes, nbrOfCompartments);
        adjustVehicles(vehicles, nbrOfCompartments);
        List<CompartmentType> checkedTypes = detectTypes(nodes, compartmentTypes, nbrOfCompartments);

        // Reset the checked compartment types
        compartmentTypes.clear();
        compartmentTypes.addAll(checkedTypes);
    }

    private static void adjustNodes(Node[] nodes, int nbrOfCompartments) {
        Arrays.stream(nodes)
                .filter(node -> node.getDemand().length != nbrOfCompartments)
                .forEach(node -> node.setDemands(Arrays.copyOf(node.getDemand(), nbrOfCompartments)));
    }

    /**
     * If there are multiple vehicles, and the number of compartments is not the same for all ones,
     * then take the maximum and add missing capacity values at the end with default capacity (unlimited).
     */
    private static void adjustVehicles(Vehicle[] vehicles, int nbrOfCompartments) {
        Arrays.stream(vehicles)
                .filter(vehicle -> vehicle.getCapacity().length != nbrOfCompartments)
                .forEach(vehicle -> {
                    int nbrOfCompartmentsBefore = vehicle.getCapacity().length;
                    vehicle.setCapacity(Arrays.copyOf(vehicle.getCapacity(), nbrOfCompartments));
                    Arrays.fill(vehicle.getCapacity(), nbrOfCompartmentsBefore, vehicle.getCapacity().length, Float.MAX_VALUE);
                });
    }

    private static int getMaxNbrOfCompartments(Node[] nodes, Vehicle[] vehicles) {
        return IntStream
                .concat(
                        Arrays.stream(nodes).mapToInt(node -> node.getDemand().length),
                        Arrays.stream(vehicles).mapToInt(vehicle -> vehicle.getCapacity().length)
                )
                .max()
                .orElse(-1);
    }

    private static List<CompartmentType> detectTypes(
            Node[] nodes,
            List<CompartmentType> givenCompartmentTypes,
            int nbrOfNeededTypes) {

        List<CompartmentType> types = new ArrayList<>(nbrOfNeededTypes);

        // Set given types
        types.addAll(givenCompartmentTypes);

        // Set missing types
        int missingTypes = nbrOfNeededTypes - givenCompartmentTypes.size();
        for (int i = 0; i < missingTypes; i++) {
            types.add(
                    detectType(givenCompartmentTypes.size() + i, nodes)
            );
        }

        return types;
    }

    /**
     * Tries to identify a missing compartment type by checking the load types of customers
     */
    private static CompartmentType detectType(int compartmentIdx, Node[] nodes) {
        LoadType foundLoadType = null;

        for (int i = nodes.length - 1; i >= 0; i--) {
            if(nodes[i].getSiteType() == SiteType.CUSTOMER && nodes[i].getDemand()[compartmentIdx] != 0) {
                if(foundLoadType == null) {
                    foundLoadType = nodes[i].getLoadType();
                } else if(foundLoadType != nodes[i].getLoadType()) {
                    return CompartmentType.MIXED;
                }
            }
        }

        if(foundLoadType == null)
            return CompartmentType.MIXED;

        switch (foundLoadType) {
            case PICKUP: return CompartmentType.PICKUP;
            case DELIVERY: return CompartmentType.DELIVERY;
            default: return CompartmentType.MIXED;
        }
    }
}
