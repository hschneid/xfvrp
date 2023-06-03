package xf.xfvrp.opt

import spock.lang.Specification
import util.instances.Helper
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveUtil

class XFVRPMoveUtilNodeMoveSpec extends Specification {

	def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
	def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
	def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
	def n4 = new Node(externID: "4", siteType: SiteType.DEPOT)
	def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
	def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
	def n7 = new Node(externID: "7", siteType: SiteType.DEPOT)

	def "Regular move - between 2 routes"() {
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])

		when:
		XFVRPMoveUtil.change(sol, [0, 0, 1, 1, 2, 0, 0] as float[])
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "1"
		result[1].externID == "3"
		result[2].externID == "4"
		result[3].externID == "5"
		result[4].externID == "2"
		result[5].externID == "6"
		result[6].externID == "1"
	}

	def "Regular move - same route and src < dst"() {
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])

		when:
		XFVRPMoveUtil.change(sol, [0, 0, 0, 1, 3, 0, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "3"
		result[2].externID == "2"
		result[3].externID == "4"
		result[4].externID == "5"
		result[5].externID == "6"
		result[6].externID == "1"
	}

	def "Regular move - same route and src > dst"() {
		
		def sol = Helper.set([n1, n2, n3, n5, n6, n7] as Node[])

		when:
		XFVRPMoveUtil.change(sol, [0, 0, 0, 3, 1, 0, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "5"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "6"
		result[5].externID == "1"
	}

	def "Regular move - no move - same positions"() {
		
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])

		when:
		XFVRPMoveUtil.change(sol, [0, 0, 0, 1, 1, 0, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "4"
		result[4].externID == "5"
		result[5].externID == "6"
		result[6].externID == "1"
	}

	def "Regular move - no move - different positions"() {
		
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])

		when:
		XFVRPMoveUtil.change(sol, [0, 0, 0, 1, 2, 0, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "4"
		result[4].externID == "5"
		result[5].externID == "6"
		result[6].externID == "1"
	}

	def "Irregular move - src is first node"() {
		
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])

		when:
		XFVRPMoveUtil.change(sol, [0, 0, 1, 0, 2, 0, 0] as float[])

		then:
		thrown(XFVRPException)
	}

	def "Irregular move - dst is first node"() {
		
		def sol = Helper.set([n1, n2, n3, n4, n5, n6, n7] as Node[])

		when:
		XFVRPMoveUtil.change(sol, [0, 0, 1, 2, 0, 0, 0] as float[])

		then:
		thrown(XFVRPException)
	}
}
