package xf.xfvrp.base.compartment;

import xf.xfvrp.base.LoadType;

public class DeliveryCompartmentLoad implements CompartmentLoad {

    private final int compartmentIdx;
    private final boolean isReplenished;

    private float load;

    public DeliveryCompartmentLoad(int compartmentIdx, boolean isReplenished) {
        this.compartmentIdx = compartmentIdx;
        this.isReplenished = isReplenished;
    }

    @Override
    public void addAmount(float[] demand, LoadType loadType) {
        if(loadType == LoadType.DELIVERY) {
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
