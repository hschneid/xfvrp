package xf.xfvrp.base.compartment;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Each truck can load or unload several compartments in parallel. For each
 * compartment a vehicle will have a certain capacity and each customer has
 * for each compartment an amount which can be picked up or delivered.
 *
 * For a certain compartments, there can be different types of handling during
 * capacity checking.
 *
 * If there are only deliveries for a compartment, then checking is different than
 * handling pickups and deliveries simultaniously.
 **/
public enum CompartmentType {

    PICKUP(true),                  // This compartment is only picked up on routes and with replenishment
    DELIVERY(true),                // This compartment is only delivered to customers and with replenishment
    MIXED(true),                   // This compartment is loaded and unloaded during route and with replenishment
    PICKUP_NO_REPLENISH(false),    // This compartment is only picked up on routes and without replenishment
    DELIVERY_NO_REPLENISH(false),  // This compartment is only delivered to customers and without replenishment
    MIXED_NO_REPLENISH(false);     // This compartment is loaded and unloaded during route and without replenishment

    private final boolean isReplenished;

    CompartmentType(boolean isReplenished) {
        this.isReplenished = isReplenished;
    }

    public CompartmentLoad createWithIndex(int idx) {
        switch (this) {
            case PICKUP: return new PickupCompartmentLoad(idx, true);
            case DELIVERY: return new DeliveryCompartmentLoad(idx, true);
            case MIXED: return new MixedCompartmentLoad(idx, true);
            case PICKUP_NO_REPLENISH: return new PickupCompartmentLoad(idx, false);
            case DELIVERY_NO_REPLENISH: return new DeliveryCompartmentLoad(idx, false);
            case MIXED_NO_REPLENISH: return new MixedCompartmentLoad(idx, false);
            // Cannot happen :-/
            default: return null;
        }
    }

    public boolean isReplenished() {
        return isReplenished;
    }
}
