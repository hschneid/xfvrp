package instances

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptTypes
import xf.xfvrp.opt.fleetmix.VRPTTMixedFleetHeuristic
import xf.xfvrp.report.Report
import xf.xfvrp.report.RouteReport

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Benchmark spec for the Vehicle Routing Problem with Trailers and
 * Transshipments (VRPTT) on the synthetic Drexl-style instances in
 * ./src/test/resources/vrptt/.
 * <p>
 * The original instances of Michael Drexl (raw milk collection, 109 instances
 * in the classes 2 2 4 - 8 8 8) were never published for download. The
 * instances in the resources folder are synthetic and were generated faithfully
 * to the recipe in Drexl (2012), LM-2012-1210, section 6.1 (see README.md
 * there). Because no best-known or optimal values exist for synthetic
 * instances, this spec reports absolute costs and checks feasibility: all
 * customers must be served and no synchronization, overload or input errors
 * may remain.
 * <p>
 * Runs only under the benchmarks profile (mvn test -P benchmarks).
 *
 * @author lhackste
 */
class VRPTTDrexl extends Specification {

	def "run synthetic Drexl VRPTT benchmark instances"() {
		when:
		execute()

		then:
		assert true
	}

	private void execute() {
		def files = Files
				.list(Path.of("./src/test/resources/vrptt/"))
				.filter({ f -> f.fileName.toString().endsWith(".txt") })
				.sorted({ a, b -> a.fileName <=> b.fileName })
				.collect(Collectors.toList())

		def infeasible = []
		float totalCost = 0
		long totalTime = 0
		int totalTransshipments = 0
		int totalMeaningfulTransshipments = 0
		int totalInterVehicleRecords = 0

		for (Path p : files) {
			def instance = parse(p)

			long time = System.currentTimeMillis()
			try {
				def report = executeInstance(instance)
				def runtime = System.currentTimeMillis() - time
				totalTime += runtime

				def cost = report.getSummary().getCost()
				def nbrOfTransshipments = countTransshipments(report)
				def nbrOfMeaningfulTransshipments = countMeaningfulTransshipments(report)
				def nbrOfInterVehicleRecords = countInterVehicleRecords(report)
				totalTransshipments += nbrOfTransshipments
				totalMeaningfulTransshipments += nbrOfMeaningfulTransshipments
				totalInterVehicleRecords += nbrOfInterVehicleRecords

				boolean feasible = report.getErrors().getErrorDescriptions().isEmpty() &&
						servedCustomerIDs(report) == instance.customerIDs &&
						report.getSummary().getOverload() == 0f &&
						noSyncViolation(report)

				if (feasible) {
					totalCost += cost
				} else {
					infeasible.add(instance.name)
				}

				println instance.name + " " +
						String.format("%.2f", cost) + " " +
						report.getSummary().getNbrOfUsedVehicles() + " " +
						String.format("%.2f", runtime / 1000.0) + " " +
						nbrOfTransshipments + " " +
						nbrOfMeaningfulTransshipments + " " +
						nbrOfInterVehicleRecords +
						(feasible ? "" : " INFEASIBLE")

			} catch (Exception e) {
				infeasible.add(instance.name + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")")
			}
		}

		println "VRPTT (synthetic Drexl instances): " +
				(files.size() - infeasible.size()) + "/" + files.size() + " feasible, " +
				String.format("%.2f", totalCost) + " total cost, " +
				String.format("%.2f", totalTime / 1000.0) + " s total time, " +
				totalTransshipments + " transshipment visits (" +
				totalMeaningfulTransshipments + " meaningful records, " +
				totalInterVehicleRecords + " inter-vehicle records)"

		assert infeasible.isEmpty() : "Infeasible instances: " + infeasible
	}

	/**
	 * Checks the structural synchronization rule of the SynchronizationAnalyzer:
	 * each pass-through visit of a transshipment location (without transferred
	 * amount) has to be followed by at least one customer visit, so that the
	 * load, which is brought to the transshipment location, is taken from it.
	 * Record visits (drop and take of a transferred amount) are checked by the
	 * analyzer (load, temporal and matching consistency) and are exempt.
	 */
	private boolean noSyncViolation(Report report) {
		return report.getRoutes().stream().allMatch({ route ->
			boolean openTransshipment = false
			for (def event : route.getEvents()) {
				if (event.getSiteType() == SiteType.TRANSSHIPMENT) {
					def amounts = event.getAmounts()
					openTransshipment = amounts.length == 0 || Math.abs(amounts[0]) < 0.0001f
				} else if (event.getSiteType() == SiteType.CUSTOMER)
					openTransshipment = false
			}
			return !openTransshipment
		})
	}

	private Report executeInstance(def instance) {
		def xfvrp = new XFVRP()

		xfvrp.addDepot()
				.setExternID(instance.depot.id)
				.setXlong(instance.depot.x)
				.setYlat(instance.depot.y)

		for (def tl : instance.tls)
			xfvrp.addTransshipment()
					.setExternID(tl.id)
					.setXlong(tl.x)
					.setYlat(tl.y)
					.setTimeWindow(0, instance.horizon)
					.setFixedTransferTime(tl.transfer)

		for (def cust : instance.customers)
			xfvrp.addCustomer()
					.setExternID(cust.id)
					.setXlong(cust.x)
					.setYlat(cust.y)
					.setDemand((float) cust.supply)
					.setServiceTime(cust.serviceTime)
					.setTrailerAccessible(cust.trailerAccessible)

		def lorryData = [:]
		for (def lorry : instance.lorries) {
			def veh = xfvrp.addVehicle()
					.setName(lorry.id)
					.setCapacity([lorry.capacity] as float[])
					.setFixCost(lorry.fixCost)
					.setVarCost(lorry.varCost)
			lorryData[lorry.id] = veh
		}

		for (def trailer : instance.trailers)
			xfvrp.addVehicle()
					.setName(trailer.id)
					.setAutonomous(false)
					.setCapacity([trailer.capacity] as float[])
					.setFixCost(trailer.fixCost)
					.setVarCost(trailer.varCost)

		// Bind the trailers to their declared pulling lorry
		for (def trailer : instance.trailers)
			lorryData[trailer.pullingLorry].setTrailerId(trailer.id)

		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.VRPTT)
		xfvrp.addOptType(XFVRPOptTypes.VRPTT_ILS)
		// Phase 3: joint multi-vehicle plan with inter-vehicle transshipment records
		xfvrp.getParameters().setMixedFleetHeuristic(new VRPTTMixedFleetHeuristic())

		xfvrp.getParameters().setNbrOfILSLoops(10)
		xfvrp.getParameters().setMaxRunningTimeInSec(30)

		xfvrp.executeRoutePlanning()
		return xfvrp.getReport()
	}

	private Set<String> servedCustomerIDs(Report report) {
		return report.getRoutes()
				.collectMany { it.getEvents() }
				.findAll { it.getSiteType() == SiteType.CUSTOMER }
				.collect { it.getID() }
				.toSet()
	}

	/**
	 * Counts the visits of transshipment locations in all routes of the
	 * report, i.e. the number of executed VIA_TDP service ways.
	 */
	private int countTransshipments(Report report) {
		return report.getRoutes()
				.collectMany { it.getEvents() }
				.count { it.getSiteType() == SiteType.TRANSSHIPMENT }
	}

	/**
	 * Counts the transshipment visits, which transfer load between two
	 * vehicles (transshipment records): visits of a transshipment location
	 * with a transferred amount. Visits without amount are pass-through ties
	 * of the Phase 1 semantics.
	 */
	private int countMeaningfulTransshipments(Report report) {
		return report.getRoutes()
				.collectMany { it.getEvents() }
				.count { it.getSiteType() == SiteType.TRANSSHIPMENT &&
						it.getAmounts().length > 0 &&
						Math.abs(it.getAmounts()[0]) > 0.0001f }
	}

	/**
	 * Counts the transshipment records, where the supplier and the taker are
	 * DIFFERENT vehicles (Phase 3): drop and take visits of the same TL and
	 * the same amount, whose routes are planned with different vehicles.
	 */
	private int countInterVehicleRecords(Report report) {
		def drops = []
		def takes = []
		report.getRoutes().each { route ->
			route.getEvents().each { e ->
				if (e.getSiteType() != SiteType.TRANSSHIPMENT || e.getAmounts().length == 0)
					return
				if (e.getAmounts()[0] < -0.0001f)
					drops << [id: e.getID(), amount: e.getAmounts()[0], vehicle: route.getVehicle().getName()]
				else if (e.getAmounts()[0] > 0.0001f)
					takes << [id: e.getID(), amount: e.getAmounts()[0], vehicle: route.getVehicle().getName()]
			}
		}

		int records = 0
		def used = [] as Set
		for (def d : drops) {
			def t = takes.find { !used.contains(it) &&
					it.id == d.id &&
					Math.abs(it.amount + d.amount) < 0.0001f &&
					it.vehicle != d.vehicle }
			if (t != null) {
				used.add(t)
				records++
			}
		}
		return records
	}

	/**
	 * Parses an instance file of the synthetic Drexl-style format (see README.md
	 * in the resources folder).
	 */
	private def parse(Path p) {
		def instance = [
				name       : p.fileName.toString().replace(".txt", ""),
				horizon    : 0,
				depot      : null,
				customers  : [],
				tls        : [],
				lorries    : [],
				trailers   : []
		]

		for (String line : Files.readAllLines(p)) {
			if (line.isBlank() || line.startsWith("#"))
				continue

			def t = line.trim().split("\\s+")
			switch (t[0]) {
				case "HORIZON":
					instance.horizon = Integer.parseInt(t[1])
					break
				case "DEPOT":
					instance.depot = [id: t[1], x: Float.parseFloat(t[2]), y: Float.parseFloat(t[3])]
					break
				case "CUSTOMER":
					instance.customers.add([
							id               : t[1],
							x                : Float.parseFloat(t[2]),
							y                : Float.parseFloat(t[3]),
							supply           : Integer.parseInt(t[4]),
							trailerAccessible: t[5] == "1",
							serviceTime      : Float.parseFloat(t[6])
					])
					break
				case "TL":
					instance.tls.add([
							id      : t[1],
							x       : Float.parseFloat(t[2]),
							y       : Float.parseFloat(t[3]),
							transfer: Float.parseFloat(t[4])
					])
					break
				case "LORRY":
					instance.lorries.add([
							id      : t[1],
							capacity: Float.parseFloat(t[2]),
							fixCost : Float.parseFloat(t[3]),
							varCost : Float.parseFloat(t[4])
					])
					break
				case "TRAILER":
					instance.trailers.add([
							id          : t[1],
							capacity    : Float.parseFloat(t[2]),
							fixCost     : Float.parseFloat(t[3]),
							varCost     : Float.parseFloat(t[4]),
							pullingLorry: t[5]
					])
					break
			}
		}

		instance.customerIDs = instance.customers.collect { it.id }.toSet()
		return instance
	}
}
