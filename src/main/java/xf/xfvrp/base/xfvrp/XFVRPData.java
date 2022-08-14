package xf.xfvrp.base.xfvrp;

import xf.xfvrp.base.compartment.CompartmentType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.base.fleximport.*;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.monitor.StatusCode;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFVRPData extends XFVRPBase {

    /* Importer and data warehouse */
    protected FlexiImporter importer = new FlexiImporter();

    /* Metric for distance and time calculation */
    protected Metric metric;

    /**
     * Akquire a customer data object to insert data for
     * a new customer. The next call of this method will
     * finalize the before akquired customer data object.
     *
     * @return Customer data object
     */
    public CustomerData addCustomer() {
        return importer.getCustomerData();
    }

    /**
     * Akquire a depot data object to insert data for
     * a new depot. The next call of this method will
     * finalize the before akquired depot data object.
     *
     * @return Depot data object
     */
    public DepotData addDepot() {
        return importer.getDepotData();
    }

    /**
     * Akquire a replenish data object to insert data for
     * a new replenishing depot. The next call of this method will
     * finalize the before akquired replenish data object.
     *
     * @return Replenish data object
     */
    public ReplenishData addReplenishment() {
        return importer.getReplenishData();
    }

    /**
     * Akquire a vehicle data object to insert data for
     * a new vehicle. The next call of this method will
     * finalize the before akquired vehicle data object.
     * <p>
     * The call of this method means, that default vehicle
     * parameters are not valid any longer. In this case they
     * have to be inserted in such a vehicle data object.
     *
     * @return Container data object
     */
    public VehicleData addVehicle() {
        return importer.getVehicleData();
    }

    /**
     * Adds a compartment to XFVRP. The number of added compartments must
     * fit to given capacities and demands in customers.
     * <p>
     * If no compartments are given, XFVRP will choose a default compartment.
     */
    public void addCompartment(CompartmentType type) {
        importer.addCompartmentType(type);
    }

    /**
     * Clears the state machine of all read customers
     */
    public void clearCustomers() {
        importer.clearCustomers();
    }

    /**
     * Clears all added depots.
     */
    public void clearDepots() {
        importer.clearDepots();
    }

    /**
     * Removes all inserted vehicles and reset the planning parameters to default.
     * (See setCapacity() and setMaxRouteDuration() )
     */
    public void clearVehicles() {
        importer.clearVehicles();
    }

    /**
     * Sets the maximal allowed capacity of a vehicle
     * <p>
     * This procedure is only available for single vehicle VRPs, which means, that
     * all already inserted vehicles will be removed and replaced
     * by this default value.
     *
     * @param capacity Upper bound of amount, which can be transported on a route.
     */
    public void setCapactiy(float capacity) throws XFVRPException {
        if (capacity <= 0) {
            statusManager.fireMessage(StatusCode.ABORT, "Parameter for capacity must be greater than zero.");
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for capacity must be greater than zero.");
        }

        // Zur Sicherheit erstmal alles entfernen
        importer.clearVehicles();
        // Den Parameter des default-Fahrzeugs anpassen
        importer.defaultVehicle.setCapacity(new float[]{capacity, 0, 0});
    }

    /**
     * Sets the maximal allowed route duration
     * <p>
     * This procedure is only available for single vehicle VRPs, which means, that
     * all already inserted vehicles will be removed and replaced
     * by a default value.
     *
     * @param maxRouteDuration Upper bound of time, which can be traveled on a route.
     */
    public void setMaxRouteDuration(int maxRouteDuration) throws XFVRPException {
        if (maxRouteDuration <= 0) {
            statusManager.fireMessage(StatusCode.ABORT, "Parameter maxRouteDuration must be greater than zero.");
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter maxRouteDuration must be greater than zero.");
        }
        // Zur Sicherheit erstmal alles entfernen
        importer.clearVehicles();
        // Den Parameter des default-Fahrzeugs anpassen
        importer.defaultVehicle.setMaxRouteDuration(maxRouteDuration);
    }

    /**
     * Sets the maximal number of routes
     * <p>
     * This procedure is only available for single vehicle VRPs, which means, that
     * all already inserted vehicles will be removed and replaced
     * by a default value.
     */
    public void setMaxNumberOfRoutes(int maxNumberOfRoutes) throws XFVRPException {
        if (maxNumberOfRoutes <= 0) {
            statusManager.fireMessage(StatusCode.ABORT, "Parameter maxNumberOfRoutes must be greater than zero.");
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter maxNumberOfRoutes must be greater than zero.");
        }
        // Zur Sicherheit erstmal alles entfernen
        importer.clearVehicles();
        // Den Parameter des default-Fahrzeugs anpassen
        importer.defaultVehicle.setCount(maxNumberOfRoutes);
    }

    /**
     * Sets the maximal number of stops
     * <p>
     * This procedure is only available for single vehicle VRPs, which means, that
     * all already inserted vehicles will be removed and replaced
     * by a default value.
     */
    public void setMaxNumberOfStopsPerRoute(int maxNbrOfStops) throws XFVRPException {
        if (maxNbrOfStops <= 1) {
            statusManager.fireMessage(StatusCode.ABORT, "Parameter maxNbrOfStops must be greater than zero.");
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter maxNbrOfStops must be greater than zero.");
        }
        // Zur Sicherheit erstmal alles entfernen
        importer.clearVehicles();
        // Den Parameter des default-Fahrzeugs anpassen
        importer.defaultVehicle.setMaxStopCount(maxNbrOfStops);
    }

    public FlexiImporter getImporter() {
        return importer;
    }

    public Metric getMetric() {
        return metric;
    }

    /**
     * Sets a metric, whereby the optimization can get
     * information about the distance or time between nodes.
     * <p>
     * Metrics can relay on coordinates or on pre-calculated data.
     */
    public void setMetric(Metric metric) {
        this.metric = metric;
    }
}
