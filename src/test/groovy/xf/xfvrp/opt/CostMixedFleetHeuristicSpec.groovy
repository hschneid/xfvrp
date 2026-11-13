package xf.xfvrp.opt

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.opt.fleetmix.CostMixedFleetHeuristic
import xf.xfvrp.opt.fleetmix.DefaultMixedFleetHeuristic

class CostMixedFleetHeuristicSpec extends Specification {

	def "merge: the default plan leaves two routes of two types, one vehicle could drive both"() {
		when:
		def plain = plan(new DefaultMixedFleetHeuristic())
		def cost = plan(new CostMixedFleetHeuristic())

		then: 'the default: V1 (priority 1, 2 places) keeps one route, V2 takes the rest'
		plain.routes.size() == 2
		and: 'by cost: all four customers on one route of V2'
		cost.routes.size() == 1
		cost.routes[0].vehicle.name == 'V2'
		cost.routes[0].events.count { it.siteType.name() == 'CUSTOMER' } == 4
		cost.summary.cost < plain.summary.cost
	}

	def "change: a route moves to a cheaper type that has a vehicle left"() {
		XFVRP xfvrp = new XFVRP()
		xfvrp.addVehicle().setName('BIG').setCapacity([10] as float[]).setFixCost(100).setVarCost(1).setCount(1)
		xfvrp.addVehicle().setName('SMALL').setCapacity([5] as float[]).setFixCost(1).setVarCost(1).setCount(1)
		xfvrp.setMetric(Metrics.EUCLEDIAN.get())
		xfvrp.addOptType(XFVRPOptTypes.RELOCATE)
		xfvrp.addDepot().setExternID('D').setXlong(0).setYlat(0)
		(1..3).each { xfvrp.addCustomer().setExternID('c' + it).setDemand(1).setXlong(it).setYlat(0) }
		xfvrp.getParameters().setMixedFleetHeuristic(new CostMixedFleetHeuristic())

		when:
		xfvrp.executeRoutePlanning()
		def rep = xfvrp.getReport()

		then:
		rep.routes.size() == 1
		rep.routes[0].vehicle.name == 'SMALL'
	}

	def "limits: no type is used more often than it has vehicles; every customer is served once"() {
		when:
		def rep = plan(new CostMixedFleetHeuristic())

		then:
		rep.routes.groupBy { it.vehicle.name }.every { name, routes -> routes.size() <= 1 }
		rep.routes.collectMany { r -> r.events.findAll { it.siteType.name() == 'CUSTOMER' }*.ID }.sort() == ['c1', 'c2', 'c3', 'c4']
	}

	private static plan(heuristic) {
		XFVRP xfvrp = new XFVRP()
		xfvrp.addVehicle().setName('V1').setCapacity([2] as float[]).setFixCost(10).setVarCost(1).setCount(1).setPriority(1)
		xfvrp.addVehicle().setName('V2').setCapacity([4] as float[]).setFixCost(10).setVarCost(1).setCount(1).setPriority(2)
		xfvrp.setMetric(Metrics.EUCLEDIAN.get())
		xfvrp.addOptType(XFVRPOptTypes.RELOCATE)
		xfvrp.addDepot().setExternID('D').setXlong(0).setYlat(0)
		(1..4).each { xfvrp.addCustomer().setExternID('c' + it).setDemand(1).setXlong(it).setYlat(0) }
		xfvrp.getParameters().setMixedFleetHeuristic(heuristic)
		xfvrp.executeRoutePlanning()
		return xfvrp.getReport()
	}
}
