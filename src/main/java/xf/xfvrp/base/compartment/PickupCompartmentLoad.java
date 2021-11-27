package xf.xfvrp.base.compartment;

import xf.xfvrp.base.LoadType;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @author hschneid
 *
 */
public class PickupCompartmentLoad implements CompartmentLoad {

    private final int compartmentIdx;
    private final boolean isReplenished;

    private float load;

    public PickupCompartmentLoad(int compartmentIdx, boolean isReplenished) {
        this.compartmentIdx = compartmentIdx;
        this.isReplenished = isReplenished;
    }

    @Override
    public void addAmount(float[] demand, LoadType loadType) {
        if(loadType == LoadType.PICKUP) {
            load += demand[compartmentIdx];
        }
    }

    @Override
    public float checkCapacity(float[] capacities) {
        return Math.max(0, load - capacities[compartmentIdx]);
    }

    @Override
    public void clear() {
        load = 0;
    }

    @Override
    public void replenish() {
        if(isReplenished) {
            load = 0;
        }
    }
}
