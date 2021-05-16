package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.exception.XFVRPException;

public class InvalidVehicle {

    public static final String invalidVehicleName = "INVALID";

    /**
     * Creates a default vehicle object for invalid routes, which parameters mean no restriction.
     *
     * @return default vehicle for invalid routes
     */
    public static Vehicle createInvalid() throws XFVRPException {
        return new VehicleData().setName(invalidVehicleName).createVehicle(-1);
    }
}
