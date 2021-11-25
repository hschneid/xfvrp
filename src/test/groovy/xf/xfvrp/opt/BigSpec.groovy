package xf.xfvrp.opt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Ignore
import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.base.monitor.DefaultStatusMonitor
import xf.xfvrp.report.RouteReport

import java.nio.file.Files
import java.nio.file.Path

class BigSpec extends Specification {

	@Ignore
	def "Do a big test with lots of constraints and data"() {
		def vrp = read()

		when:
		vrp.executeRoutePlanning()

		def rep = vrp.getReport()
		def routes = rep.routes
		// println StringWriter.write(rep)
		println rep.summary.getDistance()
		then:
		rep != null
		for (RouteReport rr  : routes) {
			if(rr.vehicle.name != 'INVALID')
				println rr.events.size() - 2
		}
	}

	@Ignore
	def "Savings speed test"() {
		when:
		def nano = System.nanoTime()
		for (i in 0..<10) {
			def vrp = build2()
			vrp.executeRoutePlanning()
		}
		def diff = ((System.nanoTime() - nano) / 1_000_000_000.0) / 10.0
		println 'XXX ' + diff

		then:
		diff > 0
	}

	private XFVRP read() {
		def bytes = Files.readAllBytes(Path.of("./src/test/resources/IML-big.json"))
		def root = new ObjectMapper().readTree(bytes)

		XFVRP vrp = new XFVRP()
		readDepot(root, vrp)
		readVehicles(root, vrp)
		readCompartments(vrp)
		readCustomers(root, vrp)

		vrp.setMetric(Metrics.EUCLEDIAN.get())
		vrp.addOptType(XFVRPOptTypes.SAVINGS)
		vrp.addOptType(XFVRPOptTypes.RELOCATE)
		vrp.addOptType(XFVRPOptTypes.PATH_RELOCATE)
		vrp.setStatusMonitor(new DefaultStatusMonitor())
		vrp.getParameters().setRouteSplitting(true)
		//vrp.getParameters().setMixedFleetHeuristic(new InTurnMixedFleetHeuristic())

		return vrp
	}

	void readDepot(JsonNode root, XFVRP vrp) {
		vrp.addDepot()
				.setExternID("DEP")
				.setXlong(root.get("depot").get("coordinates").get("longitude").asDouble().floatValue())
				.setYlat(root.get("depot").get("coordinates").get("latitude").asDouble().floatValue())
	}

	void readVehicles(JsonNode root, XFVRP vrp) {
		for (JsonNode v : root.get("vehicles").toList()) {
			vrp.addVehicle()
					.setName(v.get("name").toString())
					.setCount(v.get("count").asInt() * 5)
					.setCapacity([
							v.get("capacity").get("fresh_water").asDouble().floatValue(),
							v.get("capacity").get("grey_water").asDouble().floatValue(),
							v.get("capacity").get("cabin_units").asDouble().floatValue()
					] as float[])
					.setMaxRouteDuration(root.get("max_duration").asDouble().floatValue())
		}
	}

	void readCustomers(JsonNode root, XFVRP vrp) {
		int i = 0
		for (JsonNode o : root.get("orders").toList()) {
			vrp.addCustomer()
					.setExternID(o.get("order_id_internal").toString())
					.setXlong(o.get("coordinates").get("longitude").asDouble().floatValue())
					.setYlat(o.get("coordinates").get("latitude").asDouble().floatValue())
					.setDemand([
							o.get("amount").get("fresh_water").asDouble().floatValue(),
							o.get("amount").get("grey_water").asDouble().floatValue(),
							o.get("amount").get("cabin_units").asDouble().floatValue()
					] as float[])
					.setServiceTime(o.get("stop_time").asDouble().floatValue())
					.setLoadType(
							o.get("service").toString().contains("PICKUP") ? LoadType.PICKUP : LoadType.DELIVERY
					)
		}
	}

	void readCompartments(XFVRP vrp) {
		vrp.addCompartment(CompartmentType.DELIVERY)
		vrp.addCompartment(CompartmentType.PICKUP)
		vrp.addCompartment(CompartmentType.MIXED)
	}

	XFVRP build2() {
		def rand = new Random(1234)

		def vrp = new XFVRP()
		vrp.addDepot().setExternID("DEP1").setXlong(50).setYlat(50)
		// vrp.addDepot().setExternID("DEP1").setXlong(50).setYlat(33)
		// vrp.addDepot().setExternID("DEP2").setXlong(33).setYlat(66)
		// vrp.addDepot().setExternID("DEP3").setXlong(66).setYlat(66)

		for (i in 0..<600) {
			vrp.addCustomer()
					.setExternID("C"+i)
					.setXlong(rand.nextInt(100))
					.setYlat(rand.nextInt(100))
					.setDemand([rand.nextInt(100)] as float[])
					.setServiceTime(rand.nextInt(5))
		}

		vrp.addVehicle()
				.setName("VEH")
				.setCapacity([1000] as float[])
				.setMaxRouteDuration(400)

		vrp.setMetric(Metrics.EUCLEDIAN.get())
		// vrp.addOptType(XFVRPOptTypes.SAVINGS)
		// vrp.addOptType(XFVRPOptTypes.RELOCATE)
		vrp.addOptType(XFVRPOptTypes.PATH_RELOCATE)
		vrp.setStatusMonitor(new DefaultStatusMonitor())

		return vrp
	}
}
