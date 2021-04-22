package xf.xfvrp.opt.improve.base;

import xf.xfvrp.base.Quality;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;

public interface XFVRPImprovable {

    Quality improve(Solution giantTour, Quality bestResult, XFVRPModel model) throws XFVRPException;
}
