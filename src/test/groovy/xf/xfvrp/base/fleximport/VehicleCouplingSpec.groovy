package xf.xfvrp.base.fleximport

import spock.lang.Specification
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.exception.XFVRPException

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Checks the additive coupled capacity of the VRPTT: While a lorry pulls a
 * trailer, its effective capacity is the sum of lorry and trailer capacity
 * (train capacity), because the coupled trailer always travels with the
 * lorry (fixed coupling of Phase 1/2).
 *
 * @author lhackste
 */
class VehicleCouplingSpec extends Specification {

	def "Lorry with coupled trailer has additive train capacity"() {
		given:
		def importer = new FlexiImporter()
		importer.getDepotData().setExternID("DEP")
		importer.getCustomerData().setExternID("C1").setDemand(1f)
		importer.getVehicleData().setName("TRAILER1").setAutonomous(false).setCapacity([5] as float[])
		importer.getVehicleData().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10] as float[])
		importer.finishImport()

		when:
		def vehicles = importer.getVehicles()

		then:
		vehicles[1].getTrailerId() == 0
		vehicles[1].getCapacity()[0] == 15f
	}

	def "Lorry without trailer keeps its own capacity"() {
		given:
		def importer = new FlexiImporter()
		importer.getDepotData().setExternID("DEP")
		importer.getCustomerData().setExternID("C1").setDemand(1f)
		importer.getVehicleData().setName("TRAILER1").setAutonomous(false).setCapacity([5] as float[])
		importer.getVehicleData().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10] as float[])
		importer.getVehicleData().setName("LORRY2").setCapacity([10] as float[])
		importer.finishImport()

		when:
		def vehicles = importer.getVehicles()

		then:
		vehicles[2].getTrailerId() == Vehicle.NO_TRAILER
		vehicles[2].getCapacity()[0] == 10f
	}

	def "Train capacity is additive per compartment"() {
		given:
		def importer = new FlexiImporter()
		importer.getDepotData().setExternID("DEP")
		importer.getCustomerData().setExternID("C1").setDemand(1f)
		importer.getVehicleData().setName("TRAILER1").setAutonomous(false).setCapacity([5, 20] as float[])
		importer.getVehicleData().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10, 100] as float[])
		importer.finishImport()

		when:
		def vehicles = importer.getVehicles()

		then:
		vehicles[1].getCapacity()[0] == 15f
		vehicles[1].getCapacity()[1] == 120f
	}

	def "Trailer capacity adds to train capacity also for multiple compartments of different size"() {
		given:
		def importer = new FlexiImporter()
		importer.getDepotData().setExternID("DEP")
		importer.getCustomerData().setExternID("C1").setDemand(1f)
		// Trailer has one compartment less than the lorry (padded with unlimited)
		importer.getVehicleData().setName("TRAILER1").setAutonomous(false).setCapacity([7] as float[])
		importer.getVehicleData().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10, 100] as float[])
		importer.finishImport()

		when:
		def vehicles = importer.getVehicles()

		then:
		vehicles[1].getCapacity()[0] == 17f
		// Compartments, which the trailer does not declare, contribute nothing
		vehicles[1].getCapacity()[1] == 100f
	}

	def "Trailers are not planned autonomously and keep their own capacity"() {
		given:
		def importer = new FlexiImporter()
		importer.getDepotData().setExternID("DEP")
		importer.getCustomerData().setExternID("C1").setDemand(1f)
		importer.getVehicleData().setName("TRAILER1").setAutonomous(false).setCapacity([5] as float[])
		importer.getVehicleData().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10] as float[])
		importer.finishImport()

		when:
		def vehicles = importer.getVehicles()

		then:
		!vehicles[0].isAutonomous()
		vehicles[0].getCapacity()[0] == 5f
	}

	def "Invalid coupling is still rejected"() {
		given:
		def importer = new FlexiImporter()
		importer.getDepotData().setExternID("DEP")
		importer.getCustomerData().setExternID("C1").setDemand(1f)
		importer.getVehicleData().setName("LORRY1").setTrailerId("UNKNOWN").setCapacity([10] as float[])
		importer.finishImport()

		when:
		importer.getVehicles()

		then:
		thrown(XFVRPException)
	}
}
