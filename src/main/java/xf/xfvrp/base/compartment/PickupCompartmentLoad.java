package xf.xfvrp.base.compartment;

import xf.xfvrp.base.LoadType;

public class PickupCompartmentLoad implements CompartmentLoad {

    private final int compartmentIdx;
    private final boolean isReplenished;

    private float[] loads = null;

    public PickupCompartmentLoad(int compartmentIdx, boolean isReplenished) {
        this.compartmentIdx = compartmentIdx;
        this.isReplenished = isReplenished;
    }

    @Override
    public void addAmount(float[] demand, LoadType loadType) {
        init(demand.length);

        if(loadType == LoadType.PICKUP) {
            loads[compartmentIdx] += demand[compartmentIdx];
        }
    }

    @Override
    public float checkCapacity(float[] capacities) {
        return Math.max(0, loads[compartmentIdx] - capacities[compartmentIdx]);
    }

    @Override
    public void clear() {
        if(loads != null)
            loads = new float[loads.length];
    }

    @Override
    public void replenish() {
        if(isReplenished && loads != null) {
            loads[compartmentIdx] = 0;
        }
    }

    private void init(int nbrOfCompartments) {
        if(loads == null) loads = new float[nbrOfCompartments];
    }
}
