package xf.xfvrp.base.compartment;

import xf.xfvrp.base.LoadType;

public class MixedCompartmentLoad implements CompartmentLoad {

    private final int compartmentIdx;
    private final boolean isReplenished;

    private float load;
    private float maxCommonLoad;

    public MixedCompartmentLoad(int compartmentIdx, boolean isReplenished) {
        this.compartmentIdx = compartmentIdx;
        this.isReplenished = isReplenished;
    }

    @Override
    public void addAmount(float[] demand, LoadType loadType) {
        if(loadType == LoadType.PICKUP || loadType == LoadType.PRELOAD_AT_DEPOT) {
            load += demand[compartmentIdx];
            maxCommonLoad = Math.max(load, maxCommonLoad);
        }

        if(loadType == LoadType.DELIVERY) {
            load -= demand[compartmentIdx];
        }
    }

    @Override
    public float checkCapacity(float[] capacities) {
        return Math.max(0, maxCommonLoad - capacities[compartmentIdx]);
    }

    @Override
    public void clear() {
        load = 0;
        maxCommonLoad = 0;
    }

    @Override
    public void replenish() {
        if(isReplenished) {
            load = 0;
            maxCommonLoad = 0;
        }
    }
}
