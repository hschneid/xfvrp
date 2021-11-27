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
public interface CompartmentLoad {

    void addAmount(float[] amounts, LoadType loadType);

    float checkCapacity(float[] capacities);

    void clear();

    void replenish();
}
