package xf.xfvrp.opt


import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.base.monitor.DefaultStatusMonitor

import java.util.concurrent.atomic.AtomicInteger

class FullRouteMixedFleetIntSpec extends Specification {

	def "test default mixed fleet heuristic with 3 vehicles"() {
		XFVRP xfvrp = buildXFVRP()

		xfvrp.addCustomer().setExternID('n1').setDemand(1)
		xfvrp.addCustomer().setExternID('n2').setDemand(1)
		xfvrp.addCustomer().setExternID('n3').setDemand(1)
		xfvrp.addCustomer().setExternID('n4').setDemand(1)
		xfvrp.addCustomer().setExternID('n5').setDemand(1)
		xfvrp.addCustomer().setExternID('n6').setDemand(1)

		when:
		xfvrp.executeRoutePlanning()
		def rep = xfvrp.getReport()
		then:
		rep.routes.size() == 2
		rep.routes.count {r -> r.vehicle.name == 'V1' && r.events.size() == 6} == 1
		rep.routes.count {r -> r.vehicle.name == 'V2' && r.events.size() == 4} == 1
	}

	def "blocked customer to certain vehicle in mixed fleet"() {
		XFVRP xfvrp = buildXFVRP()

		xfvrp.addCustomer().setExternID('n1').setDemand(1)
		xfvrp.addCustomer().setExternID('n2').setDemand(1)
		xfvrp.addCustomer().setExternID('n3').setDemand(1)
		xfvrp.addCustomer().setExternID('n4').setDemand(1)
		xfvrp.addCustomer().setExternID('n5').setDemand(1)
		xfvrp.addCustomer().setExternID('n6').setDemand(1).setPresetBlockVehicleList(['V3'] as Set<String>)

		when:
		xfvrp.executeRoutePlanning()
		def rep = xfvrp.getReport()
		then:
		rep.routes.size() == 3
		rep.routes.count {r -> r.vehicle.name == 'V1' && r.events.size() == 6} == 1
		rep.routes.count {r -> r.vehicle.name == 'V2' && r.events.size() == 3} == 1
		rep.routes.count {r -> r.vehicle.name == 'V3' && r.events.size() == 3} == 1
	}

	def "test vehicle restrictions - 1 vehicle has zero allowed customers "() {
		def xfvrp = build(new File("./src/test/resources/with_vehicle_restrictions.json"))

		when:
		xfvrp.executeRoutePlanning()

		then:
		def ex = thrown(XFVRPException)
		ex.message == 'Not a single node is allowed for vehicle VEHICLE_1. Please remove it from input.'
	}

	XFVRP buildXFVRP() {
		XFVRP xfvrp = new XFVRP()
		xfvrp.addVehicle().setName('V1').setCapacity([4] as float[]).setFixCost(11).setVarCost(4).setCount(1)
		xfvrp.addVehicle().setName('V2').setCapacity([3] as float[]).setFixCost(11).setVarCost(5).setCount(1)
		xfvrp.addVehicle().setName('V3').setCapacity([2] as float[]).setFixCost(14).setVarCost(6).setCount(1)
		xfvrp.setMetric(Metrics.EUCLEDIAN.get())
		xfvrp.addOptType(XFVRPOptTypes.RELOCATE)
		xfvrp.addDepot().setExternID('nD')
		return xfvrp
	}

	private XFVRP build(File file) {
		XFVRP xfvrp = new XFVRP()
		xfvrp.setStatusMonitor(new DefaultStatusMonitor())

		Map<?, ?> map = new ObjectMapper().readValue(file, Map.class)

		def customers = map.get("Customers")
		def depots = map.get("Depots")
		def vehicles = map.get("Vehicles")

		vehicles.forEach((depot, depotVehicles) -> {
			depotVehicles.forEach(vehicle -> {

				def caps = new float[vehicle.get("capacity").size()]
				int i = 0
				for (double d : vehicle.get("capacity")) {
					caps[i++] = d
				}
				caps = Arrays.copyOf(caps, i)
				xfvrp.getData().addVehicle()
						.setName(vehicle.get("name"))
						.setCapacity(caps)
						.setMaxRouteDuration(600)
			})
		})

		depots.forEach(depot -> {
			xfvrp.getData().addDepot()
					.setExternID("DEP")
					.setYlat((float)depot.get("lat"))
					.setXlong((float)depot.get("lng"))
		})

		AtomicInteger counter = new AtomicInteger()
		customers.forEach(customer -> {
			Collection<String> vehiclesAllowed = customer.get("vehicles")
			def caps = new float[customer.get("amount").size()]
			int i = 0
			for (double d : customer.get("amount")) {
				caps[i++] = d
			}
			caps = Arrays.copyOf(caps, i)

			var cust = xfvrp.getData().addCustomer()
					.setExternID(counter.getAndIncrement()+"")
					.setXlong((float)customer.get("lng"))
					.setYlat((float)customer.get("lat"))
					.setDemand(caps)
					.setServiceTime((float)customer.get("serviceTime"))
					.setLoadType(LoadType.DELIVERY)
			if (vehiclesAllowed != null && vehiclesAllowed.size() > 0)
				cust.setPresetBlockVehicleList(new HashSet<String>(vehiclesAllowed))
		})
		println "Added " + counter + " demands."

		xfvrp.addOptType(XFVRPOptTypes.SAVINGS)
		xfvrp.addOptType(XFVRPOptTypes.RELOCATE)
		//xfvrp.addOptType(XFVRPOptType.PATH_RELOCATE)
		//xfvrp.addOptType(XFVRPOptType.PATH_EXCHANGE)

		xfvrp.getData().setMetric(new EucledianMetric())

		return xfvrp
	}
}
