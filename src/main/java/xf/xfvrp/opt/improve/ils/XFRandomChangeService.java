package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;

public interface XFRandomChangeService {

	public Solution change(Solution solution, XFVRPModel model);
	
}
