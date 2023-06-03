package script;

import util.XFVRPFileUtil;
import xf.xfvrp.XFVRP;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusMonitor;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.StringWriter;

/** 
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public class VRPFromFileTest {

	public VRPFromFileTest() {

		
		StatusMonitor testStatusManager = new StatusMonitor() {
			@Override
			public void getMessage(StatusCode code, String message) {
				System.out.println(message);
			}
		};
		
		String name = "/Users/hschneid/Downloads/krummen.xfvrp/Krummen_VRP.1.instance.1240.stops.txt.gz";
		String s = XFVRPFileUtil.readCompressedFile(name);
		
		XFVRP x = new XFVRP();
		x.importFromString(s);
		s = null;
		
		x.addOptType(XFVRPOptType.CONST);
		x.addOptType(XFVRPOptType.RELOCATE_PRECHECK);
		x.addOptType(XFVRPOptType.ILS);
		
		x.setStatusMonitor(testStatusManager);
		x.setNbrOfLoopsForILS(200);
		
		long t = System.nanoTime();
		x.executeRoutePlanning();
		System.out.println("T = "+(System.nanoTime() - t)/1000000.0);
		
		Report r = x.getReport();
		System.out.println(StringWriter.write(r));
	}
	
	public static void main(String[] args) {
		new VRPFromFileTest();
	}

}
