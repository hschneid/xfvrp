package xf.xfvrp.report

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.fleximport.InvalidVehicle
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptType

class ReportBuilderExtendedCompartmentCapacitySpec extends Specification {

	def "Number of compartments between 1 vehicle and demands is not equal and also an invalid node"() {
		def xfvrp = new XFVRP()

		initScen(xfvrp)

		when:
		xfvrp.executeRoutePlanning()
		def rep = xfvrp.getReport()

		then:
		rep.getSummary().getNbrOfUsedVehicles() == 3
		// 1 route with invalid vehicle and invalid customer
		rep.routes.stream()
				.filter({f -> (f.vehicle.name == InvalidVehicle.invalidVehicleName) })
				.flatMap({f -> f.getEvents().stream()})
				.filter({f -> f.ID == "4"})
				.count() == 1
	}

	static void initScen(XFVRP xfvrp) {
		xfvrp.addVehicle()
				.setName("V1")
				.setCapacity([4,4,5] as float[])

		xfvrp.addDepot()
				.setExternID("DEP")

		xfvrp.addCustomer()
				.setExternID("1")
				.setXlong(1)
				.setYlat(1)
				.setDemand([1, 1, 1, 1] as float[])
				.setLoadType(LoadType.DELIVERY)

		xfvrp.addCustomer()
				.setExternID("2")
				.setXlong(0)
				.setYlat(1)
				.setDemand([2, 2] as float[])
				.setLoadType(LoadType.DELIVERY)

		xfvrp.addCustomer()
				.setExternID("3")
				.setXlong(1)
				.setYlat(0)
				.setDemand([3] as float[])
				.setLoadType(LoadType.DELIVERY)

		xfvrp.addCustomer()
				.setExternID("4")
				.setXlong(0)
				.setYlat(-1)
				.setDemand([0,0,6] as float[])
				.setLoadType(LoadType.DELIVERY)

		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptType.RELOCATE)
	}

}
