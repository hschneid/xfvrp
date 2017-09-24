package xf.xfvrp.base.preset

import spock.lang.Specification
import util.instances.TestNode
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.fleximport.InternalCustomerData
import xf.xfvrp.base.monitor.StatusCode
import xf.xfvrp.base.monitor.StatusManager

class PresetDepotConverterSpec extends Specification {

	def service = new PresetDepotConverter();
	def statusManager = Mock StatusManager
	
	def "Set blocked depot index"() {
		def customers = [
			new InternalCustomerData(externID: "AA", presetDepotList: ["DEP1", "DEP2"]),
			new InternalCustomerData(externID: "AB", presetDepotList: ["DEP3"]),
			new InternalCustomerData(externID: "BA", presetDepotList: []),
			new InternalCustomerData(externID: "BB", presetDepotList: [])
			] as List<InternalCustomerData>
		
		def nodes = [
			new TestNode(globalIdx: 66, externID: "DEP1", siteType: SiteType.DEPOT).getNode(),
			new TestNode(globalIdx: 77, externID: "DEP2", siteType: SiteType.DEPOT).getNode(),
			new TestNode(globalIdx: 88, externID: "DEP3", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(3),
			customers.get(1).createCustomer(4),
			customers.get(2).createCustomer(5),
			customers.get(3).createCustomer(6)
			] as Node[]
				
		when:
		service.convert(nodes, customers, statusManager)
		def l1 = new ArrayList<Integer>()
		def l2 = new ArrayList<Integer>()
		l1.addAll(nodes[3].getPresetDepotList())
		l1.sort({c1, c2 -> c1 - c2})
		l2.addAll(nodes[4].getPresetDepotList())

		then:
		nodes[0].getPresetDepotList().size() == 0
		l1.size() == 2
		l1.get(0) == 66
		l1.get(1) == 77
		l2.size() == 1
		l2.get(0) == 88
		nodes[5].getPresetBlockVehicleList().size() == 0
		nodes[6].getPresetBlockVehicleList().size() == 0
	}
	
	def "Set blocked depot index wih wrong depot name"() {
		def customers = [
			new InternalCustomerData(externID: "AA", presetDepotList: ["DEP99"]),
			] as List<InternalCustomerData>
		
		def nodes = [
			new TestNode(globalIdx: 66, externID: "DEP1", siteType: SiteType.DEPOT).getNode(),
			new TestNode(globalIdx: 77, externID: "DEP2", siteType: SiteType.DEPOT).getNode(),
			new TestNode(globalIdx: 88, externID: "DEP3", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(3)
			] as Node[]
				
		when:
		service.convert(nodes, customers, statusManager)

		then:
		1 * statusManager.fireMessage(StatusCode.EXCEPTION,_)
		nodes[3].getPresetBlockVehicleList().size() == 0
	}
}
