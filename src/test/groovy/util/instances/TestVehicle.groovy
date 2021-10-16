package util.instances


import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.fleximport.CompartmentCapacity
import xf.xfvrp.base.fleximport.VehicleData

class TestVehicle {

	int idx = 0
	String name = ""
	int nbrOfAvailableVehicles = 9999
	float[] capacity = [10]
	List<CompartmentCapacity> compartmentCapacity
	float maxRouteDuration = 999999
	int maxStopCount = 999999
	float maxWaitingTime = 999999
	float fixCost = 0
	float varCost = 1
	int vehicleMetricId = 0
	float maxDrivingTimePerShift = 999999
	float waitingTimeBetweenShifts = 999999
	int priority = Vehicle.PRIORITY_UNDEF
	
	Vehicle getVehicle() {
		if(compartmentCapacity == null) {
			compartmentCapacity = new ArrayList<>()
			for (float v : capacity)
				compartmentCapacity.add(new CompartmentCapacity(v))
		}

		return new Vehicle(
			idx, name,
			nbrOfAvailableVehicles, VehicleData.transformCapacity(compartmentCapacity),
			maxRouteDuration, maxStopCount, maxWaitingTime,	fixCost, varCost, vehicleMetricId,
			maxDrivingTimePerShift, waitingTimeBetweenShifts, priority
		)
	}
}
