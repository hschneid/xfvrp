package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

public class RandomChangeService extends XFVRPOptBase {

	public Solution change(Solution solution, XFVRPModel model) {
		this.setModel(model);
		
		return this.execute(solution);
	}
	
	/**
	 * This perturb routine relocates single nodes iterativly. The nodes are
	 * selected randomly.
	 * 
	 * @param giantRoute
	 * @param vehicle
	 */
	@Override
	protected Solution execute(Solution solution) {
		int nbrOfVariations = 5;
		int[] param = new int[3];
		for (int i = 0; i < nbrOfVariations; i++) {
			// Search source node
			// Restriction: no depot
			chooseSrc(param, solution);

			// Search destination
			// Restriction: 
			//   Source is not destination
			//   Solution is not invalid
			int cnt = 0;
			boolean changed = false;
			
			while(true) {
				// Choose
				chooseDst(param, solution);

				boolean isValid = checkMove(solution, param);
				if(isValid)
					break;
				
				// Terminate for infinity
				if(cnt > 100)
					break;
				
				cnt++;
			}
			
			if(!changed)
				i--;
		}
		
		return solution;
	}

	private boolean checkMove(Solution solution, int[] param) {
		// Move
		pathMove(solution, param[0], param[0] + param[1], param[2]);
		
		// Eval
		Quality q = check(solution);
		if(q.getPenalty() == 0) {
			return true;
		}

		// Re-Move
		if(param[2] > param[0])
			pathMove(solution, param[2] - param[1] - 1, param[2] - 1, param[0]);
		else
			pathMove(solution, param[2], param[2] + param[1], param[0] + param[1] + 1);
		
		return false;
	}

	/**
	 * 
	 * @param param
	 * @param giantRoute
	 */
	private void chooseSrc(int[] param, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();
		
		// Choose a random source node (customer or replenish)
		int src = -1;
		do {
			src = rand.nextInt(giantRoute.length - 2) + 1;
		} while(giantRoute[src].getSiteType() == SiteType.DEPOT);

		// Reset source node at the beginning of a block
		while(src > 1) {
			Node s = giantRoute[src];
			Node l = giantRoute[src - 1];
			if(s.getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX ||
					l.getPresetBlockIdx() == BlockNameConverter.DEFAULT_BLOCK_IDX ||
					s.getPresetBlockIdx() != l.getPresetBlockIdx() ||
					l.getSiteType() == SiteType.DEPOT)
				break;

			src--;
		}

		param[0] = src;
		param[1] = 0;

		// Extend source node to source path if next nodes belongs to source node block
		if(giantRoute[src].getPresetBlockPos() >= 0) {
			int srcBlockIdx = giantRoute[src].getPresetBlockIdx();
			for (int i = src + 1; i < giantRoute.length; i++) {
				Node n = giantRoute[i];
				if(n.getPresetBlockIdx() == srcBlockIdx && n.getPresetBlockIdx() >= BlockNameConverter.DEFAULT_BLOCK_IDX)
					param[1]++;
				else
					break;
			}
		}
	}

	/**
	 * 
	 * @param param
	 * @param giantRoute
	 */
	private void chooseDst(int[] param, Solution solution) {
		Node[] giantRoute = solution.getGiantRoute();
		
		int dst = -1;
		do {
			dst = rand.nextInt(giantRoute.length - 1) + 1;
		} while(dst >= param[0] && dst <= param[0] + param[1]);
		
		while(dst > 0) {
			Node d = giantRoute[dst];
			Node l = giantRoute[dst - 1];

			if(d.getSiteType() == SiteType.DEPOT ||
					l.getSiteType() == SiteType.DEPOT)
				break;
			
			if(d.getPresetBlockIdx() != l.getPresetBlockIdx())
				break;

			dst--;
		}

		param[2] = dst;
	}

	

	

}
