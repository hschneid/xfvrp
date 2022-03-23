package xf.xfvrp.opt.construct.insert

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptTypes

class XFPDPFirstBestInsertSpecInt extends Specification {

	def rand = new Random(1234)

	def "bigger FIRST_BEST test"() {
		XFVRP v = new XFVRP()
		v.setMetric(new EucledianMetric())
		v.addDepot().setExternID("D1").setXlong(-1).setYlat(-1)
		v.addDepot().setExternID("D2").setXlong(1).setYlat(1)

		addShipments(v, 100)
		v.addVehicle().setName("V").setCapacity(new float[]{10,0,0}).setMaxRouteDuration(30)
		v.addOptType(XFVRPOptTypes.PDP_CHEAPEST_INSERT)
		v.getParameters().setNbrOfILSLoops(10)
		v.getParameters().setWithPDP(true)

		when:
		long time = System.currentTimeMillis()
		v.executeRoutePlanning()
		time = System.currentTimeMillis() - time
		def r = v.getReport()
		println r.getRoutes().size() +" "+ r.getSummary().cost + " " + time/1000f+"ms"

		then:
		r.getRoutes().size() < 100/3f
	}

	private void addShipments(XFVRP v, int nbrOfShipments) {
		List<float[]> pointList = new ArrayList<>()
		for (int i = 0; i < 360; i++)
			pointList.add(new float[]{-5 + (10 * rand.nextFloat()), -5 + (10 * rand.nextFloat())})

		for (int i = 0; i < nbrOfShipments; i++) {
			float[] p1 = getPoint(pointList)
			float[] p2 = getPoint(pointList)

			if(p1[0] == p2[0] && p1[1] == p2[2]) {
				i--
				continue
			}

			v.addCustomer()
					.setExternID("S"+i+"P")
					.setXlong(p1[0])
					.setYlat(p1[1])
					.setDemand(new float[]{1,0,0})
					.setShipID("S"+i)
					.setLoadType(LoadType.PICKUP)

			v.addCustomer()
					.setExternID("S"+i+"D")
					.setXlong(p2[0])
					.setYlat(p2[1])
					.setDemand(new float[]{-1,0,0})
					.setShipID("S"+i)
					.setLoadType(LoadType.DELIVERY)
		}
	}

	float[] getPoint(List<float[]> pointList) {
		return pointList.get(rand.nextInt(pointList.size()))
	}
}
