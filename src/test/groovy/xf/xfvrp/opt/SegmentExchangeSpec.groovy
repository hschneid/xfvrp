package xf.xfvrp.opt

import spock.lang.Specification
import util.instances.Helper
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSegmentExchange

class SegmentExchangeSpec extends Specification {

	def service = new XFVRPSegmentExchange()
	
	def "Regular path exchange - simple exchange"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		
		
		def sol = Helper.set([n1, n2, n3, n4] as Node[])
		
		when:
		service.change(sol, [-1, 0, 0, 1, 2, 0, 0, 0, -1] as float[])
		
		def result = Helper.get(sol)
		
		then:
		result[0].externID == "1"
		result[1].externID == "3"
		result[2].externID == "2"
		result[3].externID == "1"
	}
	
	def "Regular path exchange - src = dst"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)

		def sol = Helper.set([n1, n2, n3, n4] as Node[])
		
		when:
		service.change(sol, [-1, 0, 0, 1, 1, 0, 0, 0, -1] as float[])

		def result = Helper.get(sol)
		
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
		service.change(sol, [-1, 0, 0, 1, 3, 1, 0, 0, -1] as float[])

		def result = Helper.get(sol)
		
		then:
		result[0].externID == "1"
		result[1].externID == "4"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "1"
	}
	
	def "Regular path exchange - two routes"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
		def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
		def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
		def n7 = new Node(externID: "7", siteType: SiteType.DEPOT)
		
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])
		
		when:
		service.change(sol, [-1, 0, 1, 1, 1, 1, 0, 1, -1] as float[])

		def result = Helper.get(sol)

		then:
		result[0].externID == "1"
		result[1].externID == "5"
		result[2].externID == "1"
		result[3].externID == "4"
		result[4].externID == "3"
		result[5].externID == "2"
		result[6].externID == "6"
		result[7].externID == "4"
	}
	
	def "Path exchange - at the border"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		
		def sol = Helper.set([n1, n2, n3, n4, n1] as Node[])
		
		when:
		service.change(sol, [-1, 0, 0, 0, 2, 0, 0, 0, -1] as float[])

		then:
		thrown XFVRPException
	}
	
	def "Path exchange - over lapping"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
		def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
		def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
		def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
		def n5 = new Node(externID: "5", siteType: SiteType.DEPOT)
		
		def sol = Helper.set([n1, n2, n3, n4, n5] as Node[])
		
		when:
		service.change(sol, [-1, 0, 0, 1, 2, 1, 0, 0, -1] as float[])

		then:
		thrown XFVRPException
	}

}
