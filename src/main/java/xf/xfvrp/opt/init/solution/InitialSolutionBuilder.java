package xf.xfvrp.opt.init.solution;

import java.util.ArrayList;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.init.solution.pdp.PDPInitialSolutionBuilder;
import xf.xfvrp.opt.init.solution.vrp.VRPInitialSolutionBuilder;

/**
 * Creates a trivial solution out of the model. 
 * 
 * The solution must be feasible/valid, but no optimization is
 * applied.
 * 
 */
public class InitialSolutionBuilder {

	public Solution build(XFVRPModel model, XFVRPParameter parameter, StatusManager statusManager) {
		if(parameter.isWithPDP())
			return new PDPInitialSolutionBuilder().build(model);

		return new VRPInitialSolutionBuilder().build(model, new ArrayList<Node>(), statusManager);
	}
}
