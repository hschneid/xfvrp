package xf.xfvrp.opt.construct

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.metric.Metrics
import xf.xfvrp.opt.XFVRPOptTypes

class XFVRPConstSpec extends Specification {

	def "Optimize with single depot and blocks"() {
		def vrp = getVRP()

		when:
		vrp.executeRoutePlanning()
		def rep = vrp.getReport()
		then:
		rep.routes.size() == 3
		rep.routes.find {r ->
			r.events.count {e -> e.ID == 'A1'} == 1 &&
					r.events.count {e -> e.ID == 'C1'} == 1 &&
					r.events.size() == 4} != null
		rep.routes.find {r ->
			r.events.count {e -> e.ID == 'B1'} == 1 &&
					r.events.count {e -> e.ID == 'B2'} == 1 &&
					r.events.size() == 4} != null
		rep.routes.find {r ->
			r.events.count {e -> e.ID == 'E1'} == 1 &&
					r.events.count {e -> e.ID == 'E2'} == 1 &&
					r.events.size() == 4} != null
	}

	def "Optimize with multiple depots and blocks"() {
		def vrp = getVRP()
		vrp.addDepot().setExternID('D2').setXlong(100).setYlat(100)

		when:
		vrp.executeRoutePlanning()
		def rep = vrp.getReport()

		def d1Routes = rep.routes.findAll {r -> r.events.get(0).ID == 'D1'}
		def d2Routes = rep.routes.findAll {r -> r.events.get(0).ID == 'D2'}
		then:
		rep.routes.size() == 4
		d1Routes.size() == 2
		d2Routes.size() == 2
		d1Routes.find {r -> r.events.count {e -> e.ID == 'A1'} == 1 && r.events.size() == 3} != null
		d1Routes.find {r ->
			r.events.count {e -> e.ID == 'B1'} == 1 &&
					r.events.count {e -> e.ID == 'B2'} == 1 &&
					r.events.size() == 4} != null
		d2Routes.find {r -> r.events.count {e -> e.ID == 'C1'} == 1 && r.events.size() == 3} != null
		d2Routes.find {r ->
			r.events.count {e -> e.ID == 'E1'} == 1 &&
					r.events.count {e -> e.ID == 'E2'} == 1 &&
					r.events.size() == 4} != null
	}

	def "Optimize with allowed depots"() {
		def vrp = getVRP()
		vrp.addDepot().setExternID('D2').setXlong(100).setYlat(100)
		vrp.addDepot().setExternID('D3').setXlong(200).setYlat(200)

		vrp.addCustomer().setExternID('F1').setXlong(10).setYlat(0).setDemand(5).setPresetDepotList(['D3'] as Set<String>)
		vrp.addCustomer().setExternID('G1').setXlong(10).setYlat(0).setDemand(5).setPresetDepotList(['D2', 'D3'] as Set<String>)
		vrp.addCustomer().setExternID('H1').setXlong(0).setYlat(10).setDemand(5).setPresetBlockName("H").setPresetDepotList(['D3'] as Set<String>)
		vrp.addCustomer().setExternID('H2').setXlong(10).setYlat(5).setDemand(5).setPresetBlockName("H").setPresetDepotList(['D3'] as Set<String>)

		when:
		vrp.executeRoutePlanning()
		def rep = vrp.getReport()

		def d1Routes = rep.routes.findAll {r -> r.events.get(0).ID == 'D1'}
		def d2Routes = rep.routes.findAll {r -> r.events.get(0).ID == 'D2'}
		def d3Routes = rep.routes.findAll {r -> r.events.get(0).ID == 'D3'}
		then:
		rep.routes.size() == 6
		d1Routes.size() == 2
		d2Routes.size() == 2
		d3Routes.size() == 2

		d1Routes.find {r -> r.events.count {e -> e.ID == 'A1'} == 1 && r.events.size() == 3} != null
		d1Routes.find {r ->
			r.events.count {e -> e.ID == 'B1'} == 1 &&
					r.events.count {e -> e.ID == 'B2'} == 1 &&
					r.events.size() == 4} != null
		d2Routes.find {r ->
			r.events.count {e -> e.ID == 'C1'} == 1 &&
					r.events.count {e -> e.ID == 'G1'} == 1 &&
					r.events.size() == 4} != null
		d2Routes.find {r ->
			r.events.count {e -> e.ID == 'E1'} == 1 &&
					r.events.count {e -> e.ID == 'E2'} == 1 &&
					r.events.size() == 4} != null
		d3Routes.find {r -> r.events.count {e -> e.ID == 'F1'} == 1 && r.events.size() == 3} != null
		d3Routes.find {r ->
			r.events.count {e -> e.ID == 'H1'} == 1 &&
					r.events.count {e -> e.ID == 'H2'} == 1 &&
					r.events.size() == 4} != null
	}

	static XFVRP getVRP() {
		def vrp = new XFVRP()
		vrp.addVehicle().setName('V1').setCapacity([10] as float[])
		// Add depots
		vrp.addDepot().setExternID('D1').setXlong(0).setYlat(0)
		// Add customers
		vrp.addCustomer().setExternID('A1').setXlong(10).setYlat(0).setDemand(5)
		vrp.addCustomer().setExternID('B1').setXlong(0).setYlat(10).setDemand(5).setPresetBlockName("B")
		vrp.addCustomer().setExternID('B2').setXlong(10).setYlat(5).setDemand(5).setPresetBlockName("B")
		vrp.addCustomer().setExternID('C1').setXlong(110).setYlat(100).setDemand(5)
		vrp.addCustomer().setExternID('E1').setXlong(100).setYlat(110).setDemand(5).setPresetBlockName("E")
		vrp.addCustomer().setExternID('E2').setXlong(110).setYlat(105).setDemand(5).setPresetBlockName("E")

		vrp.setMetric(Metrics.EUCLEDIAN.get())
		vrp.addOptType(XFVRPOptTypes.CONST)
		return vrp
	}
}
