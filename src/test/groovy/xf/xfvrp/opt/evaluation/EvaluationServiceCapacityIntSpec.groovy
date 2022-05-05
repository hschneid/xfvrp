package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.opt.XFVRPOptTypes

import java.util.stream.Collectors

class EvaluationServiceCapacityIntSpec extends Specification {

	def "Check capacities with different compartment types - all fit on 1 truck"() {
		XFVRP vrp = build()
		vrp.addVehicle().setName('V').setCapacity([3, 2, 3] as float[])
		vrp.addCompartment(CompartmentType.DELIVERY)
		vrp.addCompartment(CompartmentType.PICKUP)
		vrp.addCompartment(CompartmentType.MIXED)

		vrp.addDepot().setExternID('nD')
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		// Pickup of 1. compartment will be ignored
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand([1,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		// Pickup of 1. compartment will be ignored
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand([1,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 1
		result.routes[0].vehicle.name == 'V'
	}

	def "Check capacities - more routes because of overloads"() {
		XFVRP vrp = build()
		vrp.addVehicle().setName('V').setCapacity([2, 1, 2] as float[])
		vrp.addCompartment(CompartmentType.DELIVERY)
		vrp.addCompartment(CompartmentType.PICKUP)
		vrp.addCompartment(CompartmentType.MIXED)

		vrp.addDepot().setExternID('nD')
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 2
		result.routes[0].vehicle.name == 'V'
		result.routes[1].vehicle.name == 'V'
		result.routes.count {r -> r.events.size() == 5} == 1
		result.routes.count {r -> r.events.size() == 4} == 1
	}

	def "No given compartments, detect compartments automatically"() {
		XFVRP vrp = build()
		vrp.addVehicle().setName('V').setCapacity([3, 2, 3] as float[])

		vrp.addDepot().setExternID('nD')
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 1
		result.routes[0].vehicle.name == 'V'
	}

	def "Not all compartments are given, detect missing compartments automatically"() {
		XFVRP vrp = build()
		vrp.addCompartment(CompartmentType.DELIVERY)
		vrp.addVehicle().setName('V').setCapacity([3, 2, 3] as float[])

		vrp.addDepot().setExternID('nD')
		vrp.addCustomer().setExternID('n1').setXlong(100).setYlat(100).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n2').setXlong(101).setYlat(101).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n3').setXlong(102).setYlat(102).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n4').setXlong(103).setYlat(103).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n5').setXlong(104).setYlat(104).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 1
		result.routes[0].vehicle.name == 'V'
	}

	def "Check with replenishment"() {
		XFVRP vrp = build()
		vrp.addOptType(XFVRPOptTypes.ILS)
		// vrp.getParameters().nbrOfILSLoops = 100
		vrp.addVehicle().setName('V').setCapacity([3, 2, 3] as float[])
		vrp.addCompartment(CompartmentType.DELIVERY)
		vrp.addCompartment(CompartmentType.PICKUP)
		vrp.addCompartment(CompartmentType.MIXED)

		vrp.addDepot().setExternID('nD')
		vrp.addReplenishment().setExternID('nR').setXlong(80).setYlat(80)
		vrp.addCustomer().setExternID('n11').setXlong(100).setYlat(100).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n12').setXlong(101).setYlat(101).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n13').setXlong(102).setYlat(102).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n14').setXlong(103).setYlat(103).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n15').setXlong(104).setYlat(104).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n21').setXlong(100).setYlat(100).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n22').setXlong(101).setYlat(101).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n23').setXlong(102).setYlat(102).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n24').setXlong(103).setYlat(103).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n25').setXlong(104).setYlat(104).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 1
		result.routes[0].vehicle.name == 'V'
		result.routes[0].events.get(6).getID() == 'nR'
	}

	def "Check with inactive replenishment and presets"() {
		XFVRP vrp = build()
		vrp.addOptType(XFVRPOptTypes.ILS)
		vrp.addVehicle().setName('V').setCapacity([5, 3] as float[])
		vrp.addCompartment(CompartmentType.MIXED_NO_REPLENISH)
		vrp.addCompartment(CompartmentType.MIXED)

		vrp.addDepot().setExternID('DD')

		vrp.addReplenishment().setExternID('R').setXlong(80).setYlat(80)
		vrp.addCustomer().setExternID('A').setXlong(101).setYlat(101).setDemand([2,1] as float[]).setLoadType(LoadType.PICKUP).setPresetBlockName('A').setPresetBlockPos(1)
		vrp.addCustomer().setExternID('B').setXlong(100).setYlat(100).setDemand([1,2] as float[]).setLoadType(LoadType.DELIVERY).setPresetBlockName('A').setPresetBlockPos(2)
		vrp.addCustomer().setExternID('C').setXlong(103).setYlat(103).setDemand([2,2] as float[]).setLoadType(LoadType.PICKUP).setPresetBlockName('B').setPresetBlockPos(1)
		vrp.addCustomer().setExternID('D').setXlong(102).setYlat(102).setDemand([1,1] as float[]).setLoadType(LoadType.DELIVERY).setPresetBlockName('B').setPresetBlockPos(2)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 1
		result.routes[0].vehicle.name == 'V'
		result.routes[0].events.stream().map(n -> n.getID()).collect( Collectors.joining( "" ) ) == 'DDABRCDDD'
	}

	def "Check without replenishment"() {
		XFVRP vrp = build()
		vrp.addOptType(XFVRPOptTypes.ILS)
		vrp.addVehicle().setName('V').setCapacity([3, 2, 3] as float[])
		vrp.addCompartment(CompartmentType.DELIVERY_NO_REPLENISH)
		vrp.addCompartment(CompartmentType.PICKUP_NO_REPLENISH)
		vrp.addCompartment(CompartmentType.MIXED_NO_REPLENISH)

		vrp.addDepot().setExternID('nD')
		vrp.addReplenishment().setExternID('nR').setXlong(102).setYlat(102)
		vrp.addCustomer().setExternID('n11').setXlong(100).setYlat(100).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n12').setXlong(101).setYlat(101).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n13').setXlong(102).setYlat(102).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n14').setXlong(103).setYlat(103).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n15').setXlong(104).setYlat(104).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n21').setXlong(100).setYlat(100).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n22').setXlong(101).setYlat(101).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n23').setXlong(102).setYlat(102).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)
		vrp.addCustomer().setExternID('n24').setXlong(103).setYlat(103).setDemand([0,1,1] as float[]).setLoadType(LoadType.PICKUP)
		vrp.addCustomer().setExternID('n25').setXlong(104).setYlat(104).setDemand([1,0,1] as float[]).setLoadType(LoadType.DELIVERY)

		when:
		vrp.executeRoutePlanning()
		def result = vrp.getReport()

		then:
		result.routes.size() == 2
		result.routes[0].vehicle.name == 'V'
		result.routes[1].vehicle.name == 'V'
		result.routes[0].events.size() == 7
		result.routes[1].events.size() == 7
		result.routes.stream().flatMap(r -> r.events.stream()).filter(f -> f.getID() == 'nR').count() == 0
	}

	XFVRP build() {
		XFVRP vrp = new XFVRP()
		vrp.setMetric(Metrics.EUCLEDIAN.get())
		vrp.addOptType(XFVRPOptTypes.RELOCATE)
		return vrp
	}
}
