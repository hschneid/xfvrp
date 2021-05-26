package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.exception.XFVRPException;

import java.util.Arrays;

public class InvalidVehicle {

    public static final String invalidVehicleName = "INVALID";

    /**
     * Creates a default vehicle object for invalid routes, which parameters mean no restriction.
     *
     * @return default vehicle for invalid routes
     */
    public static Vehicle createInvalid(int nbrOfCompartments) throws XFVRPException {
        float[] capacities = new float[nbrOfCompartments];
        Arrays.fill(capacities, Float.MAX_VALUE);

        return new VehicleData()
                .setName(invalidVehicleName)
                .setCapacity(capacities)
                .createVehicle(-1);
    }
}
