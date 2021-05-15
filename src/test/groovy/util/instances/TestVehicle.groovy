package util.instances

import xf.xfvrp.base.CompartmentLoadType
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.fleximport.CompartmentCapacity

class TestVehicle {

	int idx = 0;
	String name = "";
	int nbrOfAvailableVehicles = 9999;
	float[] capacity = [10];
	float maxRouteDuration = 999999;
	int maxStopCount = 999999;
	float maxWaitingTime = 999999;
	float fixCost = 0;
	float varCost = 1;
	int vehicleMetricId = 0;
	float maxDrivingTimePerShift = 999999;
	float waitingTimeBetweenShifts = 999999;
	int priority = Vehicle.PRIORITY_UNDEF;
	
	Vehicle getVehicle() {
		List<CompartmentCapacity> compCapa = new ArrayList<>();
		for (float v : capacity)
			compCapa.add(new CompartmentCapacity(v))

		return new Vehicle(
			idx, name,
			nbrOfAvailableVehicles, compCapa,
			maxRouteDuration, maxStopCount, maxWaitingTime,	fixCost, varCost, vehicleMetricId,
			maxDrivingTimePerShift, waitingTimeBetweenShifts, priority
		);
	}
}
