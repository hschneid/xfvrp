package xf.xfvrp.base.preset

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.monitor.DefaultStatusMonitor
import xf.xfvrp.opt.XFVRPOptType
import xf.xfvrp.report.Report

import java.util.stream.Collectors

class PresetBlockSpec extends Specification {

	def "Consider block presets - name, position and rank"() {
		def vrp = build()
		// no block, no pos, no rank
		vrp.addCustomer().setExternID("NB1").setDemand(1).setXlong(100).setYlat(100)
		vrp.addCustomer().setExternID("NB2").setDemand(1).setXlong(101).setYlat(101)
		vrp.addCustomer().setExternID("NB3").setDemand(1).setXlong(102).setYlat(102)
		vrp.addCustomer().setExternID("NB4").setDemand(1).setXlong(103).setYlat(103)
		vrp.addCustomer().setExternID("NB5").setDemand(1).setXlong(100).setYlat(110)
		vrp.addCustomer().setExternID("NB6").setDemand(1).setXlong(101).setYlat(111)
		vrp.addCustomer().setExternID("NB7").setDemand(1).setXlong(102).setYlat(112)
		vrp.addCustomer().setExternID("NB8").setDemand(1).setXlong(103).setYlat(113)
		// no block, pos, rank (pos & rank are ignored)
		vrp.addCustomer().setExternID("NBPoRa1").setDemand(1).setXlong(108).setYlat(108).setPresetBlockPos(3).setPresetBlockRank(1)
		vrp.addCustomer().setExternID("NBPoRa2").setDemand(1).setXlong(119).setYlat(119).setPresetBlockPos(2).setPresetBlockRank(2)
		vrp.addCustomer().setExternID("NBPoRa3").setDemand(1).setXlong(117).setYlat(117).setPresetBlockPos(1).setPresetBlockRank(3)
		// block, no pos, no rank
		vrp.addCustomer().setExternID("BA1").setDemand(50).setXlong(100).setYlat(100).setPresetBlockName("A")
		vrp.addCustomer().setExternID("BA2").setDemand(1).setXlong(101).setYlat(101).setPresetBlockName("A")
		vrp.addCustomer().setExternID("BA3").setDemand(1).setXlong(102).setYlat(102).setPresetBlockName("A")
		vrp.addCustomer().setExternID("BB1").setDemand(50).setXlong(100).setYlat(100).setPresetBlockName("B")
		vrp.addCustomer().setExternID("BB2").setDemand(1).setXlong(101).setYlat(101).setPresetBlockName("B")
		vrp.addCustomer().setExternID("BB3").setDemand(1).setXlong(102).setYlat(102).setPresetBlockName("B")
		// block, pos, rank (rank is ignored)
		vrp.addCustomer().setExternID("BC1").setDemand(1).setXlong(100).setYlat(100).setPresetBlockName("C").setPresetBlockRank(3).setPresetBlockPos(1)
		vrp.addCustomer().setExternID("BC2").setDemand(1).setXlong(101).setYlat(101).setPresetBlockName("C").setPresetBlockRank(2).setPresetBlockPos(2)
		vrp.addCustomer().setExternID("BC3").setDemand(1).setXlong(102).setYlat(102).setPresetBlockName("C").setPresetBlockRank(1).setPresetBlockPos(3)
		// block, pos, no rank
		vrp.addCustomer().setExternID("BD1").setDemand(1).setXlong(100).setYlat(100).setPresetBlockName("D").setPresetBlockPos(20)
		vrp.addCustomer().setExternID("BD2").setDemand(1).setXlong(101).setYlat(101).setPresetBlockName("D").setPresetBlockPos(12)
		vrp.addCustomer().setExternID("BD3").setDemand(1).setXlong(102).setYlat(102).setPresetBlockName("D").setPresetBlockPos(33)
		// block, no pos, rank
		vrp.addCustomer().setExternID("BE1").setDemand(1).setXlong(100).setYlat(100).setPresetBlockName("E").setPresetBlockRank(10)
		vrp.addCustomer().setExternID("BE2").setDemand(1).setXlong(101).setYlat(101).setPresetBlockName("E").setPresetBlockRank(3)
		vrp.addCustomer().setExternID("BE3").setDemand(1).setXlong(102).setYlat(102).setPresetBlockName("E").setPresetBlockRank(5)

		when:
		vrp.executeRoutePlanning()
		def res = vrp.getReport()

		then:
		res.routes.size() == 2
		// Check blocks
		checkBlock(res, "A")
		checkBlock(res, "B")
		checkBlock(res, "C")
		checkBlock(res, "D")
		checkBlock(res, "E")
		checkPos(res, "BC1BC2BC3")
		checkPos(res, "BD2BD1BD3")
		checkRank(res, "E", [2,3,1] as int[])
	}

	def "Invalid block - constraint violation"() {
		def vrp = build()
		// demand too big for capacity of all nodes in block
		vrp.addCustomer().setExternID("NB1").setDemand(60).setXlong(100).setYlat(100).setPresetBlockName('A')
		vrp.addCustomer().setExternID("NB2").setDemand(39).setXlong(101).setYlat(101).setPresetBlockName('A')
		vrp.addCustomer().setExternID("NB3").setDemand(2).setXlong(102).setYlat(102).setPresetBlockName('A')

		when:
		vrp.executeRoutePlanning()
		def res = vrp.getReport()

		then:
		res.routes.size() == 1
		res.routes[0].vehicle.name == 'INVALID'
	}

	def "Invalid block - position duplicate"() {
		def vrp = build()

		vrp.addCustomer().setExternID("BE1").setDemand(1).setXlong(100).setYlat(100).setPresetBlockName("E").setPresetBlockPos(10)
		vrp.addCustomer().setExternID("BE2").setDemand(1).setXlong(101).setYlat(101).setPresetBlockName("E").setPresetBlockPos(3)
		vrp.addCustomer().setExternID("BE3").setDemand(1).setXlong(102).setYlat(102).setPresetBlockName("E").setPresetBlockPos(5)
		vrp.addCustomer().setExternID("BE4").setDemand(1).setXlong(102).setYlat(102).setPresetBlockName("E").setPresetBlockPos(5)

		when:
		vrp.executeRoutePlanning()

		then:
		thrown(XFVRPException)
	}

	def "Invalid block - negative rank"() {
		def vrp = build()

		// block, no pos, rank
		vrp.addCustomer().setExternID("BE1").setDemand(1).setXlong(100).setYlat(100).setPresetBlockName("E").setPresetBlockRank(10)
		vrp.addCustomer().setExternID("BE2").setDemand(1).setXlong(101).setYlat(101).setPresetBlockName("E").setPresetBlockRank(-3)
		vrp.addCustomer().setExternID("BE3").setDemand(1).setXlong(102).setYlat(102).setPresetBlockName("E").setPresetBlockRank(5)

		when:
		vrp.executeRoutePlanning()

		then:
		thrown(XFVRPException)
	}

	boolean checkBlock(Report rep, String blockName) {
		return rep.routes.count(route -> route.events.find{e -> e.ID.startsWith('B'+ blockName)}) == 1
	}

	boolean checkPos(Report rep, String nodes) {
		return rep.routes.stream()
				.flatMap(route -> route.events.stream())
				.map(e -> e.ID)
				.collect(Collectors.joining()).contains(nodes)
	}

	void checkRank(Report rep, String blockName, int[] order) {
		def route = rep.routes.find {route -> route.events.find {e -> e.ID.startsWith('B'+blockName)}}
		int[] pos = new int[order.length];
		for (i in 0..<order.length) {
			for (j in 0..<route.events.size()) {
				if(route.events[j].ID.equals('B'+blockName+order[i])) {
					pos[i] = j
					break
				}
			}
		}
		// Check
		for (i in 0..<order.length - 1) {
			assert pos[i] < pos[i + 1]
		}
	}

	private XFVRP build() {
		XFVRP xfvrp = new XFVRP()
		xfvrp.setStatusMonitor(new DefaultStatusMonitor())

		xfvrp.addVehicle().setCapacity(100).setName("V")
		xfvrp.addDepot().setExternID("DD")

		xfvrp.addOptType(XFVRPOptType.ILS)

		xfvrp.setMetric(new EucledianMetric())

		return xfvrp
	}
}
