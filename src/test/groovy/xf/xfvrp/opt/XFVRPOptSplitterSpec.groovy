package xf.xfvrp.opt

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.base.monitor.StatusManager

class XFVRPOptSplitterSpec extends Specification {

	def "Failing block optimization is propagated as XFVRPException"() {
		given:
		def depot = new TestNode(externID: "DEP", siteType: SiteType.DEPOT, demand: [0]).getNode()
		def c1 = new TestNode(externID: "1", globalIdx: 1).getNode()
		def c2 = new TestNode(externID: "2", globalIdx: 2).getNode()

		def model = TestXFVRPModel.get([depot, c1, c2], new TestVehicle(name: "V1").getVehicle())
		def sol = Helper.set(model, [depot, c1, c2, depot] as xf.xfvrp.base.Node[])

		def opt = new XFVRPOptBase() {
					Solution execute(Solution s) {
						throw new RuntimeException("boom")
					}
				}

		when:
		new XFVRPOptSplitter().execute(sol, model, new StatusManager(), opt)

		then:
		thrown(XFVRPException)
	}
}
