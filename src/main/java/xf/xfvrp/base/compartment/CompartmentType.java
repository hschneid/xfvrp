package xf.xfvrp.base.compartment;

/**
 * Copyright (c) 2012-2021 Holger Schneider
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

    PICKUP,     // This compartment is only picked up on routes
    DELIVERY,   // This compartment is only delivered to customers
    MIXED;       // This compartment is loaded and unloaded during route

    public CompartmentLoad createWithIndex(int idx) {
        switch (this) {
            case PICKUP: return new PickupCompartmentLoad(idx);
            case DELIVERY: return new DeliveryCompartmentLoad(idx);
            case MIXED: return new MixedCompartmentLoad(idx);
        }
        // This case cannot happen, but compiler is not smart enough :-/
        return null;
    }
}
