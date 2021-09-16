package util;

import xf.xfvrp.XFVRP;
import xf.xfvrp.base.exception.XFVRPException;

public class Application {

	public static void main(String[] args) {
		try {
			new XFVRP().executeRoutePlanning();
		} catch (XFVRPException e) {
			e.printStackTrace();
		}
	}
}
