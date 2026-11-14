package xf.xfvrp.opt.construct

import spock.lang.Specification
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.opt.construct.VRPTTTask.Mode

class VRPTTTaskEnumeratorSpec extends Specification {

	def "Customer without transshipment candidates gets a direct task only"() {
		given:
		def customer = customer(1)

		when:
		def tasks = new VRPTTTaskEnumerator().enumerate(customer, [])

		then:
		tasks.size() == 1
		tasks[0].mode == Mode.DIRECT
		tasks[0].customer.is(customer)
		tasks[0].transshipment == null
	}

	def "Customer gets a direct task and one VIA_TDP task per transshipment"() {
		given:
		def customer = customer(1)
		def transshipments = [transshipment(2), transshipment(3)]

		when:
		def tasks = new VRPTTTaskEnumerator().enumerate(customer, transshipments)

		then:
		tasks.size() == 3

		tasks[0].mode == Mode.DIRECT
		tasks[0].customer == customer

		tasks[1].mode == Mode.VIA_TDP
		tasks[1].transshipment == transshipments[0]
		tasks[1].customer == customer

		tasks[2].mode == Mode.VIA_TDP
		tasks[2].transshipment == transshipments[1]
		tasks[2].customer == customer
	}

	//////////////////////////////////////////

	private Node customer(int idx) {
		return new Node(
				idx, "C" + idx, SiteType.CUSTOMER,
				0, 0, -1,
				[5] as float[],
				new float[][]{new float[]{0, 100}},
				0, 0, LoadType.DELIVERY, 0, "", 0
		)
	}

	private Node transshipment(int idx) {
		return new Node(
				idx, "T" + idx, SiteType.TRANSSHIPMENT,
				0, 0, -1,
				new float[0],
				new float[][]{new float[]{0, 100}},
				5, 0, LoadType.UNDEF, 0, "", 0
		)
	}
}
