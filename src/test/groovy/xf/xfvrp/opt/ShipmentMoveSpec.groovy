package xf.xfvrp.opt

import spock.lang.Specification
import util.instances.Helper
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove

class ShipmentMoveSpec extends Specification {

	def service = new XFVRPSingleMove()
	
	def "Regular move - all right"() {
		def n0 = new Node(externID: "0", siteType: SiteType.DEPOT)
		def n1 = new Node(externID: "1", siteType: SiteType.CUSTOMER)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n99 = new Node(externID: "99", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n0, n1, n2, n3, n4, n5, n6, n99] as Node[])
		
		when:
		service.shipmentMove(sol, 1, 3, 4, 7)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "0"
		result[1].externID == "2"
		result[2].externID == "1"
		result[3].externID == "4"
		result[4].externID == "5"
		result[5].externID == "6"
		result[6].externID == "3"
		result[7].externID == "0"
	}
	
	def "Regular move - all left"() {
		def n0 = new Node(externID: "0", siteType: SiteType.DEPOT)
		def n1 = new Node(externID: "1", siteType: SiteType.CUSTOMER)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n99 = new Node(externID: "99", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n0, n1, n2, n3, n4, n5, n6, n99] as Node[])
		
		when:
		service.shipmentMove(sol, 4, 6, 1, 2)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "0"
		result[1].externID == "4"
		result[2].externID == "1"
		result[3].externID == "6"
		result[4].externID == "2"
		result[5].externID == "3"
		result[6].externID == "5"
		result[7].externID == "0"
	}
	
	def "Regular move - in the mid"() {
		def n0 = new Node(externID: "0", siteType: SiteType.DEPOT)
		def n1 = new Node(externID: "1", siteType: SiteType.CUSTOMER)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n99 = new Node(externID: "99", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n0, n1, n2, n3, n4, n5, n6, n99] as Node[])
		
		when:
		service.shipmentMove(sol, 1, 6, 3, 5)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "0"
		result[1].externID == "2"
		result[2].externID == "1"
		result[3].externID == "3"
		result[4].externID == "4"
		result[5].externID == "6"
		result[6].externID == "5"
		result[7].externID == "0"
	}
	
	def "Regular move - from the mid"() {
		def n0 = new Node(externID: "0", siteType: SiteType.DEPOT)
		def n1 = new Node(externID: "1", siteType: SiteType.CUSTOMER)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n99 = new Node(externID: "99", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n0, n1, n2, n3, n4, n5, n6, n99] as Node[])
		
		when:
		service.shipmentMove(sol, 3, 4, 1, 7)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "0"
		result[1].externID == "3"
		result[2].externID == "1"
		result[3].externID == "2"
		result[4].externID == "5"
		result[5].externID == "6"
		result[6].externID == "4"
		result[7].externID == "0"
	}
	
	def "Regular move - overlapping"() {
		def n0 = new Node(externID: "0", siteType: SiteType.DEPOT)
		def n1 = new Node(externID: "1", siteType: SiteType.CUSTOMER)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n99 = new Node(externID: "99", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n0, n1, n2, n3, n4, n5, n6, n99] as Node[])
		
		when:
		service.shipmentMove(sol, 2, 5, 4, 7)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "0"
		result[1].externID == "1"
		result[2].externID == "3"
		result[3].externID == "2"
		result[4].externID == "4"
		result[5].externID == "6"
		result[6].externID == "5"
		result[7].externID == "0"
	}
	
	def "Regular move - no move"() {
		def n0 = new Node(externID: "0", siteType: SiteType.DEPOT)
		def n1 = new Node(externID: "1", siteType: SiteType.CUSTOMER)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n99 = new Node(externID: "99", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n0, n1, n2, n3, n4, n5, n6, n99] as Node[])
		
		when:
		service.shipmentMove(sol, 2, 5, 2, 5)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "0"
		result[1].externID == "1"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "4"
		result[5].externID == "5"
		result[6].externID == "6"
		result[7].externID == "0"
	}
	
	def "Regular move - no move 2"() {
		def n0 = new Node(externID: "0", siteType: SiteType.DEPOT)
		def n1 = new Node(externID: "1", siteType: SiteType.CUSTOMER)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n99 = new Node(externID: "99", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n0, n1, n2, n3, n4, n5, n6, n99] as Node[])
		
		when:
		service.shipmentMove(sol, 2, 5, 3, 6)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "0"
		result[1].externID == "1"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "4"
		result[5].externID == "5"
		result[6].externID == "6"
		result[7].externID == "0"
	}
	
	def "Regular move - before same node"() {
		def n0 = new Node(externID: "0", siteType: SiteType.DEPOT)
		def n1 = new Node(externID: "1", siteType: SiteType.CUSTOMER)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n99 = new Node(externID: "99", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n0, n1, n2, n3, n4, n5, n6, n99] as Node[])
		
		when:
		service.shipmentMove(sol, 1, 2, 5, 5)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "0"
		result[1].externID == "3"
		result[2].externID == "4"
		result[3].externID == "1"
		result[4].externID == "2"
		result[5].externID == "5"
		result[6].externID == "6"
		result[7].externID == "0"
	}

}
