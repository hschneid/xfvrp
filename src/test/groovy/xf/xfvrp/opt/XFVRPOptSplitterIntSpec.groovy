package xf.xfvrp.opt


import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.monitor.DefaultStatusMonitor

class XFVRPOptSplitterIntSpec extends Specification {

	def rand = new Random(1234)

	def "Optimize with activated route splitting"() {
		XFVRP xfvrp = build()
		xfvrp.parameters.isRouteSplittingAllowed()

		when:
		xfvrp.executeRoutePlanning()

		then:
		// Number is customers fits
		xfvrp.getReport().getRoutes().sum {route -> route.events.findAll {e -> e.siteType == SiteType.CUSTOMER}.size()} == 550
		// Number is depots fits
		xfvrp.getReport().getRoutes().stream().flatMap({route -> route.getEvents().stream()}).filter({e -> e.getSiteType() == SiteType.DEPOT}).map(n -> n.getID()).distinct().count() == 1
	}

	private XFVRP build() {
		XFVRP xfvrp = new XFVRP()
		xfvrp.setStatusMonitor(new DefaultStatusMonitor())

		xfvrp.addVehicle().setCapacity(100).setName("V1")
		xfvrp.addDepot().setXlong(10).setYlat(10).setExternID("D")
		for (i in 0..<550) {
			xfvrp.addCustomer()
					.setExternID("C1"+i)
					.setXlong(rand.nextFloat() * 20f as float)
					.setYlat(rand.nextFloat() * 20f as float)
					.setDemand(rand.nextInt(10) + 1)
					.setServiceTime(5)
		}

		xfvrp.addOptType(XFVRPOptType.SAVINGS)
		xfvrp.addOptType(XFVRPOptType.RELOCATE)
		xfvrp.addOptType(XFVRPOptType.SWAP)
		//xfvrp.addOptType(XFVRPOptType.PATH_RELOCATE)
		//xfvrp.addOptType(XFVRPOptType.PATH_EXCHANGE)

		xfvrp.setMetric(new EucledianMetric())

		return xfvrp
	}
}
