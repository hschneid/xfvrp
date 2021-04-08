package util;
import xf.xfvrp.XFVRP;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.init.precheck.PreCheckException;

public class Application {

	public static void main(String[] args) {
		try {
			new XFVRP().executeRoutePlanning();
		} catch (PreCheckException | XFVRPException e) {
			e.printStackTrace();
		}
	}
}
