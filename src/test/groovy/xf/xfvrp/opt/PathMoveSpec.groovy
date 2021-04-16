package xf.xfvrp.opt

import spock.lang.Specification
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.opt.improve.routebased.move.XFVRPNodeMove

class PathMoveSpec extends Specification {

	def service = new XFVRPNodeMove();
	
	def "Regular path move - same position"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER);
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER);
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT);
		
		def sol = new Solution()
		sol.setGiantRoute([n1, n2, n3, n4] as Node[])
		
		when:
		service.pathMove(sol, 1, 2, 3);
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "1"
	}
	
	def "Regular path move - all same"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER);
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER);
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT);
		
		def sol = new Solution()
		sol.setGiantRoute([n1, n2, n3, n4] as Node[])
		
		when:
		service.pathMove(sol, 1, 1, 1);
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "1"
	}
	
	def "Regular path move - one route"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER);
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER);
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER);
		def n5 = new Node(externID: "5", siteType: SiteType.DEPOT);
		
		def sol = new Solution()
		sol.setGiantRoute([n1, n2, n3, n4, n5] as Node[])
		
		when:
		service.pathMove(sol, 1, 2, 4);
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "4"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "1"
	}
	
	def "Regular path move - two routes after src area"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER);
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER);
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT);
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER);
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER);
		def n7 = new Node(externID: "7", siteType: SiteType.DEPOT);

		def sol = new Solution()
		sol.setGiantRoute([n1, n2, n3, n4, n5, n6, n7] as Node[])
		
		when:
		service.pathMove(sol, 2, 3, 6);
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "5"
		result[3].externID == "6"
		result[4].externID == "3"
		result[5].externID == "4"
		result[6].externID == "4"
	}
	
	def "Regular path move - two routes before src area"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER);
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER);
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT);
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER);
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER);
		def n7 = new Node(externID: "7", siteType: SiteType.DEPOT);

		def sol = new Solution()
		sol.setGiantRoute([n1, n2, n3, n4, n5, n6, n7] as Node[])
		
		when:
		service.pathMove(sol, 4, 5, 1);
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "5"
		result[2].externID == "6"
		result[3].externID == "2"
		result[4].externID == "3"
		result[5].externID == "4"
		result[6].externID == "4"
	}
	
	def "Path move - at the border"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER);
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER);
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER);
		def n5 = new Node(externID: "5", siteType: SiteType.DEPOT);
		
		def sol = new Solution()
		sol.setGiantRoute([n1, n2, n3, n4, n5] as Node[])
		
		when:
		service.pathMove(sol, 2, 3, 0);
		
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "3"
		result[1].externID == "4"
		result[2].externID == "1"
		result[3].externID == "2"
		result[4].externID == "1"
	}

	def "Path move - with exception"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER);
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER);
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT);
		
		def sol = new Solution()
		sol.setGiantRoute([n1, n2, n3, n4] as Node[])
		
		when:
		service.pathMove(sol, 2, 3, 5);
		
		def result = sol.getGiantRoute()
		
		then:
		thrown ArrayIndexOutOfBoundsException
	}
	
	def "Path move - negative area size"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER);
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER);
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER);
		def n5 = new Node(externID: "5", siteType: SiteType.DEPOT);
		
		def sol = new Solution()
		sol.setGiantRoute([n1, n2, n3, n4, n5] as Node[])
		
		when:
		service.pathMove(sol, 2, 1, 4);
		
		def result = sol.getGiantRoute()
		
		then:
		thrown XFVRPException
	}

}
