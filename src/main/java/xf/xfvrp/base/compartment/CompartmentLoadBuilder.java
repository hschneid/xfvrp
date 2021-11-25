package xf.xfvrp.base.compartment;

public class CompartmentLoadBuilder {

    public CompartmentLoad[] createCompartmentLoads(CompartmentType[] compartmentTypes) {
        CompartmentLoad[] loads = new CompartmentLoad[compartmentTypes.length];
        for (int idx = compartmentTypes.length - 1; idx >= 0; idx--) {
            loads[idx] = compartmentTypes[idx].createWithIndex(idx);
        }

        return loads;
    }
}
