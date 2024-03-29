package xf.xfvrp.opt.evaluation


import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.opt.XFVRPOptTypes

class EvaluationServiceMaxRoutesDepotSpec extends Specification {

	def "Consider max routes for 1 depot"() {
		XFVRP vrp = build()
		vrp.addVehicle().setName('V').setCapacity([3] as float[])

		vrp.addDepot().setExternID('nD').setMaxNbrRoutes(2)
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand(1)
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand(1)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand(1)
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand(1)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand(1)
		vrp.addCustomer().setExternID('n6').setXlong(-105).setYlat(-105).setDemand(1)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 2
		result.routes[0].vehicle.name == 'V'
		result.routes[1].vehicle.name == 'V'
	}

	def "Work with overhang when too many routes for 1 depot"() {
		XFVRP vrp = build()
		vrp.addVehicle().setName('V').setCapacity([3] as float[])

		vrp.addDepot().setExternID('nD').setMaxNbrRoutes(2)
		vrp.addCustomer().setExternID('n11').setXlong(100).setYlat(100).setDemand(1)
		vrp.addCustomer().setExternID('n12').setXlong(101).setYlat(100).setDemand(1)
		vrp.addCustomer().setExternID('n13').setXlong(101).setYlat(101).setDemand(1)
		vrp.addCustomer().setExternID('n21').setXlong(-102).setYlat(102).setDemand(1)
		vrp.addCustomer().setExternID('n22').setXlong(-102).setYlat(103).setDemand(1)
		vrp.addCustomer().setExternID('n23').setXlong(-103).setYlat(103).setDemand(1)
		vrp.addCustomer().setExternID('n31').setXlong(14).setYlat(14).setDemand(1)
		vrp.addCustomer().setExternID('n32').setXlong(4).setYlat(4).setDemand(1)
		vrp.addCustomer().setExternID('n33').setXlong(-105).setYlat(-105).setDemand(1)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 3
		result.routes[0].events.count {e -> e.getID().startsWith("n1")} == 3
		result.routes[1].events.count {e -> e.getID().startsWith("n2")} == 3
		result.routes[2].events.count {e -> e.getID().startsWith("n3")} == 3
	}

	def "Consider max routes for 2 depots"() {
		XFVRP vrp = build()
		vrp.addVehicle().setName('V').setCapacity([3] as float[])

		vrp.addDepot().setExternID('nD1').setMaxNbrRoutes(1)
		vrp.addDepot().setExternID('nD2').setXlong(-100).setYlat(-100).setMaxNbrRoutes(1)
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand(1)
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand(1)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand(1)
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand(1)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand(1)
		vrp.addCustomer().setExternID('n6').setXlong(-105).setYlat(-105).setDemand(1)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 2
		result.routes.count {r -> r.events.get(0).ID == 'nD1'} == 1
		result.routes.count {r -> r.events.get(0).ID == 'nD2'} == 1
	}

	def "Consider max routes with SAVINGS"() {
		XFVRP vrp = build()
		vrp.clearOptTypes()
		vrp.addOptType(XFVRPOptTypes.SAVINGS)

		vrp.addVehicle().setName('V').setCapacity([3] as float[])

		vrp.addDepot().setExternID('nD').setMaxNbrRoutes(2)
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand(1)
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand(1)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand(1)
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand(1)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand(1)
		vrp.addCustomer().setExternID('n6').setXlong(-105).setYlat(-105).setDemand(1)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 2
		result.routes[0].vehicle.name == 'V'
		result.routes[1].vehicle.name == 'V'
	}

	def "Consider max routes with FIRST_BEST"() {
		XFVRP vrp = build()
		vrp.clearOptTypes()
		vrp.addOptType(XFVRPOptTypes.FIRST_BEST)

		vrp.addVehicle().setName('V').setCapacity([3] as float[])

		vrp.addDepot().setExternID('nD').setMaxNbrRoutes(2)
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand(1)
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand(1)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand(1)
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand(1)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand(1)
		vrp.addCustomer().setExternID('n6').setXlong(-105).setYlat(-105).setDemand(1)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 2
		result.routes[0].vehicle.name == 'V'
		result.routes[1].vehicle.name == 'V'
	}

	XFVRP build() {
		XFVRP vrp = new XFVRP()
		vrp.setMetric(Metrics.EUCLEDIAN.get())
		vrp.addOptType(XFVRPOptTypes.RELOCATE)
		vrp.addOptType(XFVRPOptTypes.SWAP)
		return vrp
	}
}
