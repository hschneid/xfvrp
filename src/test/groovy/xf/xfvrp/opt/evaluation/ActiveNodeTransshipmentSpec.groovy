package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Checks the handling of stranded transshipment visits: A visit of a
 * transshipment location, which transfers no load (no demand, part of no
 * transshipment record) and which is not followed by a customer, is
 * deactivated in the evaluation. Such a visit is a pure detour, which the
 * optimization may leave behind when customers are relocated.
 *
 * @author lhackste
 */
class ActiveNodeTransshipmentSpec extends Specification {

	def "Stranded transshipment visit is deactivated"() {
		given:
		def route = [depot(), transshipment(0), depot()] as Node[]

		when:
		def activeNodes = ActiveNodeAnalyzer.getActiveNodes(route as Node[])

		then:
		activeNodes.length == 2
		activeNodes.every { it.getSiteType() == SiteType.DEPOT }
	}

	def "Transshipment visit followed by customers stays active"() {
		given:
		def route = [depot(), transshipment(0), customer(5), depot()] as Node[]

		when:
		def activeNodes = ActiveNodeAnalyzer.getActiveNodes(route as Node[])

		then:
		activeNodes.length == 4
	}

	def "Transshipment visit of a record stays active"() {
		given:
		// The visit carries the transferred amount and hauls it back to the depot
		def route = [depot(), transshipment(10), depot()] as Node[]

		when:
		def activeNodes = ActiveNodeAnalyzer.getActiveNodes(route as Node[])

		then:
		activeNodes.length == 3
		activeNodes[1].getSiteType() == SiteType.TRANSSHIPMENT
	}

	def "Stranded transshipment visit before a record visit stays active if it transfers no load"() {
		given:
		// The first TL visit is stranded, the second transfers load (record take)
		def route = [depot(), transshipment(0), transshipment(10), depot()] as Node[]

		when:
		def activeNodes = ActiveNodeAnalyzer.getActiveNodes(route as Node[])

		then:
		// The stranded visit is deactivated, the record visit stays
		activeNodes.length == 3
		activeNodes[1].getDemand()[0] == 10f
	}

	def "Transshipment visit before a zero demand customer is deactivated"() {
		given:
		def route = [depot(), transshipment(0), customer(0), depot()] as Node[]

		when:
		def activeNodes = ActiveNodeAnalyzer.getActiveNodes(route as Node[])

		then:
		activeNodes.length == 3
		activeNodes.count { it.getSiteType() == SiteType.CUSTOMER } == 1
	}

	//////////////////////////////////////////

	private Node depot() {
		return new Node(
				0, "DEP", SiteType.DEPOT,
				0, 0, -1,
				[0] as float[],
				new float[][]{new float[]{0, Integer.MAX_VALUE}},
				0, 0, LoadType.UNDEF, 0, "", 0
		)
	}

	private Node transshipment(float demand) {
		return new Node(
				1, "T1", SiteType.TRANSSHIPMENT,
				0, 0, -1,
				(demand != 0) ? ([demand] as float[]) : new float[0],
				new float[][]{new float[]{0, 100}},
				5, 0, LoadType.UNDEF, 0, "", 0
		)
	}

	private Node customer(float demand) {
		return new Node(
				2, "C1", SiteType.CUSTOMER,
				0, 0, -1,
				[demand] as float[],
				new float[][]{new float[]{0, 100}},
				0, 0, LoadType.PICKUP, 0, "", 0
		)
	}
}
