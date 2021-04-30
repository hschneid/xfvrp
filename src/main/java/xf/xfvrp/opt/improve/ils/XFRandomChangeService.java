package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;

public interface XFRandomChangeService {

	Solution change(Solution solution, XFVRPModel model) throws XFVRPException;
	
}
