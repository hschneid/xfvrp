package xf.xfvrp.opt

import spock.lang.Specification
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveUtil

class XFVRPMoveUtilSpec extends Specification {

	def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
	def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
	def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
	def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
	def n5 = new Node(externID: "5", siteType: SiteType.DEPOT)
	def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
	def n7 = new Node(externID: "7", siteType: SiteType.CUSTOMER)
	def n8 = new Node(externID: "8", siteType: SiteType.DEPOT)

	def sol = new Solution()

	def "segment move - normal move"() {
		sol.setGiantRoute([n1, n2, n3, n4, n5, n6, n7, n8] as Node[])

		when:
		XFVRPMoveUtil.move(sol, 0, 1, 2, 3, 2)

		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "5"
		result[3].externID == "6"
		result[4].externID == "3"
		result[5].externID == "4"
		result[6].externID == "7"
		result[7].externID == "5"
	}

	def "segment move - take all from 1 route to another"() {
		sol.setGiantRoute([n1, n2, n3, n4, n5, n8] as Node[])

		when:
		XFVRPMoveUtil.move(sol, 0, 1, 1, 3, 1)

		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "5"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "4"
		result[5].externID == "5"
	}

	def "segment move - move segment to same position normally impossible"() {
		sol.setGiantRoute([n1, n2, n3, n4, n5, n6, n7, n8] as Node[])

		when:
		XFVRPMoveUtil.move(sol, 0, 0, 2, 3, 4)

		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "3"
		result[3].externID == "4"
		result[4].externID == "5"
		result[5].externID == "6"
		result[6].externID == "7"
		result[7].externID == "5"
	}

	def "segment move - with segment size = 1"() {
		sol.setGiantRoute([n1, n2, n3, n4, n5, n6, n7, n8] as Node[])

		when:
		XFVRPMoveUtil.move(sol, 0, 1,1, 1, 1)

		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "3"
		result[2].externID == "4"
		result[3].externID == "5"
		result[4].externID == "2"
		result[5].externID == "6"
		result[6].externID == "7"
		result[7].externID == "5"
	}

	def "segment move - same route - move to the right"() {
		sol.setGiantRoute([n1, n2, n3, n4, n6, n7, n8] as Node[])

		when:
		XFVRPMoveUtil.move(sol, 0, 0,2, 3, 6)

		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "6"
		result[3].externID == "7"
		result[4].externID == "3"
		result[5].externID == "4"
		result[6].externID == "1"
	}

	def "segment move - same route - move to the left"() {
		sol.setGiantRoute([n1, n2, n3, n4, n6, n7, n8] as Node[])

		when:
		XFVRPMoveUtil.move(sol, 0, 0,4, 5, 2)

		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "6"
		result[3].externID == "7"
		result[4].externID == "3"
		result[5].externID == "4"
		result[6].externID == "1"
	}

	def "segment move - same route - move directly before src"() {
		sol.setGiantRoute([n1, n2, n3, n4, n6, n7, n8] as Node[])

		when:
		XFVRPMoveUtil.move(sol, 0, 0,3, 5, 2)

		def result = sol.getGiantRoute()

		then:
		result[0].externID == "1"
		result[1].externID == "2"
		result[2].externID == "4"
		result[3].externID == "6"
		result[4].externID == "7"
		result[5].externID == "3"
		result[6].externID == "1"
	}

	def "segment move - negative area size"() {
		sol.setGiantRoute([n1, n2, n3, n4, n5, n6, n7, n8] as Node[])

		when:
		XFVRPMoveUtil.move(sol, 0, 1, 2, 1, 3)

		sol.getGiantRoute()
		
		then:
		thrown XFVRPException
	}
}
