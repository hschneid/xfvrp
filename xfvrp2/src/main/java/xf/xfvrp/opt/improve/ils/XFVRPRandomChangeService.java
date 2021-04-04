package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.BlockPositionConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.XFVRPPathMove;

public class XFVRPRandomChangeService extends XFVRPOptBase implements XFRandomChangeService {

	private int NBR_ACCEPTED_INVALIDS = 100;
	private int NBR_OF_VARIATIONS = 5;
	
	private XFVRPPathMove operator = new XFVRPPathMove();

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
	 */
	@Override
	protected Solution execute(Solution solution) {
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

	private boolean checkMove(Choice choice, Solution solution) {
		operator.change(solution, choice.toArray());

		Quality q = check(solution);
		if(q.getPenalty() == 0) {
			return true;
		}

		operator.reverseChange(solution, choice.toArray());
		return false;
	}

	private void chooseSrc(Choice choice, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		// Choose a random source node (customer or replenish)
		int src = -1;
		do {
			src = rand.nextInt(giantRoute.length - 2) + 1;
		} while(giantRoute[src].getSiteType() == SiteType.DEPOT);

		// Reset source node at the beginning of a block
		src = adjustForBlockCriteria(giantRoute, src, 1);

		choice.srcIdx = src;
		choice.srcPathLength = 0;

		// Extend source node to source path if next nodes belongs to source node block
		expandToBlock(choice, giantRoute);
	}

	private void expandToBlock(Choice choice, Node[] giantRoute) {
		int src = choice.srcIdx;
		int srcBlockIdx = giantRoute[src].getPresetBlockIdx();
		int srcPosValue = giantRoute[src].getPresetBlockPos();

		if(srcBlockIdx > BlockNameConverter.UNDEF_BLOCK_IDX && srcPosValue > BlockPositionConverter.UNDEF_POSITION) {

			for (int i = src + 1; i < giantRoute.length; i++) {
				Node n = giantRoute[i];
				if(n.getPresetBlockIdx() == srcBlockIdx && n.getPresetBlockPos() > srcPosValue) {
					choice.srcPathLength++;
					srcPosValue = n.getPresetBlockPos();
				} else
					break;
			}
		}
	}

	private void chooseDst(Choice choice, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();

		int dst = -1;
		do {
			dst = rand.nextInt(giantRoute.length - 1) + 1;
		} while(dst >= choice.srcIdx && dst <= choice.srcIdx + choice.srcPathLength);

		choice.dstIdx = adjustForBlockCriteria(giantRoute, dst, 0);
	}

	private int adjustForBlockCriteria(Node[] giantRoute, int pos, int untilPos) {
		if(giantRoute[pos].getPresetBlockIdx() > BlockNameConverter.DEFAULT_BLOCK_IDX) {
			while(pos > untilPos) {
				Node thisNode = giantRoute[pos];
				Node beforeNode = giantRoute[pos - 1];

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
		int srcIdx;
		int srcPathLength;
		int dstIdx;

		public Choice() {
		}
		
		public float[] toArray() {
			return new float[] {srcIdx, dstIdx, srcPathLength, XFVRPPathMove.NO_INVERT};
		}
	}
}
