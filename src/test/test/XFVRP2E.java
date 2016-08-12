package test;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import xf.xfvrp.XFVRP;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.metric.EucledianMetric;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.report.Report;

public class XFVRP2E {

	@Test
	public void testPickupTimeAtDepot() {
		XFVRP v = createXFVRPEuclead();
		
		// White test
		v.addDepot().setExternID("D").setXlong(0).setYlat(0).setTimeWindow(0, 20);
		v.addCustomer().setExternID("C1").setDemand(1).setLoadType(LoadType.DELIVERY).setXlong(0).setYlat(10).setTimeWindow(10, 11);
		v.addCustomer().setExternID("C2").setDemand(1).setLoadType(LoadType.PICKUP).setXlong(0).setYlat(-10).setTimeWindow(10, 11);
		v.addVehicle().setName("V").setCapacity(new float[]{1});
		
		v.addOptType(XFVRPOptType.CONST);
		
		v.executeRoutePlanning();
		Report r;
		r = v.getReport();
		assertTrue(r.getRoutes().size() == 2);
		assertTrue(r.getRoutes().get(0).getVehicle().name.equals("V"));
		assertTrue(r.getRoutes().get(1).getVehicle().name.equals("V"));
		
		// Black test
		v.clearCustomers();
		v.addCustomer().setExternID("C1").setDemand(1).setLoadType(LoadType.DELIVERY).setEarliestPickupAtDepot(2).setXlong(0).setYlat(10).setTimeWindow(10, 11);
		v.addCustomer().setExternID("C2").setDemand(1).setLoadType(LoadType.PICKUP).setEarliestPickupAtDepot(2).setXlong(0).setYlat(-10).setTimeWindow(10, 11);
		
		v.allowsPickupTimeAtDepot();
		v.executeRoutePlanning();
		
		r = v.getReport();
		assertTrue(r.getRoutes().size() == 2);
		assertTrue(r.getRoutes().get(0).getVehicle().name.equals("V"));
		assertTrue(r.getRoutes().get(1).getVehicle().name.contains("INVALID"));
	}
	
	/**
	 * 
	 * @return
	 */
	private XFVRP createXFVRPEuclead() {
		XFVRP v = new XFVRP();
		v.setMetric(new EucledianMetric());

		return v; 
	}
}
