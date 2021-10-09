package xf.xfvrp.opt.init.precheck.pdp

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.InvalidReason
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType

class PDPPreCheckServiceSpec extends Specification {

	def service = new PDPPreCheckService()
	def nd = new TestNode(externID: "nd").getNode()
	def nd2 = new TestNode(externID: "nd2").getNode()
	def nr = new TestNode(externID: "nr").getNode()
	def n1 = new TestNode(externID: "n1", demand: [10, 10]).getNode()
	def n2 = new TestNode(externID: "n2", demand: [13, 13]).getNode()
	def n3 = new TestNode(externID: "n3", demand: [11, 11]).getNode()
	def n4 = new TestNode(externID: "n4", demand: [-33,-33]).getNode()

	def "Get Valid Nodes - normal"() {
		def customers = [n1, n2, n4] as List<Node>
		def nodesPerType = [
			(SiteType.DEPOT) : [nd, nd2],
			(SiteType.REPLENISH) : [nr],
			(SiteType.CUSTOMER) : [n1, n2, n3, n4]
		]

		when:
		def result = service.getValidNodes(customers, nodesPerType)

		then:
		result.length == 6
		result[0].externID == "nd"
		result[1].externID == "nd2"
		result[2].externID == "nr"
		result[3].externID == "n1"
		result[4].externID == "n2"
		result[5].externID == "n4"
	}
	
	def "Check capacity - normal"() {
		def customers = [n1, n2, n3, n4] as List<Node>
		def vehicle = new TestVehicle(name: "V1", capacity: [10, 10] as float[]).getVehicle()
		
		when:
		service.checkCapacity(customers, vehicle)
		
		then:
		customers[0].invalidReason == InvalidReason.NONE
		customers[1].invalidReason == InvalidReason.PDP_IMPROPER_AMOUNTS
		customers[2].invalidReason == InvalidReason.PDP_IMPROPER_AMOUNTS
		customers[3].invalidReason == InvalidReason.NONE
	}
	
	def "Check capacity - more capacity dims than demand"() {
		def customers = [n1, n2, n3, n4] as List<Node>
		def vehicle = new TestVehicle(name: "V1", capacity: [10, 10, 10] as float[]).getVehicle()
		
		when:
		service.checkCapacity(customers, vehicle)
		
		then:
		customers[0].invalidReason == InvalidReason.NONE
		customers[1].invalidReason == InvalidReason.PDP_IMPROPER_AMOUNTS
		customers[2].invalidReason == InvalidReason.PDP_IMPROPER_AMOUNTS
		customers[3].invalidReason == InvalidReason.NONE
	}
	
	def "Check capacity - no customer"() {
		def customers = [] as List<Node>
		def vehicle = new TestVehicle(name: "V1", capacity: [10, 10] as float[]).getVehicle()
		
		when:
		service.checkCapacity(customers, vehicle)
		
		then:
		customers.size() == 0
	}
	
	def "Remove Uncomplete Shipments - ship id is wrong"() {
		def n1 = new TestNode(externID: "n1", shipID: null, shipmentIdx: -1, demand: [10, 10]).getNode()
		def n2 = new TestNode(externID: "n2", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n3 = new TestNode(externID: "n3", shipID: "B", shipmentIdx: 2, demand: [11, 11]).getNode()
		def n4 = new TestNode(externID: "n4", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()
		def n5 = new TestNode(externID: "n5", shipID: "", shipmentIdx: -1, demand: [-33,-33]).getNode()
		def n6 = new TestNode(externID: "n6", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n7 = new TestNode(externID: "n7", shipID: "B", shipmentIdx: 2, demand: [11, 11]).getNode()
		def n8 = new TestNode(externID: "n8", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()

		def customers = [n1, n2, n3, n4, n5, n6, n7, n8] as List<Node>
	
		when:
		service.removeUncompleteShipments(customers)
		
		then:
		n1.invalidReason == InvalidReason.PDP_INCOMPLETE
		n2.invalidReason == InvalidReason.NONE
		n3.invalidReason == InvalidReason.NONE
		n4.invalidReason == InvalidReason.NONE
		n5.invalidReason == InvalidReason.PDP_INCOMPLETE
		n6.invalidReason == InvalidReason.NONE
		n7.invalidReason == InvalidReason.NONE
		n8.invalidReason == InvalidReason.NONE
	}
	
	def "Remove Uncomplete Shipments - too many nodes per shipment"() {
		def n1 = new TestNode(externID: "n1", shipID: "D", shipmentIdx: 4, demand: [10, 10]).getNode()
		def n2 = new TestNode(externID: "n2", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n3 = new TestNode(externID: "n3", shipID: "A", shipmentIdx: 1, demand: [11, 11]).getNode()
		def n4 = new TestNode(externID: "n4", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()
		def n5 = new TestNode(externID: "n5", shipID: "D", shipmentIdx: 4, demand: [-33,-33]).getNode()
		def n6 = new TestNode(externID: "n6", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n8 = new TestNode(externID: "n8", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()

		def customers = [n1, n2, n3, n4, n5, n6, n8] as List<Node>
	
		when:
		service.removeUncompleteShipments(customers)
		
		then:
		n1.invalidReason == InvalidReason.NONE
		n2.invalidReason == InvalidReason.PDP_ILLEGAL_NUMBER_OF_CUSTOMERS_PER_SHIPMENT
		n3.invalidReason == InvalidReason.PDP_ILLEGAL_NUMBER_OF_CUSTOMERS_PER_SHIPMENT
		n4.invalidReason == InvalidReason.NONE
		n5.invalidReason == InvalidReason.NONE
		n6.invalidReason == InvalidReason.PDP_ILLEGAL_NUMBER_OF_CUSTOMERS_PER_SHIPMENT
		n8.invalidReason == InvalidReason.NONE
	}
	
	def "Remove Uncomplete Shipments - not enough nodes per shipment"() {
		def n1 = new TestNode(externID: "n1", shipID: "D", shipmentIdx: 4, demand: [10, 10]).getNode()
		def n2 = new TestNode(externID: "n2", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n4 = new TestNode(externID: "n4", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()
		def n5 = new TestNode(externID: "n5", shipID: "D", shipmentIdx: 4, demand: [-33,-33]).getNode()
		def n6 = new TestNode(externID: "n6", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n7 = new TestNode(externID: "n7", shipID: "B", shipmentIdx: 2, demand: [11, 11]).getNode()
		def n8 = new TestNode(externID: "n8", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()

		def customers = [n1, n2, n4, n5, n6, n7, n8] as List<Node>
	
		when:
		service.removeUncompleteShipments(customers)
		
		then:
		n1.invalidReason == InvalidReason.NONE
		n2.invalidReason == InvalidReason.NONE
		n4.invalidReason == InvalidReason.NONE
		n5.invalidReason == InvalidReason.NONE
		n6.invalidReason == InvalidReason.NONE
		n7.invalidReason == InvalidReason.PDP_INCOMPLETE
		n8.invalidReason == InvalidReason.NONE
	}
	
	def "Check Shipments - normal"() {
		def n1 = new TestNode(externID: "n1", shipID: "D", shipmentIdx: 4, demand: [10, 10]).getNode()
		def n2 = new TestNode(externID: "n2", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n3 = new TestNode(externID: "n3", shipID: "B", shipmentIdx: 2, demand: [11, 11]).getNode()
		def n4 = new TestNode(externID: "n4", shipID: "C", shipmentIdx: 3, demand: [33, 33]).getNode()
		def n5 = new TestNode(externID: "n5", shipID: "D", shipmentIdx: 4, demand: [-10,-10]).getNode()
		def n6 = new TestNode(externID: "n6", shipID: "A", shipmentIdx: 1, demand: [-13,-13]).getNode()
		def n7 = new TestNode(externID: "n7", shipID: "B", shipmentIdx: 2, demand: [-11,-11]).getNode()
		def n8 = new TestNode(externID: "n8", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()

		def customers = [n1, n2, n3, n4, n5, n6, n7, n8] as List<Node>
		def ships = [
			1 : [n2, n6] as Node[],
			2 : [n3, n7] as Node[],
			3 : [n4, n8] as Node[],
			4 : [n1, n5] as Node[]
			]
	
		when:
		service.checkShipments(ships, customers)
		
		then:
		n1.invalidReason == InvalidReason.NONE
		n2.invalidReason == InvalidReason.NONE
		n3.invalidReason == InvalidReason.NONE
		n4.invalidReason == InvalidReason.NONE
		n5.invalidReason == InvalidReason.NONE
		n6.invalidReason == InvalidReason.NONE
		n7.invalidReason == InvalidReason.NONE
		n8.invalidReason == InvalidReason.NONE
		customers.size() == 8
	}
	
	def "Check Shipments - amounts not fitting"() {
		def n1 = new TestNode(externID: "n1", shipID: "D", shipmentIdx: 4, demand: [10, 10]).getNode()
		def n2 = new TestNode(externID: "n2", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n3 = new TestNode(externID: "n3", shipID: "B", shipmentIdx: 2, demand: [11, 11]).getNode()
		def n4 = new TestNode(externID: "n4", shipID: "C", shipmentIdx: 3, demand: [31, 33]).getNode()
		def n5 = new TestNode(externID: "n5", shipID: "D", shipmentIdx: 4, demand: [-10,-10]).getNode()
		def n6 = new TestNode(externID: "n6", shipID: "A", shipmentIdx: 1, demand: [13,13]).getNode()
		def n7 = new TestNode(externID: "n7", shipID: "B", shipmentIdx: 2, demand: [-11,-11]).getNode()
		def n8 = new TestNode(externID: "n8", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()

		def customers = [n1, n2, n3, n4, n5, n6, n7, n8] as List<Node>
		def ships = [
			1 : [n2, n6] as Node[],
			2 : [n3, n7] as Node[],
			3 : [n4, n8] as Node[],
			4 : [n1, n5] as Node[]
			]
	
		when:
		service.checkShipments(ships, customers)
		
		then:
		n1.invalidReason == InvalidReason.NONE
		n2.invalidReason == InvalidReason.PDP_IMPROPER_AMOUNTS
		n3.invalidReason == InvalidReason.NONE
		n4.invalidReason == InvalidReason.PDP_IMPROPER_AMOUNTS
		n5.invalidReason == InvalidReason.NONE
		n6.invalidReason == InvalidReason.PDP_IMPROPER_AMOUNTS
		n7.invalidReason == InvalidReason.NONE
		n8.invalidReason == InvalidReason.PDP_IMPROPER_AMOUNTS
		customers.size() == 4
		customers.contains(n1)
		customers.contains(n3)
		customers.contains(n5)
		customers.contains(n7)
	}
	
	def "Check Shipments - amounts not fitting but IGNORE_IMPROPER_AMOUNTS"() {
		def n1 = new TestNode(externID: "n1", shipID: "D", shipmentIdx: 4, demand: [10, 10]).getNode()
		def n2 = new TestNode(externID: "n2", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n3 = new TestNode(externID: "n3", shipID: "B", shipmentIdx: 2, demand: [11, 11]).getNode()
		def n4 = new TestNode(externID: "n4", shipID: "C", shipmentIdx: 3, demand: [31, 33]).getNode()
		def n5 = new TestNode(externID: "n5", shipID: "D", shipmentIdx: 4, demand: [-10,-10]).getNode()
		def n6 = new TestNode(externID: "n6", shipID: "A", shipmentIdx: 1, demand: [14,14]).getNode()
		def n7 = new TestNode(externID: "n7", shipID: "B", shipmentIdx: 2, demand: [-11,-11]).getNode()
		def n8 = new TestNode(externID: "n8", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()

		def customers = [n1, n2, n3, n4, n5, n6, n7, n8] as List<Node>
		def ships = [
			1 : [n2, n6] as Node[],
			2 : [n3, n7] as Node[],
			3 : [n4, n8] as Node[],
			4 : [n1, n5] as Node[]
			]
			
		service.IGNORE_IMPROPER_AMOUNTS = true
	
		when:
		service.checkShipments(ships, customers)
		
		then:
		n1.invalidReason == InvalidReason.NONE
		n2.invalidReason == InvalidReason.NONE
		n3.invalidReason == InvalidReason.NONE
		n4.invalidReason == InvalidReason.NONE
		n5.invalidReason == InvalidReason.NONE
		n6.invalidReason == InvalidReason.NONE
		n7.invalidReason == InvalidReason.NONE
		n8.invalidReason == InvalidReason.NONE
		customers.size() == 8
		n4.demand[0] == 33
		n8.demand[0] == -33
		n2.demand[0] == 14
		n6.demand[0] == -14
	}
	
	def "Get Shipments - normal"() {
		def n1 = new TestNode(externID: "n1", shipID: "D", shipmentIdx: 4, demand: [10, 10]).getNode()
		def n2 = new TestNode(externID: "n2", shipID: "A", shipmentIdx: 1, demand: [13, 13]).getNode()
		def n3 = new TestNode(externID: "n3", shipID: "B", shipmentIdx: 2, demand: [11, 11]).getNode()
		def n4 = new TestNode(externID: "n4", shipID: "C", shipmentIdx: 3, demand: [33, 33]).getNode()
		def n5 = new TestNode(externID: "n5", shipID: "D", shipmentIdx: 4, demand: [-10,-10]).getNode()
		def n6 = new TestNode(externID: "n6", shipID: "A", shipmentIdx: 1, demand: [-13,-13]).getNode()
		def n7 = new TestNode(externID: "n7", shipID: "B", shipmentIdx: 2, demand: [-11,-11]).getNode()
		def n8 = new TestNode(externID: "n8", shipID: "C", shipmentIdx: 3, demand: [-33,-33]).getNode()

		def customers = [n1, n2, n3, n4, n5, n6, n7, n8] as List<Node>
			
		when:
		def result = service.getShipments(customers)
		
		then:
		result.size() == 4
		result.get(1)[0] == n2
		result.get(1)[1] == n6
		result.get(2)[0] == n3
		result.get(2)[1] == n7
		result.get(3)[0] == n4
		result.get(3)[1] == n8
		result.get(4)[0] == n1
		result.get(4)[1] == n5
	}
	
	def "Get Shipments - empty"() {
		when:
		def result = service.getShipments([] as List<Node>)
		
		then:
		result != null
		result.size() == 0
	}
}
