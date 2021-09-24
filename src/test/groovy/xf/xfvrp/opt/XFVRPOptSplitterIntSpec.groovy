package xf.xfvrp.opt

import cern.colt.list.FloatArrayList
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.monitor.DefaultStatusMonitor

import java.util.concurrent.atomic.AtomicInteger

class XFVRPOptSplitterIntSpec extends Specification {

	def "Optimize with activated route splitting"() {
		XFVRP xfvrp = build(new File("./src/test/resources/with_route_plan_splitting.json"))
		xfvrp.allowsRoutePlanSplitting()

		when:
		xfvrp.executeRoutePlanning()

		then:
		1 == 1
	}

	static XFVRP build(File file) {
		XFVRP xfvrp = new XFVRP()
		xfvrp.setStatusMonitor(new DefaultStatusMonitor())

		Map<?, ?> map = new ObjectMapper().readValue(file, Map.class)

		def customers = map.get("Customers")
		def depots = map.get("Depots")
		def vehicles = map.get("Vehicles")

		vehicles.forEach((depot, depotVehicles) -> {
			depotVehicles.forEach(vehicle -> {
				Collection<Double> dblCap = vehicle.get("capacity")
				FloatArrayList fltCap = new FloatArrayList()
				dblCap.forEach(d -> fltCap.add((float)d))
				fltCap.trimToSize()
				xfvrp.addVehicle()
						.setName(vehicle.get("name"))
						.setCapacity(fltCap.elements())
						.setMaxRouteDuration(600)
			})
		})

		depots.forEach(depot -> {
			xfvrp.addDepot()
					.setExternID("DEP")
					.setYlat((float)depot.get("lat"))
					.setXlong((float)depot.get("lng"))
		})

		AtomicInteger counter = new AtomicInteger()
		customers.forEach(customer -> {
			Collection<Double> dblDemand = customer.get("amount")
			Collection<String> vehiclesAllowed = customer.get("vehicles")
			FloatArrayList fltDemand = new FloatArrayList()
			dblDemand.forEach(d -> fltDemand.add((float)d))
			fltDemand.trimToSize()
			xfvrp.addCustomer()
					.setExternID(counter.getAndIncrement()+"")
					.setXlong((float)customer.get("lng"))
					.setYlat((float)customer.get("lat"))
					.setDemand(fltDemand.elements())
					.setServiceTime((float)customer.get("serviceTime"))
					.setLoadType(LoadType.DELIVERY)
		})
		println "Added " + counter + " demands."

		xfvrp.addOptType(XFVRPOptType.SAVINGS)
		xfvrp.addOptType(XFVRPOptType.RELOCATE)
		//xfvrp.addOptType(XFVRPOptType.PATH_RELOCATE)
		//xfvrp.addOptType(XFVRPOptType.PATH_EXCHANGE)

		xfvrp.setMetric(new EucledianMetric())

		return xfvrp
	}
}
