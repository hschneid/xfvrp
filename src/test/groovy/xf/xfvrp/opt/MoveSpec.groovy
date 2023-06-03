package xf.xfvrp.opt

import spock.lang.Specification
import util.instances.Helper
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove

class MoveSpec extends Specification {

	def service = new XFVRPSingleMove()
	
	def "Regular move - same position"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)

		def sol = Helper.set([n1, n2, n3, n4] as Node[])
		
		when:
		service.move(sol, 1, 1)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "1"
	}
	
	def "Regular move - no change"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n1, n2, n3, n4] as Node[])
		
		when:
		service.move(sol, 1, 2)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "1"
	}
	
	def "Regular move - one route"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n1, n2, n3, n4] as Node[])
		
		when:
		service.move(sol, 1, 3)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "3"
		result[2].externID == "2"
		result[3].externID == "1"
	}
	
	def "Regular move - two routes"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n7 = new Node(externID: "7", siteType: SiteType.DEPOT)

		
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])
		
		when:
		service.move(sol, 2, 5)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "4"
		result[3].externID == "5"
		result[4].externID == "3"
		result[5].externID == "6"
		result[6].externID == "1"
	}
	
	def "Move - at the border"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n1, n2, n3, n4] as Node[])
		
		when:
		service.move(sol, 1, 0)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "2"
		result[1].externID == "1"
		result[2].externID == "3"
		result[3].externID == "1"
	}
}
