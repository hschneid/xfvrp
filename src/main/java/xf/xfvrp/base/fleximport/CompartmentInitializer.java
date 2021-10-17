package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 */
class CompartmentInitializer {

    /**
     * Checks, that all entities have the same number of compartments
     */
    public static void execute(Node[] nodes, Vehicle[] vehicles) {
        int nbrOfCompartments = getMaxNbrOfCompartments(nodes, vehicles);

        adjustNodes(nodes, nbrOfCompartments);
        adjustVehicles(vehicles, nbrOfCompartments);
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
}
