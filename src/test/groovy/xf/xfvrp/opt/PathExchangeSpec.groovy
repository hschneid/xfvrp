package xf.xfvrp.opt

import spock.lang.Specification
import util.instances.Helper
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove

class PathExchangeSpec extends Specification {

	def service = new XFVRPSingleMove()
	
	def "Regular path exchange - simple exchange"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n1, n2, n3, n4] as Node[])
		
		when:
		service.exchange(sol, 1, 2, 0, 0)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "3"
		result[2].externID == "2"
		result[3].externID == "1"
	}
	
	def "Regular path exchange - all same"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n1, n2, n3, n4] as Node[])
		
		when:
		service.exchange(sol, 1, 1, 0, 0)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "1"
	}
	
	def "Regular path exchange - one route"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n1, n2, n3, n4, n5] as Node[])
		
		when:
		service.exchange(sol, 1, 3, 1, 0)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "4"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "1"
	}
	
	def "Regular path exchange - two routes after src area"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n7 = new Node(externID: "7", siteType: SiteType.DEPOT)

		
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])
		
		when:
		service.exchange(sol, 1, 4, 1, 0)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "5"
		result[2].externID == "4"
		result[3].externID == "2"
		result[4].externID == "3"
		result[5].externID == "6"
		result[6].externID == "1"
	}
	
	def "Regular path exchange - two routes before src area"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n7 = new Node(externID: "7", siteType: SiteType.DEPOT)

		
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])
		
		when:
		service.exchange(sol, 4, 1, 0, 1)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "5"
		result[2].externID == "4"
		result[3].externID == "2"
		result[4].externID == "3"
		result[5].externID == "6"
		result[6].externID == "1"
	}
	
	def "Path exchange - at the border"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)

		
		def sol = Helper.set([n1, n2, n3, n4, n1] as Node[])
		
		when:
		service.exchange(sol, 0, 3, 1, 1)
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "4"
		result[1].externID == "1"
		result[2].externID == "3"
		result[3].externID == "1"
		result[4].externID == "2"
	}

	/*def "Path exchange - with exception"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)

		
		def sol = Helper.set([n1, n2, n3, n4] as Node[])
		
		when:
		service.exchange(sol, 1, 2, 0, 3)

		def result = sol.getGiantRoute()

		then:
		result != null
		// thrown ArrayIndexOutOfBoundsException
	}*/
	
	def "Path exchange - over lapping"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n1, n2, n3, n4, n5] as Node[])
		
		when:
		service.exchange(sol, 1, 2, 1, 1)
		
		def result = sol.getGiantRoute()
		
		then:
		thrown XFVRPException
	}

}
