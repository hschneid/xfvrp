package xf.xfvrp.opt.improve.ils;

import java.util.NoSuchElementException;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.XFPDPRelocate;

public class XFPDPRandomChangeService extends XFVRPOptBase implements XFRandomChangeService {

	private static final int MAX_TRIES_CHOOSING = 100;
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
			try {
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
			} catch (NoSuchElementException e) {
				// Means, that one of the choose methods could not find a valid variation parameter.
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

		throw new NoSuchElementException("Structural exception of giant route, where a pickup nopde of a shipment has no delivery node.");
	}

	private void chooseDstPickup(Choice choice, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		int dstPickupIdx = -1;
		do {
			dstPickupIdx = rand.nextInt(giantRoute.length - 1) + 1;
		} while(dstPickupIdx == choice.srcPickupIdx || dstPickupIdx == choice.srcDeliveryIdx);

		choice.dstPickupIdx = dstPickupIdx;
	}

	/**
	 * 
	 * @param choice
	 * @param solution
	 */
	private void chooseDstDelivery(Choice choice, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		int[] routeIdxArr = getIndexOfRoutes(giantRoute);

		int dstDeliveryIdx = -1;
		int counter = 0;
		do {
			dstDeliveryIdx = rand.nextInt(giantRoute.length - 1) + 1;
			counter++;
		} while(isInvalidDstDeliveryIdx(choice, routeIdxArr, dstDeliveryIdx) && counter < MAX_TRIES_CHOOSING);

		if(counter == MAX_TRIES_CHOOSING)
			throw new NoSuchElementException();

		choice.dstDeliveryIdx = dstDeliveryIdx;
	}

	private boolean isInvalidDstDeliveryIdx(Choice choice, int[] routeIdxArr, int dstDeliveryIdx) {
		return (
				// pickup before delivery
				choice.dstPickupIdx > dstDeliveryIdx ||
				// Same route
				routeIdxArr[dstDeliveryIdx] != routeIdxArr[choice.dstPickupIdx] ||
				// Prevent no-op change
				(choice.srcPickupIdx + 2 == choice.dstPickupIdx && choice.srcDeliveryIdx + 1 == dstDeliveryIdx)
				);
	}

	private int[] getIndexOfRoutes(Node[] giantRoute) {
		int[] routeIdxArr = new int[giantRoute.length];
		int id = 0;
		for (int i = 1; i < giantRoute.length; i++) {
			routeIdxArr[i] = id;
			if(giantRoute[i].getSiteType() == SiteType.DEPOT)
				id++;
		}
		
		return routeIdxArr;
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
