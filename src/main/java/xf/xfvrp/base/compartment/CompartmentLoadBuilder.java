package xf.xfvrp.base.compartment;

public class CompartmentLoadBuilder {

    public static CompartmentLoad[] createCompartmentLoads(CompartmentType[] compartmentTypes) {
        CompartmentLoad[] loads = new CompartmentLoad[compartmentTypes.length];
        for (int idx = 0; idx < compartmentTypes.length; idx++) {
            loads[idx] = compartmentTypes[idx].createWithIndex(idx);
        }

        return loads;
    }
}
