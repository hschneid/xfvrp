package xf.xfvrp.opt.init.check.pdp;

import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.XFVRPModel;

public class PDPCheckService {
	
	public boolean check(XFVRPModel model, Node[] shipment) {
		Node depot = model.getNodes()[0];
		Node pick = shipment[0];
		Node deli = shipment[1];

		boolean isValid = checkVehicleType(model, pick, deli);
		if(!isValid)
			return false;

		isValid = checkRouteDuration(model, pick, deli, depot);
		if(!isValid)
			return false;
		
		isValid = checkTimeWindows(model, depot, pick, deli);
		if(!isValid)
			return false;

		return true;
	}

	private boolean checkTimeWindows(XFVRPModel model, Node depot, Node pick, Node deli) {
		float travelTime = model.getTime(depot, pick);
		float travelTime2 = model.getTime(pick, deli);
		float travelTime3 = model.getTime(deli, depot);		

		// Check time window

		// Arrival time at pickup
		float[] depTW = depot.getTimeWindow(0);
		float arrTime = depTW[0] + travelTime;
		float[] pickTW = pick.getTimeWindow(arrTime);
		arrTime = Math.max(arrTime, pickTW[0]);
		// Check pickup time window
		if(arrTime > pickTW[1]) {
			pick.setInvalidReason(InvalidReason.TIME_WINDOW);
			deli.setInvalidReason(InvalidReason.TIME_WINDOW);
			return false;
		}

		// Arrival time at delivery
		float arrTime2 = arrTime + pick.getServiceTime() + travelTime2;
		float[] deliTW = deli.getTimeWindow(arrTime2);
		arrTime2 = Math.max(arrTime2, deliTW[0]);
		// Check pickup time window
		if(arrTime2 > deliTW[1]) {
			pick.setInvalidReason(InvalidReason.TIME_WINDOW);
			deli.setInvalidReason(InvalidReason.TIME_WINDOW);
			return false;
		}

		// Check depot time window
		if(arrTime2 + travelTime3 + deli.getServiceTime() > depTW[1]) {
			pick.setInvalidReason(InvalidReason.TIME_WINDOW);
			deli.setInvalidReason(InvalidReason.TIME_WINDOW);
			return false;
		}
		
		return true;
	}
	
	private boolean checkRouteDuration(XFVRPModel model, Node pick, Node deli, Node depot) {
		float travelTime = model.getTime(depot, pick);
		float travelTime2 = model.getTime(pick, deli);
		float travelTime3 = model.getTime(deli, depot);		
		
		// Check route duration with this customer
		float time = travelTime + travelTime2 + travelTime3 + pick.getServiceTime() + deli.getServiceTime();
		if(time > model.getVehicle().maxRouteDuration) {
			pick.setInvalidReason(InvalidReason.TRAVEL_TIME, "Customer " + pick.getExternID() + " - Traveltime required: " + time);
			deli.setInvalidReason(InvalidReason.TRAVEL_TIME, "Customer " + deli.getExternID() + " - Traveltime required: " + time);
			return false;
		}
		
		return true;
	}

	private boolean checkVehicleType(XFVRPModel model, Node pick, Node deli) {
		// Check if customer is allowed for this vehicle type
		if(!pick.getPresetBlockVehicleList().isEmpty() && ! pick.getPresetBlockVehicleList().contains(model.getVehicle().idx)){
			pick.setInvalidReason(InvalidReason.WRONG_VEHICLE_TYPE);
			return false;
		}
		// Check if customer is allowed for this vehicle type
		if(!deli.getPresetBlockVehicleList().isEmpty() && ! deli.getPresetBlockVehicleList().contains(model.getVehicle().idx)){
			deli.setInvalidReason(InvalidReason.WRONG_VEHICLE_TYPE);
			return false;
		}
		
		return true;
	}
}
