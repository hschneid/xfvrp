package xf.xfvrp.opt.init.check.vrp;

import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.preset.BlockPositionConverter;
import xf.xfvrp.opt.init.solution.vrp.SolutionBuilderDataBag;

public class CheckCustomerService {

	/**
	 * Checks a customers whether it can be served within all constraints.
	 * If one constraint is violated, the customer is invalid for optimization.
	 * 
	 * The checked constraints are:
	 *  - A customer have to be allowed for this vehicle type
	 *  - No customer must have more demand than max loading capacity
	 *  - All customers must be reached from one depot (see Multiple Depots) directly within their time windows.
	 *  - The route duration for direct service from one depot (see Multiple Depots) must be smaller than maximal route duration.
	 * 
	 * If a customer leads to an invalid route plan, then the cause for this is written into the invalid reason at the customer object 
	 * 
	 * @param cust Customer node
	 * @param model Model with all necessary data
	 * @return Can the customer be served within given constraints?
	 */
	public boolean checkCustomer(Node cust, XFVRPModel model, SolutionBuilderDataBag solutionBuilderDataBag) {
		checkPresets(cust, solutionBuilderDataBag);

		// Time Windows or duration
		boolean isValid = checkTimeWindows(cust, model);
		if(!isValid)			
			return false;

		// Capacities
		isValid = checkDemands(cust, model);
		if(!isValid)
			return false;

		return true;
	}

	private void checkPresets(Node cust, SolutionBuilderDataBag solutionBuilderDataBag) {
		if(cust.getPresetBlockRank() < 0)
			throw new IllegalArgumentException("The sequence rank " + cust.getPresetBlockRank() + " in block " + cust.getPresetBlockIdx() + " is lower than zero, which is forbidden.");
		if(cust.getPresetBlockPos() < 0)
			throw new IllegalArgumentException("The sequence position " + cust.getPresetBlockPos() + " in block " + cust.getPresetBlockIdx() + " is lower than zero, which is forbidden.");
		if(cust.getPresetBlockPos() > BlockPositionConverter.UNDEF_POSITION && solutionBuilderDataBag.getKnownSequencePositions().contains(cust.getPresetBlockPos()))
			throw new IllegalArgumentException("The sequence position " + cust.getPresetBlockPos() + " in block " + cust.getPresetBlockIdx() + " is given multiple times, which is forbidden.");
	}

	private boolean checkTimeWindows(Node cust, XFVRPModel model) {
		// Check each depot if this customer can be serviced by this depot
		// with valid constraints
		boolean canBeValid = false;
		for (int i = 0; i < model.getNbrOfDepots(); i++) {
			cust.setInvalidReason(InvalidReason.NONE, "");
			
			Node depot = model.getNodes()[i];

			float travelTime = model.getTime(depot, cust);
			float travelTime2 = model.getTime(cust, depot);		

			// Check route duration with this customer
			float time = travelTime + travelTime2 + cust.getServiceTime();
			if(time > model.getVehicle().maxRouteDuration){
				cust.setInvalidReason(InvalidReason.TRAVEL_TIME, "Customer " + cust.getExternID() + " - Traveltime required: " + time);
				continue;
			}

			// Check time window
			float[] depTW = depot.getTimeWindow(0);
			float arrTime = depTW[0] + travelTime;
			float[] custTW = cust.getTimeWindow(arrTime);
			arrTime = Math.max(arrTime, custTW[0]);
			if(arrTime > custTW[1]) {
				cust.setInvalidReason(InvalidReason.TIME_WINDOW);
				continue;
			}
			if(arrTime + travelTime2 + cust.getServiceTime() > depTW[1]) {
				cust.setInvalidReason(InvalidReason.TIME_WINDOW);
				continue;
			}

			canBeValid = true;
		}
		return canBeValid;
	}

	private boolean checkDemands(Node cust, XFVRPModel model) {
		float[] demands = cust.getDemand();
		float[] capacities = model.getVehicle().capacity;

		int length = Math.min(demands.length, capacities.length);
		for (int i = 0; i < length; i++) {
			if(	demands[i] > capacities[i]) {
				cust.setInvalidReason(
						InvalidReason.CAPACITY,
						"Customer " + cust.getExternID() + " - Capacity " + (i + 1) + " demand: " +capacities[i]+" required: "+demands[i]
						);
				return false;
			} 				
		}
		
		return true;
	}
}
