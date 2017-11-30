package xf.xfvrp.opt.init.precheck;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.opt.init.precheck.pdp.PDPPreCheckService;
import xf.xfvrp.opt.init.precheck.vrp.VRPPreCheckService;

public class PreCheckService {

	public Node[] precheck(Node[] nodes, Vehicle vehicle, XFVRPParameter parameter) throws PreCheckException {
		if(parameter.isWithPDP())
			return new PDPPreCheckService().precheck(nodes, vehicle);

		return new VRPPreCheckService().precheck(nodes, vehicle);
	}
}
