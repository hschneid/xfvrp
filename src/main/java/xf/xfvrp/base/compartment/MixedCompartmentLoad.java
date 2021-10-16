package xf.xfvrp.base.compartment;

public class MixedCompartmentLoad implements CompartmentLoad {

    private final int compartmentIdx;

    public MixedCompartmentLoad(int compartmentIdx) {
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
