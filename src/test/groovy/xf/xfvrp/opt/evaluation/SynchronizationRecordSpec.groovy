package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.Quality
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.init.ModelBuilder
import xf.xfvrp.base.monitor.StatusManager

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Checks the Phase 2 extension of the SynchronizationAnalyzer: Transshipment
 * records link the visits of two different vehicles at the same transshipment
 * location (TL). A record is expressed by a pair of TL visits in different
 * routes, where the supplier route visits the TL with a negative amount
 * (drop) and the taker route visits the TL with the same positive amount
 * (take).
 *
 * @author lhackste
 */
class SynchronizationRecordSpec extends Specification {

	/** Global index (identity) and model index (metric row) of the two TLs */
	private static final int TL1 = 10, TL1_MODEL_IDX = 2
	private static final int TL2 = 20, TL2_MODEL_IDX = 3

	/** Transfer duration of the TL (service time) */
	private static final float TRANSFER_TIME = 5f

	def "Take visit with matching drop in another route is synchronized"() {
		given:
		def supplier = [depot(), customer(5), drop(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]
		def taker = [depot(), take(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]

		when:
		def q = evaluate(taker, [supplier, taker])

		then:
		q.getPenalty() == 0
	}

	def "Take visit without matching drop is penalized"() {
		given:
		def other = [depot(), customer(5), depot()] as Node[]
		def taker = [depot(), take(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]

		when:
		def q = evaluate(taker, [other, taker])

		then:
		q.getPenalty() == 1
	}

	def "Drop visit without matching take is penalized"() {
		given:
		// The dropped amount is covered by the collected load (100), but no
		// other route takes it.
		def supplier = [depot(), customer(100), drop(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]

		when:
		def q = evaluate(supplier, [supplier])

		then:
		q.getPenalty() == 1
	}

	def "Drop visit may not exceed the load, which was collected before"() {
		given:
		// The customer demand (5) is less than the dropped amount (100)
		def supplier = [depot(), customer(5), drop(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]
		def taker = [depot(), take(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]

		when:
		def q = evaluate(supplier, [supplier, taker])

		then:
		q.getPenalty() == 1
	}

	def "Take visit at the end of a route is not penalized by the pass-through rule"() {
		given:
		// Phase 1 rule: A zero-demand TL visit has to be followed by customers.
		// A record take visit (demand != 0) is exempted.
		def supplier = [depot(), customer(5), drop(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]
		def taker = [depot(), take(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]

		when:
		def q = evaluate(taker, [supplier, taker])

		then:
		q.getPenalty() == 0
	}

	def "Amounts of take and drop visit have to match"() {
		given:
		def supplier = [depot(), customer(5), drop(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]
		def taker = [depot(), take(TL1, TL1_MODEL_IDX, 90), depot()] as Node[]

		when:
		def q = evaluate(taker, [supplier, taker])

		then:
		q.getPenalty() == 1
	}

	def "Visits at different transshipment locations do not match"() {
		given:
		def supplier = [depot(), customer(5), drop(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]
		def taker = [depot(), take(TL2, TL2_MODEL_IDX, 100), depot()] as Node[]

		when:
		def q = evaluate(taker, [supplier, taker])

		then:
		q.getPenalty() == 1
	}

	def "Drop and take visit at same location in same route do not match"() {
		given:
		def route = [depot(), customer(100), drop(TL1, TL1_MODEL_IDX, 100), take(TL1, TL1_MODEL_IDX, 100), depot()] as Node[]

		when:
		def q = evaluate(route, [route])

		then:
		q.getPenalty() == 1
	}

	def "Transfer which does not finish before the TL closes is penalized"() {
		given:
		// The TL closes at time 10, but the transfer can start earliest after
		// the travel from the depot (approx. 70 time units).
		def supplier = [depot(), customer(5), drop(TL1, TL1_MODEL_IDX, 100, 10), depot()] as Node[]
		def taker = [depot(), take(TL1, TL1_MODEL_IDX, 100, 10), depot()] as Node[]

		when:
		def q = evaluate(taker, [supplier, taker])

		then:
		q.getPenalty() == 1
	}

	//////////////////////////////////////////

	/**
	 * Evaluates the given route in the context of the given routes and
	 * returns its quality.
	 */
	private Quality evaluate(Node[] route, List<Node[]> routes) {
		def model = model()
		def q = new Quality()

		SynchronizationAnalyzer.analyze(route, routes as Node[][], model, q)

		return q
	}

	private XFVRPModel model() {
		def nodes = [
				depot(),
				customer(5),
				tlNode(TL1, TL1_MODEL_IDX, 100),
				tlNode(TL2, TL2_MODEL_IDX, 100)
		] as Node[]

		def vehicle = new Vehicle(0, "V", 1, [1000f] as float[], Float.MAX_VALUE, 100, 0, 0, 0, 0, 0, 0, 0)
		return new ModelBuilder().build(
				nodes,
				[CompartmentType.MIXED] as CompartmentType[],
				vehicle,
				new EucledianMetric(),
				new XFVRPParameter(),
				new StatusManager()
		)
	}

	private Node depot() {
		return new Node(
				0, "DEP", SiteType.DEPOT,
				0, 0, -1,
				new float[0],
				new float[][]{new float[]{0, Integer.MAX_VALUE}},
				0, 0, LoadType.UNDEF, 0, "", 0
		)
	}

	/**
	 * Creates a base node of a transshipment location, which is located far
	 * away from the depot. The time window of the TL is [0, twClose].
	 */
	private Node tlNode(int globalIdx, int modelIdx, float twClose) {
		return new Node(
				globalIdx, "T" + globalIdx, SiteType.TRANSSHIPMENT,
				50, 50, -1,
				new float[0],
				new float[][]{new float[]{0, twClose}},
				TRANSFER_TIME, 0, LoadType.UNDEF, 0, "", 0
		)
	}

	/**
	 * Creates a customer node at the depot location.
	 */
	private Node customer(float demand) {
		return new Node(
				1, "C1", SiteType.CUSTOMER,
				0, 0, -1,
				[demand] as float[],
				new float[][]{new float[]{0, 100}},
				0, 0, LoadType.PICKUP, 0, "", 0
		)
	}

	/**
	 * Creates a drop visit (supplier side of a transshipment record) at the TL
	 * with the given identity. The visit carries the negative transferred
	 * amount.
	 */
	private Node drop(int tlGlobalIdx, int tlModelIdx, float amount, float twClose = 100) {
		Node n = tlNode(tlGlobalIdx, tlModelIdx, twClose)
		n.setIdx(tlModelIdx)
		n.setDemands([-amount] as float[])
		return n
	}

	/**
	 * Creates a take visit (taker side of a transshipment record) at the TL
	 * with the given identity. The visit carries the positive transferred
	 * amount.
	 */
	private Node take(int tlGlobalIdx, int tlModelIdx, float amount, float twClose = 100) {
		Node n = tlNode(tlGlobalIdx, tlModelIdx, twClose)
		n.setIdx(tlModelIdx)
		n.setDemands([amount] as float[])
		return n
	}
}
