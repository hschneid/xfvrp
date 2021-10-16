package xf.xfvrp.base.compartment;

public class DeliveryCompartmentLoad implements CompartmentLoad {

    private final int compartmentIdx;

    public DeliveryCompartmentLoad(int compartmentIdx) {
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
