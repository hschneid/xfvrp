package xf.xfvrp.base.compartment;

import xf.xfvrp.base.LoadType;

public class MixedCompartmentLoad implements CompartmentLoad {

    private final int compartmentIdx;

    public MixedCompartmentLoad(int compartmentIdx) {
        this.compartmentIdx = compartmentIdx;
    }

    @Override
    public void addAmount(float[] demand, LoadType loadType) {

    }

    @Override
    public int checkCapacity(float[] capacities) {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public void replenish() {

    }


}
