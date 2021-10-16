package xf.xfvrp.base.compartment;

public interface CompartmentLoad {

    void addAmount(float[] demand, CompartmentLoad load);

    int checkCapacity(float[] capacities);
}
