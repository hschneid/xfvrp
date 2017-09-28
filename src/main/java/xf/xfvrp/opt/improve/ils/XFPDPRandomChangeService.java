package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.XFPDPRelocate;

public class XFPDPRandomChangeService extends XFVRPOptBase implements XFRandomChangeService {

	private int NBR_ACCEPTED_INVALIDS = 100;
	private int NBR_OF_VARIATIONS = 5;
	
	private XFPDPRelocate operator = new XFPDPRelocate();

	/*
	 * (non-Javadoc)
	 * @see xf.xfvrp.opt.improve.ils.XFRandomChangeService#change(xf.xfvrp.opt.Solution, xf.xfvrp.base.XFVRPModel)
	 */
	@Override
	public Solution change(Solution solution, XFVRPModel model) {
		this.setModel(model);

		return this.execute(solution);
	}

	/**
	 * This perturb routine relocates single nodes iterativly. The nodes are
	 * selected randomly.
	 * 
	 * @param solution
	 * @return changed solution
	 */
	@Override
	protected Solution execute(Solution solution) {
		Choice choice = new Choice();

		for (int i = 0; i < NBR_OF_VARIATIONS; i++) {
			// Search nodes for source shipment
			// Restriction: no depot
			chooseSrcPickup(choice, solution);
			chooseSrcDelivery(choice, solution);

			// Search destination
			// Restriction: 
			//   Source is not destination
			//   Solution is not invalid
			int cnt = 0;
			while(cnt < NBR_ACCEPTED_INVALIDS) {
				// Choose
				chooseDstPickup(choice, solution);
				chooseDstDelivery(choice, solution);

				boolean isValid = checkMove(choice, solution);
				if(isValid)
					break;

				cnt++;
			}
		}

		return solution;
	}

	private boolean checkMove(Choice choice, Solution solution) {
		operator.change(solution, choice.toArray());

		Quality q = check(solution);
		if(q.getPenalty() == 0) {
			return true;
		}

		operator.reverseChange(solution, choice.toArray());

		return false;
	}

	private void chooseSrcPickup(Choice choice, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		// Choose a random source node (customer or replenish)
		int srcPickupIdx = -1;
		do {
			srcPickupIdx = rand.nextInt(giantRoute.length - 2) + 1;
		} while(giantRoute[srcPickupIdx].getSiteType() == SiteType.DEPOT || giantRoute[srcPickupIdx].getDemand()[0] < 0);

		choice.srcPickupIdx = srcPickupIdx;
	}

	private void chooseSrcDelivery(Choice choice, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		Node srcPickup = giantRoute[choice.srcPickupIdx];
		int shipIdx = srcPickup.getShipmentIdx();

		choice.srcDeliveryIdx = -1;
		for (int i = 0; i < giantRoute.length; i++) {
			if(giantRoute[i].getShipmentIdx() == shipIdx && giantRoute[i].getDemand()[0] < 0) {
				choice.srcDeliveryIdx = i;
				return;
			}
		}

		if(choice.srcDeliveryIdx == -1)
			throw new IllegalStateException("Structural exception of giant route, where a pickup nopde of a shipment has no delivery node.");
	}

	private void chooseDstPickup(Choice choice, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		choice.dstPickupIdx = rand.nextInt(giantRoute.length - 1) + 1;
	}

	/**
	 * 
	 * @param choice
	 * @param solution
	 */
	private void chooseDstDelivery(Choice choice, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		int[] routeIdxArr = new int[giantRoute.length];
		int id = 0;
		for (int i = 1; i < giantRoute.length; i++) {
			routeIdxArr[i] = id;
			if(giantRoute[i].getSiteType() == SiteType.DEPOT)
				id++;
		}

		int dstPickupIdx = routeIdxArr[choice.dstPickupIdx];

		int dstDeliveryIdx = -1;
		do {
			dstDeliveryIdx = rand.nextInt(giantRoute.length - 1) + 1;
		} while(
				// pickup before delivery
				dstPickupIdx > dstDeliveryIdx ||
				// Same route
				routeIdxArr[dstDeliveryIdx] != dstPickupIdx ||
				// Prevent no-op change
				(choice.srcPickupIdx == choice.dstPickupIdx + 1 && choice.srcDeliveryIdx == dstDeliveryIdx)
				);


		choice.dstDeliveryIdx = dstDeliveryIdx;
	}

	private class Choice {
		int srcPickupIdx;
		int srcDeliveryIdx;
		int dstPickupIdx;
		int dstDeliveryIdx;

		public Choice() {
		}
		
		public float[] toArray() {
			return new float[] {srcPickupIdx, srcDeliveryIdx, dstPickupIdx, dstDeliveryIdx};
		}
	}


}
