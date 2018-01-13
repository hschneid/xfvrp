package xf.xfvrp.opt

import spock.lang.Specification
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType

class SolutionRoutesIteratorSpec extends Specification {

	def service = new SolutionRoutesIterator();

	def "Has next without holes in the middle"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);

		def sol = new SolutionRoutesIterator([[n1, n1, n1], [n1, n1], [n1, n1]] as Node[][])
		sol.currentIndex = 0;

		when:
		def result = sol.hasNext();

		then:
		result
		sol.length == 3
	}

	def "Has next without holes at the end"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);

		def sol = new SolutionRoutesIterator([[n1, n1, n1], [n1, n1], [n1, n1]] as Node[][])
		sol.currentIndex = 2;

		when:
		def result = sol.hasNext();

		then:
		!result
	}
	
	def "Has next with holes in the middle"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);

		def sol = new SolutionRoutesIterator([[n1, n1, n1], [], [n1, n1], [n1, n1], []] as Node[][])
		sol.currentIndex = 1;

		when:
		def result = sol.hasNext();

		then:
		result
		sol.length == 5
	}

	def "Has next with holes at the end"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);

		def sol = new SolutionRoutesIterator([[n1, n1, n1], [], [n1, n1], [n1, n1], []] as Node[][])
		sol.currentIndex = 3;

		when:
		def result = sol.hasNext();

		then:
		!result
	}
	
	def "Next without holes in the middle"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);

		def sol = new SolutionRoutesIterator([[n1, n1, n1], [n1, n1], [n1]] as Node[][])
		sol.currentIndex = 0;

		when:
		def result = sol.next();

		then:
		result.length == 2
	}

	def "Next without holes at the end"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);

		def sol = new SolutionRoutesIterator([[n1, n1, n1], [n1, n1], [n1]] as Node[][])
		sol.currentIndex = 2;

		when:
		def result = sol.next();

		then:
		result == null
	}
	
	def "Next with holes in the middle"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);

		def sol = new SolutionRoutesIterator([[n1, n1, n1], [], [n1, n1], [n1], []] as Node[][])
		sol.currentIndex = 1;

		when:
		def result = sol.next();

		then:
		result.length == 2
	}

	def "Next with holes at the end"() {
		def n1 = new Node(externID: "1", siteType: SiteType.DEPOT);

		def sol = new SolutionRoutesIterator([[n1, n1, n1], [], [n1, n1], [n1, n1], []] as Node[][])
		sol.currentIndex = 3;

		when:
		def result = sol.next();

		then:
		result == null
	}
}
