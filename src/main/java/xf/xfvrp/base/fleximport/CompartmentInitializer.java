package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.compartment.CompartmentType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;

import java.util.Arrays;

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

    private static void adjustVehicles(Vehicle[] vehicles, int nbrOfCompartments) {
        Arrays.stream(vehicles)
                .filter(vehicle -> (vehicle.getCapacity().length / CompartmentType.NBR_OF_LOAD_TYPES) != nbrOfCompartments)
                .forEach(vehicle -> {
                    int nbrOfCompartmentsBefore = vehicle.getCapacity().length;
                    vehicle.setCapacity(Arrays.copyOf(vehicle.getCapacity(), nbrOfCompartments * CompartmentType.NBR_OF_LOAD_TYPES));
                    Arrays.fill(vehicle.getCapacity(), nbrOfCompartmentsBefore, vehicle.getCapacity().length, Float.MAX_VALUE);
                });
    }

    private static int getMaxNbrOfCompartments(Node[] nodes, Vehicle[] vehicles) {
        int nbrOfCompartments = 0;
        nbrOfCompartments = Math.max(
                nbrOfCompartments,
                Arrays.stream(nodes)
                        .mapToInt(node -> node.getDemand().length)
                        .max()
                        .orElse(-1)
        );
        nbrOfCompartments = Math.max(
                nbrOfCompartments,
                Arrays.stream(vehicles)
                        .mapToInt(vehicle -> vehicle.getCapacity().length / CompartmentType.NBR_OF_LOAD_TYPES)
                        .max()
                        .orElse(-1)
        );

        return nbrOfCompartments;
    }
}
