package xf.xfvrp.base.compartment;

public class PickupCompartmentLoad implements CompartmentLoad {

    private final int compartmentIdx;

    public PickupCompartmentLoad(int compartmentIdx) {
        this.compartmentIdx = compartmentIdx;
    }

    @Override
    public void addAmount(float[] demand, CompartmentLoad load) {

    }

    @Override
    public int checkCapacity(float[] capacities) {
        return 0;
    }
}
