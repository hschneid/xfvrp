package xf.xfvrp.base.compartment;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @author hschneid
 *
 */
public class CompartmentLoadBuilder {

    public CompartmentLoad[] createCompartmentLoads(CompartmentType[] compartmentTypes) {
        CompartmentLoad[] loads = new CompartmentLoad[compartmentTypes.length];
        for (int idx = compartmentTypes.length - 1; idx >= 0; idx--) {
            loads[idx] = compartmentTypes[idx].createWithIndex(idx);
        }

        return loads;
    }
}
