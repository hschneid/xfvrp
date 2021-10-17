package xf.xfvrp.base.compartment;

import xf.xfvrp.base.LoadType;

public interface CompartmentLoad {

    void addAmount(float[] amounts, LoadType loadType);

    int checkCapacity(float[] capacities);

    void clear();

    void replenish();
}
