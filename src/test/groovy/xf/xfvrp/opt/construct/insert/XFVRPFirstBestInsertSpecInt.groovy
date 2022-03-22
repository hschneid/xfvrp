package xf.xfvrp.opt.construct.insert

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptTypes

class XFVRPFirstBestInsertSpecInt extends Specification {

	def "Opt with reinsertion"() {
		def vrp = initScen()

		when:
		def newSol = vrp.executeRoutePlanning()
		def rep = vrp.getReport()

		then:
		rep != null
		rep.routes.size() == 4
	}

	def "bigger FIRST_BEST test"() {
		XFVRP v = new XFVRP()
		v.setMetric(new EucledianMetric())
		v.addDepot().setExternID("D1").setXlong(-1).setYlat(-1)
		v.addDepot().setExternID("D2").setXlong(1).setYlat(1)

		addCustomers(v, 300)
		v.addVehicle().setName("V").setCapacity(new float[]{10,0,0}).setMaxRouteDuration(30)
		v.addOptType(XFVRPOptTypes.FIRST_BEST)

		when:
		long time = System.currentTimeMillis()
		v.executeRoutePlanning()
		time = System.currentTimeMillis() - time
		def r = v.getReport()
		println r.getRoutes().size() +" "+ r.getSummary().cost + " " + time/1000f+"ms"

		then:
		r.getRoutes().size() < 100/3f
	}

	XFVRP initScen() {
		XFVRP v = new XFVRP()
		v.setMetric(new EucledianMetric())
		v.addDepot().setExternID("DEP").setXlong(-1).setYlat(0).setTimeWindow(0,99).setTimeWindow(2,99)
		v.addDepot().setExternID("DEP2").setXlong(1).setYlat(0).setTimeWindow(0,99).setTimeWindow(2,99)

		v.addCustomer().setExternID("1").setXlong(-2).setYlat(2).setDemand([1,1] as float[]).setTimeWindow(0,99)
		v.addCustomer().setExternID("2").setXlong(-2).setYlat(1).setDemand([1,1] as float[]).setTimeWindow(0,99)
		v.addCustomer().setExternID("3").setXlong(-2).setYlat(-1).setDemand([1,1] as float[]).setTimeWindow(0,99)
		v.addCustomer().setExternID("4").setXlong(-2).setYlat(-2).setDemand([1,1] as float[]).setTimeWindow(0,99)
		v.addCustomer().setExternID("5").setXlong(2).setYlat(2).setDemand([1,1] as float[]).setTimeWindow(0,99)

		v.addCustomer().setExternID("6").setXlong(2).setYlat(1).setDemand([1,1] as float[]).setTimeWindow(0,99)
		v.addCustomer().setExternID("7").setXlong(2).setYlat(-1).setDemand([1,1] as float[]).setTimeWindow(0,99)
		v.addCustomer().setExternID("8").setXlong(2).setYlat(-2).setDemand([1,1] as float[]).setTimeWindow(0,99)

		v.addVehicle().setName("V1").setCapacity(new float[]{3, 3})
		v.addOptType(XFVRPOptTypes.FIRST_BEST)

		return v
	}

	private void addCustomers(XFVRP v, int nbrOfCustomers) {
		Random rand = new Random(1234)

		List<float[]> pointList = new ArrayList<>()
		for (int i = 0; i < 360; i++)
			pointList.add(new float[]{-5 + (10 * rand.nextFloat()), -5 + (10 * rand.nextFloat())})

		int nodeIdx = 0
		for (int i = 0; i < nbrOfCustomers; i++) {
			float[] p1 = pointList.get(rand.nextInt(pointList.size()))
			float[] p2 = null
			do {
				p2 = pointList.get(rand.nextInt(pointList.size()))
			} while(p1 == p2)

			v.addCustomer()
					.setExternID("N"+(nodeIdx++))
					.setXlong(p1[0])
					.setYlat(p1[1])
					.setDemand(new float[]{1,0,0})
					.setLoadType(LoadType.PICKUP)
		}
	}
}
