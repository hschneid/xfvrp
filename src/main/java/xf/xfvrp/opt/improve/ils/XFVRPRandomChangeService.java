package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.BlockPositionConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveUtil;
import xf.xfvrp.opt.improve.routebased.move.XFVRPSegmentMove;

public class XFVRPRandomChangeService extends XFVRPOptBase implements XFRandomChangeService {

	private int NBR_ACCEPTED_INVALIDS = 100;
	private int NBR_OF_VARIATIONS = 5;

	private final XFVRPSegmentMove operator = new XFVRPSegmentMove();

	/*
	 * (non-Javadoc)
	 * @see xf.xfvrp.opt.improve.ils.XFRandomChangeService#change(xf.xfvrp.opt.Solution, xf.xfvrp.base.XFVRPModel)
	 */
	@Override
	public Solution change(Solution solution, XFVRPModel model) throws XFVRPException {
		this.setModel(model);

		return this.execute(solution);
	}

	/**
	 * This perturb routine relocates single nodes iterativly. The nodes are
	 * selected randomly.
	 */
	@Override
	protected Solution execute(Solution solution) throws XFVRPException {
		Choice choice = new Choice();

		for (int i = 0; i < NBR_OF_VARIATIONS; i++) {
			// Search source node
			// Restriction: no depot
			chooseSrc(choice, solution);

			// Search destination
			// Restriction: 
			//   Source is not destination
			//   Solution is not invalid
			int cnt = 0;
			while(cnt < NBR_ACCEPTED_INVALIDS) {
				// Choose
				chooseDst(choice, solution);

				boolean isValid = checkMove(choice, solution);
				if(isValid) {
					break;
				}

				cnt++;
			}
		}

		return solution;
	}

	private boolean checkMove(Choice choice, Solution solution) throws XFVRPException {
		XFVRPMoveUtil.change(solution, choice.toArray());

		Quality q = check(solution);
		if(q.getPenalty() == 0) {
			return true;
		}

		XFVRPMoveUtil.reverseChange(solution, choice.toArray());
		return false;
	}

	private void chooseSrc(Choice choice, Solution solution) {
		Node[][] routes = solution.getRoutes();

		// Choose not empty route
		int srcRouteIdx;
		do {
			srcRouteIdx = rand.nextInt(routes.length);
		} while (routes[srcRouteIdx].length <= 2);

		// Choose a source node (customer or replenish)
		int srcPos;
		do {
			srcPos = rand.nextInt(routes[srcRouteIdx].length - 2) + 1;
		} while (routes[srcRouteIdx][srcPos].getSiteType() == SiteType.DEPOT);

		// Reset source node at the beginning of a block
		srcPos = adjustForBlockCriteria(routes[srcRouteIdx], srcPos, 1);

		choice.srcRouteIdx = srcRouteIdx;
		choice.srcPos = srcPos;
		choice.segmentLength = 0;

		// Extend source node to source path if next nodes belongs to source node block
		expandToBlock(choice, routes[srcRouteIdx]);
	}

	private void expandToBlock(Choice choice, Node[] route) {
		int src = choice.srcPos;
		int srcBlockIdx = route[src].getPresetBlockIdx();
		int srcPosValue = route[src].getPresetBlockPos();

		if(srcBlockIdx > BlockNameConverter.UNDEF_BLOCK_IDX && srcPosValue > BlockPositionConverter.UNDEF_POSITION) {

			for (int i = src + 1; i < route.length; i++) {
				Node n = route[i];
				if(n.getPresetBlockIdx() == srcBlockIdx && n.getPresetBlockPos() > srcPosValue) {
					choice.segmentLength++;
					srcPosValue = n.getPresetBlockPos();
				} else
					break;
			}
		}
	}

	private void chooseDst(Choice choice, Solution solution) {
		Node[][] routes = solution.getRoutes();

		int dstRouteIdx = rand.nextInt(routes.length);

		int dstPos;
		do {
			dstPos = rand.nextInt(routes[dstRouteIdx].length - 1) + 1;
		} while (
				choice.srcRouteIdx == dstRouteIdx &&
						dstPos >= choice.srcPos &&
						dstPos <= choice.srcPos + choice.segmentLength
		);

		choice.dstRouteIdx = dstRouteIdx;
		choice.dstPos = adjustForBlockCriteria(routes[dstRouteIdx], dstPos, 1);
	}

	private int adjustForBlockCriteria(Node[] route, int pos, int untilPos) {
		if(route[pos].getPresetBlockIdx() > BlockNameConverter.DEFAULT_BLOCK_IDX) {
			while(pos > untilPos) {
				Node thisNode = route[pos];
				Node beforeNode = route[pos - 1];

				if(thisNode.getSiteType() == SiteType.DEPOT || beforeNode.getSiteType() == SiteType.DEPOT)
					break;

				if(thisNode.getPresetBlockIdx() != beforeNode.getPresetBlockIdx())
					break;

				pos--;
			}
		}
		return pos;
	}

	private class Choice {
		int srcRouteIdx;
		int dstRouteIdx;
		int srcPos;
		int segmentLength;
		int dstPos;

		public Choice() {
		}

		public float[] toArray() {
			return new float[] {-1, srcRouteIdx, dstRouteIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.NO_INVERT};
		}
	}
}
