package xf.xfvrp.base.compartment;

import xf.xfvrp.base.LoadType;

public class MixedCompartmentLoad implements CompartmentLoad {

    private final int compartmentIdx;
    private final boolean isReplenished;

    private float[] loads = null;
    private float[] maxCommonLoads = null;

    public MixedCompartmentLoad(int compartmentIdx, boolean isReplenished) {
        this.compartmentIdx = compartmentIdx;
        this.isReplenished = isReplenished;
    }

    @Override
    public void addAmount(float[] demand, LoadType loadType) {
        init(demand.length);

        if(loadType == LoadType.PICKUP || loadType == LoadType.PRELOAD_AT_DEPOT) {
            loads[compartmentIdx] += demand[compartmentIdx];
            maxCommonLoads[compartmentIdx] = Math.max(
                    loads[compartmentIdx],
                    maxCommonLoads[compartmentIdx]
            );
        }

        if(loadType == LoadType.DELIVERY) {
            loads[compartmentIdx] -= demand[compartmentIdx];
        }
    }

    @Override
    public float checkCapacity(float[] capacities) {
        return Math.max(0, maxCommonLoads[compartmentIdx] - capacities[compartmentIdx]);
    }

    @Override
    public void clear() {
        if(loads != null) {
            loads = new float[loads.length];
            maxCommonLoads = new float[maxCommonLoads.length];
        }
    }

    @Override
    public void replenish() {
        if(isReplenished && loads != null) {
            loads[compartmentIdx] = 0;
            maxCommonLoads = new float[maxCommonLoads.length];
        }
    }

    private void init(int nbrOfCompartments) {
        if(loads == null) {
            loads = new float[nbrOfCompartments];
            maxCommonLoads = new float[nbrOfCompartments];
        }
    }


}
