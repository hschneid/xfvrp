package xf.xfvrp.opt.improve.routebased.swap

import spock.lang.Specification
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.opt.Solution

class XFVRPSwapUtilSpec extends Specification {

	def nd = new Node(externID: "D", siteType: SiteType.DEPOT)
	def n1 = new Node(externID: "1", siteType: SiteType.CUSTOMER)
	def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
	def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
	def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
	def n5 = new Node(externID: "5", siteType: SiteType.CUSTOMER)
	def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)

	def "Change - 1 node exchange - 2 routes"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, nd, n3, n4, nd] as Node[])
		
		when:
		XFVRPSwapUtil.change(sol, [0, 1, 1, 2, 0, 0, 0] as float[])
		def result = sol.getGiantRoute()
		
		then:
		result[0].externID == "D"
		result[1].externID == "4"
		result[2].externID == "2"
		result[3].externID == "D"
		result[4].externID == "3"
		result[5].externID == "1"
		result[6].externID == "D"
	}

	def "Change - 1 node exchange - 1 route - a < b"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 0, 2, 4, 0, 0, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "1"
		result[2].externID == "4"
		result[3].externID == "3"
		result[4].externID == "2"
		result[5].externID == "D"
	}

	def "Change - 1 node exchange - 1 route - b < a"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 0, 4, 2, 0, 0, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "1"
		result[2].externID == "4"
		result[3].externID == "3"
		result[4].externID == "2"
		result[5].externID == "D"
	}

	def "Change - 1 node exchange - 1 route - b directly before a"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 0, 2, 3, 0, 0, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "1"
		result[2].externID == "3"
		result[3].externID == "2"
		result[4].externID == "4"
		result[5].externID == "D"
	}

	def "Change - 1 node exchange - 1 route - a directly before b"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 0, 3, 2, 0, 0, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "1"
		result[2].externID == "3"
		result[3].externID == "2"
		result[4].externID == "4"
		result[5].externID == "D"
	}

	//////////////////////////////////////////////////

	def "Change - segment exchange - 2 routes - same length"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, nd, n4, n5, n6, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 1, 2, 1, 1, 1, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "1"
		result[2].externID == "4"
		result[3].externID == "5"
		result[4].externID == "D"
		result[5].externID == "2"
		result[6].externID == "3"
		result[7].externID == "6"
		result[8].externID == "D"
	}

	def "Change - segment exchange - 2 routes - different lengths"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, nd, n4, n5, n6, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 1, 1, 2, 2, 1, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "5"
		result[2].externID == "6"
		result[3].externID == "D"
		result[4].externID == "4"
		result[5].externID == "1"
		result[6].externID == "2"
		result[7].externID == "3"
		result[8].externID == "D"
	}

	def "Change - segment exchange - 1 route - different lengths - A < B"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, n5, n6, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 0, 1, 4, 1, 2, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "4"
		result[2].externID == "5"
		result[3].externID == "6"
		result[4].externID == "3"
		result[5].externID == "1"
		result[6].externID == "2"
		result[7].externID == "D"
	}

	def "Change - segment exchange - 1 route - differnt lengths - B < A"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, n5, n6, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 0, 4, 2, 2, 1, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "1"
		result[2].externID == "4"
		result[3].externID == "5"
		result[4].externID == "6"
		result[5].externID == "2"
		result[6].externID == "3"
		result[7].externID == "D"
	}

	def "Change - segment exchange - 1 route - same lengths - A < B"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, n5, n6, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 0, 1, 4, 1, 1, 0] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "4"
		result[2].externID == "5"
		result[3].externID == "3"
		result[4].externID == "1"
		result[5].externID == "2"
		result[6].externID == "6"
		result[7].externID == "D"
	}

	def "Change - segment exchange - 1 route - different lengths - Invert both"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, n5, n6, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 0, 1, 4, 1, 2, XFVRPSwapUtil.BOTH_INVERT] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "6"
		result[2].externID == "5"
		result[3].externID == "4"
		result[4].externID == "3"
		result[5].externID == "2"
		result[6].externID == "1"
		result[7].externID == "D"
	}

	def "Change - segment exchange - 1 route - different lengths - Only invert B "() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, n5, n6, nd] as Node[])

		when:
		XFVRPSwapUtil.change(sol, [0, 0, 1, 4, 1, 2, XFVRPSwapUtil.B_INVERT] as float[])
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "6"
		result[2].externID == "5"
		result[3].externID == "4"
		result[4].externID == "3"
		result[5].externID == "1"
		result[6].externID == "2"
		result[7].externID == "D"
	}

	////////////////////////////////////////////////////////////

	def "Change/Reverse - 2 routes - different lengths - both inverts"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, nd, n4, n5, n6, nd] as Node[])

		def para = [0, 1, 1, 2, 2, 1, XFVRPSwapUtil.BOTH_INVERT] as float[]

		when:
		XFVRPSwapUtil.change(sol, para)
		XFVRPSwapUtil.reverseChange(sol, para)
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "1"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "D"
		result[5].externID == "4"
		result[6].externID == "5"
		result[7].externID == "6"
		result[8].externID == "D"
	}

	def "Change/Reverse - 1 route - different lengths - B invert"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, n5, n6, nd] as Node[])

		def para = [0, 0, 6, 1, 0, 2, XFVRPSwapUtil.B_INVERT] as float[]

		when:
		XFVRPSwapUtil.change(sol, para)
		XFVRPSwapUtil.reverseChange(sol, para)
		def result = sol.getGiantRoute()

		then:
		result[0].externID == "D"
		result[1].externID == "1"
		result[2].externID == "2"
		result[3].externID == "3"
		result[4].externID == "4"
		result[5].externID == "5"
		result[6].externID == "6"
		result[7].externID == "D"
	}

	///////////////////////////////////////////////////
	def "Change - Segment contains leading depot"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, nd, n4, n5, n6, nd] as Node[])

		def para = [0, 1, 1, 0, 1, 1, 0] as float[]

		when:
		XFVRPSwapUtil.change(sol, para)

		then:
		thrown(XFVRPException)
	}
	def "Change - Segment contains trailing depot"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, nd, n4, n5, n6, nd] as Node[])

		def para = [0, 1, 3, 1, 1, 1, 0] as float[]

		when:
		XFVRPSwapUtil.change(sol, para)

		then:
		thrown(XFVRPException)
	}
	def "Change - Segments are overlapping"() {
		def sol = new Solution()
		sol.setGiantRoute([nd, n1, n2, n3, n4, n5, n6, nd] as Node[])

		def para = [0, 0, 2, 4, 2, 1, 0] as float[]

		when:
		XFVRPSwapUtil.change(sol, para)

		then:
		thrown(XFVRPException)
	}
}
