package xf.xfvrp.opt.evaluation

import xf.xfvrp.base.Vehicle

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
	int priority = 0;
	
	Vehicle getVehicle() {
		return new Vehicle(
			idx, name,
			nbrOfAvailableVehicles, capacity,
			maxRouteDuration, maxStopCount, maxWaitingTime,	fixCost, varCost, vehicleMetricId,
			maxDrivingTimePerShift, waitingTimeBetweenShifts, priority
		);
	}
}
