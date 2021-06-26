package xf.xfvrp.opt.init.precheck

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.exception.XFVRPException

class PreCheckServiceSpec extends Specification {

	def service = new PreCheckService()
	def veh = new TestVehicle(name: "V1").getVehicle()
	def nd = new TestNode(externID: "nd").getNode()
	def nd2 = new TestNode(externID: "nd2").getNode()
	def nr = new TestNode(externID: "nr", siteType: SiteType.REPLENISH).getNode()
	def n1 = new TestNode(externID: "n1", demand: [10, 10]).getNode()
	def n2 = new TestNode(externID: "n2", demand: [13, 13]).getNode()
	def n3 = new TestNode(externID: "n3", demand: [11, 11]).getNode()
	def n4 = new TestNode(externID: "n4", demand: [-33,-33]).getNode()

	def "Extern ID is not unique"() {
		def nr = new TestNode(externID: "nd2").getNode()
		def n1 = new TestNode(externID: "n3", demand: [10, 10]).getNode()

		def nodes = [nd, nd2, nr, n1, n2, n3, n4] as Node[]
		def vehicle = veh
		def parameter = new XFVRPParameter()


		when:
		service.precheck(nodes, vehicle, parameter)

		then:
		def ex = thrown(XFVRPException)
		ex.message.contains("n3, nd2")
	}

	def "One node in block is not fitting to vehicle type - remove block"() {
		def nd = new TestNode(siteType: SiteType.DEPOT, externID: "nd").getNode()
		def n1 = new TestNode(siteType: SiteType.CUSTOMER, externID: "n1", demand: [10, 10], presetBlockIdx: 2, presetVehicleIdx: 1, presetVehicleIdx2: 3).getNode()
		def n2 = new TestNode(siteType: SiteType.CUSTOMER, externID: "n2", demand: [10, 10], presetBlockIdx: 2, presetVehicleIdx: 1, presetVehicleIdx2: 8).getNode()
		def n3 = new TestNode(siteType: SiteType.CUSTOMER, externID: "n3", demand: [10, 10], presetBlockIdx: 3, presetVehicleIdx: 1, presetVehicleIdx2: 3).getNode()
		def n4 = new TestNode(siteType: SiteType.CUSTOMER, externID: "n4", demand: [10, 10], presetBlockIdx: 3, presetVehicleIdx: 1, presetVehicleIdx2: 3).getNode()

		def nodes = [nd, nr, n1, n2, n3, n4] as Node[]
		def vehicle = new TestVehicle(name: "V1", idx: 3).getVehicle()
		def parameter = new XFVRPParameter()

		when:
		def res = service.precheck(nodes, vehicle, parameter)
		then:
		res.length == 4
		res.count {r -> r.externID == "n1"} == 0
		res.count {r -> r.externID == "n2"} == 0
	}

}
