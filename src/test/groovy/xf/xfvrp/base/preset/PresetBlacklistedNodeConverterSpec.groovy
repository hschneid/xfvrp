package xf.xfvrp.base.preset

import spock.lang.Specification
import util.instances.TestNode
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.fleximport.CustomerData

class PresetBlacklistedNodeConverterSpec extends Specification {

	def service = new PresetBlacklistedNodeConverter();
	
	def "Set blacklisted node index"() {
		def customers = [
				new CustomerData(externID: "AA", presetRoutingBlackList: ["AB", "BB"]),
				new CustomerData(externID: "AB", presetRoutingBlackList: ["BA"]),
				new CustomerData(externID: "BA", presetRoutingBlackList: []),
				new CustomerData(externID: "BB", presetRoutingBlackList: [])
			] as List<CustomerData>
		
		def nodes = [
			new TestNode(externID: "DEP", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(1),
			customers.get(1).createCustomer(2),
			customers.get(2).createCustomer(3),
			customers.get(3).createCustomer(4)
			] as Node[]
				
		when:
		service.convert(nodes, customers)
		def l1 = new ArrayList<Integer>()
		def l2 = new ArrayList<Integer>()
		l1.addAll(nodes[1].getPresetRoutingBlackList())
		l1.sort({c1, c2 -> c1 - c2})
		l2.addAll(nodes[2].getPresetRoutingBlackList())

		then:
		nodes[0].getPresetRoutingBlackList().size() == 0
		l1.size() == 2
		l1.get(0) == 2
		l1.get(1) == 4
		l2.size() == 1
		l2.get(0) == 3
		nodes[3].getPresetRoutingBlackList().size() == 0
		nodes[4].getPresetRoutingBlackList().size() == 0
	}
	
	def "Set blacklisted node index with wrong name"() {
		def customers = [
			new CustomerData(externID: "AB", presetRoutingBlackList: ["CC"]),
			new CustomerData(externID: "BB", presetRoutingBlackList: []),
			] as List<CustomerData>
		
		def nodes = [
			new TestNode(externID: "DEP", siteType: SiteType.DEPOT).getNode(),
			customers.get(0).createCustomer(1),
			customers.get(1).createCustomer(2)
			] as Node[]
				
		when:
		service.convert(nodes, customers)

		then:
		nodes[1].getPresetRoutingBlackList().size() == 0
	}
}
