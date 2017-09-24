package xf.xfvrp.base.preset

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.fleximport.InternalCustomerData
import xf.xfvrp.base.monitor.StatusManager

class BlockedVehicleListConverterSpec extends Specification {

	def service = new BlockedVehicleListConverter();
	def statusManager = Mock StatusManager
	
	def "Set blocked vehicle index"() {
		def customers = [
			new InternalCustomerData(externID: "AA", presetBlockVehicleList: ["V2", "V4"]),
			new InternalCustomerData(externID: "AB", presetBlockVehicleList: ["V3"]),
			new InternalCustomerData(externID: "BA", presetBlockVehicleList: []),
			new InternalCustomerData(externID: "BB", presetBlockVehicleList: null)
			] as List<InternalCustomerData>
		
		def nodes = [
			new TestNode(externID: "DEP", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(1),
			customers.get(1).createCustomer(2),
			customers.get(2).createCustomer(3),
			customers.get(3).createCustomer(4)
			] as Node[]
			
		def vehicles = [
			new TestVehicle(idx: 0, name: "V1").getVehicle(),
			new TestVehicle(idx: 1, name: "V2").getVehicle(),
			new TestVehicle(idx: 2, name: "V3").getVehicle(),
			new TestVehicle(idx: 3, name: "V4").getVehicle(),
		] as Vehicle[]
				
		when:
		service.convert(nodes, customers, vehicles, statusManager)
		def l1 = new ArrayList<Integer>()
		def l2 = new ArrayList<Integer>()
		l1.addAll(nodes[1].getPresetBlockVehicleList())
		l1.sort({c1, c2 -> c1 - c2})
		l2.addAll(nodes[2].getPresetBlockVehicleList())

		then:
		nodes[0].getPresetBlockVehicleList().size() == 0
		l1.size() == 2
		l1.get(0) == 1
		l1.get(1) == 3
		l2.size() == 1
		l2.get(0) == 2
		nodes[3].getPresetBlockVehicleList().size() == 0
		nodes[4].getPresetBlockVehicleList().size() == 0
	}
	
	def "Set blocked vehicle index wih wrong vehicle name"() {
		def customers = [
			new InternalCustomerData(externID: "AB", presetBlockVehicleList: ["V99"]),
			] as List<InternalCustomerData>
		
		def nodes = [
			new TestNode(externID: "DEP", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(1)
			] as Node[]
			
		def vehicles = [
			new TestVehicle(idx: 0, name: "V1").getVehicle()
		] as Vehicle[]
				
		when:
		service.convert(nodes, customers, vehicles, statusManager)

		then:
		1 * statusManager.fireMessage(_,_)
		nodes[1].getPresetBlockVehicleList().size() == 0
	}
}
