package groovy

import spock.lang.Specification
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.XFVRPOptBase
import xf.xfvrp.opt.improve.XFVRPRelocate

class SwapSpec extends Specification {

	def service = new XFVRPRelocate();
	
	def "Test1"() {
		def n1 = new Node(externID: "1");
		def n2 = new Node(externID: "2");
		def n3 = new Node(externID: "3");
		def n4 = new Node(externID: "4");
		
		def sol = new Solution(giantRoute: [n1, n2, n3, n4])
		
		when:
		service.swap(sol, 1, 2);
		
		then:
		sol.giantRoute[0].externID == "1"
		sol.giantRoute[1].externID == "3"
		sol.giantRoute[2].externID == "2"
		sol.giantRoute[3].externID == "4"
	}
}
