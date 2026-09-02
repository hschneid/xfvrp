[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Version](https://img.shields.io/static/v1?label=version&message=11.7.0&color=-)
![Java](https://img.shields.io/badge/Java-21-blue)

# xfvrp

> **eXtreme Fast Vehicle Routing Problem** – A fast and easy-to-use solver for Rich Vehicle Routing Problems in Java.

There are a lot of solvers for the Vehicle Routing Problem (VRP) on GitHub. Some are good at certain features (like CVRP or VRPTW) and some are quite complex.

xfvrp is a fast and easy-to-use solver for Rich Vehicle Routing Problems like
- Multiple compartments
  - Separate capacities for routes with only pickup, only delivery or mixed pickup and delivery
- Multiple time windows
- Multiple depots
- Heterogeneous fleet
- Pick and delivery or backhauls
- Replenishment sites
- State of the art optimization heuristics
- Presettings (i.e. packages must be loaded together or not)
- Open route planning (start and/or end)
- Predefined initial solutions
- and many more.

Additional requirements are the usage and maintainability of the API like
- User shall change only the necessary values. The rest is done by default values.
- The user API shall be as easy as possible to understand. Users of xfvrp need only to know one class as interface.
- No parameter tuning (i.e. mutation rate, population size, annealing temperature)
- No free lunch: Good results with good performance.

---

## Table of Contents
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Usage Examples](#usage-examples)
- [Optimization Types](#optimization-types)
- [API Reference](#api-reference)
- [Benchmarks](#benchmarks)
- [Contributing](#contributing)
- [License](#license)
- [Change Log](#change-log)

---

## Prerequisites

- **Java 21** or higher
- **Gradle 8+** (for building from source)

## Getting Started

### Add dependency to your project

**Maven:**
```xml
<dependency>
  <groupId>com.github.hschneid</groupId>
  <artifactId>xfvrp</artifactId>
  <version>11.6.1-RELEASE</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'com.github.hschneid:xfvrp:11.6.1-RELEASE'
```

### Build from source
```bash
git clone https://github.com/hschneid/xfvrp.git
cd xfvrp
./gradlew build
```

---

## Usage Examples

### Capacitated VRP (CVRP)

A simple example for a capacitated vehicle route planning:

```java
XFVRP xfvrp = new XFVRP();
xfvrp.addDepot().setExternID("DEP1").setXlong(5.667).setYlat(51.23);
xfvrp.addCustomer().setExternID("C1").setXlong(1.002).setYlat(50.12).setDemand(new float[]{1.5, 0, 2.3});
xfvrp.addCustomer().setExternID("C2").setXlong(2.345).setYlat(50.98).setDemand(new float[]{2.0, 1, 1.0});
xfvrp.addVehicle().setName("Truck").setCapacity(new float[]{3, 2, 5});
xfvrp.addCompartment(CompartmentType.DELIVERY);
xfvrp.setMetric(new EucledianMetric());
xfvrp.addOptType(XFVRPOptTypes.RELOCATE);

xfvrp.executeRoutePlanning();

Report report = xfvrp.getReport();
report.getSummary().getDistance();
report.getSummary().getNbrOfUsedVehicles();
report.getSummary().getDuration();
```

### VRP with Time Windows (VRPTW)

```java
XFVRP xfvrp = new XFVRP();
xfvrp.addDepot().setExternID("DEP1").setXlong(5.667).setYlat(51.23)
     .setTimeWindow(0, 480); // Depot open 0-480 min

xfvrp.addCustomer().setExternID("C1").setXlong(1.002).setYlat(50.12)
     .setDemand(new float[]{1.5f})
     .setTimeWindow(60, 120);  // Customer available 60-120 min

xfvrp.addVehicle().setName("Truck").setCapacity(new float[]{10});
xfvrp.addCompartment(CompartmentType.DELIVERY);
xfvrp.setMetric(new EucledianMetric());
xfvrp.addOptType(XFVRPOptTypes.ILS);

xfvrp.executeRoutePlanning();
Report report = xfvrp.getReport();
```

### Pickup and Delivery (PDP)

```java
XFVRP xfvrp = new XFVRP();
xfvrp.addDepot().setExternID("DEP1").setXlong(5.667).setYlat(51.23);

xfvrp.addCustomer().setExternID("P1").setXlong(1.0).setYlat(50.0)
     .setDemand(new float[]{1}).setLoadType(LoadType.PICKUP)
     .setShipID("SHIP1");
xfvrp.addCustomer().setExternID("D1").setXlong(2.0).setYlat(51.0)
     .setDemand(new float[]{1}).setLoadType(LoadType.DELIVERY)
     .setShipID("SHIP1");

xfvrp.addVehicle().setName("Van").setCapacity(new float[]{5});
xfvrp.addCompartment(CompartmentType.MIXED);
xfvrp.setMetric(new EucledianMetric());
xfvrp.getParameters().setWithPDP(true);
xfvrp.addOptType(XFVRPOptTypes.PDP_CHEAPEST_INSERT);
xfvrp.addOptType(XFVRPOptTypes.PDP_ILS);

xfvrp.executeRoutePlanning();
Report report = xfvrp.getReport();
```

### Multi-Depot VRP

```java
XFVRP xfvrp = new XFVRP();
xfvrp.addDepot().setExternID("DEP_NORTH").setXlong(5.0).setYlat(52.0).setMaxNbrRoutes(3);
xfvrp.addDepot().setExternID("DEP_SOUTH").setXlong(5.0).setYlat(50.0).setMaxNbrRoutes(3);

// Add customers...
xfvrp.addVehicle().setName("Truck").setCapacity(new float[]{10});
xfvrp.addCompartment(CompartmentType.DELIVERY);
xfvrp.setMetric(new EucledianMetric());
xfvrp.addOptType(XFVRPOptTypes.ILS);

xfvrp.executeRoutePlanning();
```

---

## Optimization Types

xfvrp provides a variety of optimization algorithms via `XFVRPOptTypes`:

| Type | Description | Use case |
|------|-------------|----------|
| `SAVINGS` | Clarke-Wright Savings heuristic | Construction |
| `CONST` | Christofides construction heuristic | Construction |
| `FIRST_BEST` | First-Best insertion heuristic | Construction |
| `RANDOM` | Randomized construction heuristic | Multi-start base |
| `RELOCATE` | Single node move (inter/intra-route) | Improvement |
| `PATH_RELOCATE` | Segment move | Improvement |
| `SWAP` | Single node swap | Improvement |
| `SWAPSEGMENT` | Segment swap | Improvement |
| `PATH_EXCHANGE` | Segment exchange (swap & move) | Improvement |
| `BORDER_PATH_EXCHANGE` | Border segment exchange | Improvement |
| `ILS` | Iterated Local Search | Meta-heuristic |
| `PDP_CHEAPEST_INSERT` | Cheapest insertion for PDP | PDP Construction |
| `PDP_RELOCATE2` | Single move for PDP | PDP Improvement |
| `PDP_ILS` | Iterated Local Search for PDP | PDP Meta-heuristic |

Multiple optimization types can be chained:
```java
xfvrp.addOptType(XFVRPOptTypes.SAVINGS);      // Initial construction
xfvrp.addOptType(XFVRPOptTypes.RELOCATE);      // Improve by moves
xfvrp.addOptType(XFVRPOptTypes.SWAP);          // Improve by swaps
xfvrp.addOptType(XFVRPOptTypes.ILS);           // Final meta-heuristic
```

---

## API Reference

### Main Class: `XFVRP`

| Method | Description |
|--------|-------------|
| `addDepot()` | Add a depot and return a `DepotData` object for configuration |
| `addCustomer()` | Add a customer and return a `CustomerData` object for configuration |
| `addReplenishment()` | Add a replenishment site |
| `addVehicle()` | Add a vehicle type and return a `VehicleData` object for configuration |
| `addCompartment(CompartmentType)` | Add a compartment type (`DELIVERY`, `PICKUP`, `MIXED`, ...) |
| `setMetric(Metric)` | Set distance/time metric (e.g. `EucledianMetric`) |
| `addOptType(XFVRPOptType)` | Add an optimization algorithm |
| `clearOptTypes()` | Remove all added optimization algorithms |
| `executeRoutePlanning()` | Execute the route planning |
| `getReport()` | Get the planning result as `Report` |
| `getParameters()` | Access `XFVRPParameter` for advanced settings |

### Compartment Types

| Type | Description |
|------|-------------|
| `DELIVERY` | Only delivery allowed |
| `PICKUP` | Only pickup allowed |
| `MIXED` | Mixed pickup and delivery |
| `DELIVERY_NO_REPLENISH` | Delivery without replenishment reset |
| `PICKUP_NO_REPLENISH` | Pickup without replenishment reset |
| `MIXED_NO_REPLENISH` | Mixed without replenishment reset |

### Parameters (`XFVRPParameter`)

| Method | Default | Description |
|--------|---------|-------------|
| `setRouteSplitting(boolean)` | `false` | Allow splitting of large solutions for faster optimization |
| `setOpenRouteAtStart(boolean)` | `false` | Open route planning (no return to start depot) |
| `setOpenRouteAtEnd(boolean)` | `false` | Open route planning (no return to end depot) |
| `setLoadingTimeAtDepot(boolean)` | `false` | Consider loading time at depot in time window planning |
| `setUnloadingTimeAtDepot(boolean)` | `false` | Consider unloading time at depot |
| `setNbrOfILSLoops(int)` | `50` | Number of ILS iterations |
| `setMaxRunningTimeInSec(long)` | unlimited | Maximum running time for optimization |
| `setWithPDP(boolean)` | `false` | Enable Pickup-and-Delivery mode |
| `setPredefinedSolutionString(String)` | `null` | Predefined initial solution `{(id,id,...),(id,...)}` |
| `setMixedFleetHeuristic(IMixedFleetHeuristic)` | default | Custom mixed fleet heuristic |

### Report

```java
Report report = xfvrp.getReport();
ReportSummary summary = report.getSummary();

summary.getDistance();          // Total distance
summary.getDuration();          // Total duration
summary.getNbrOfUsedVehicles(); // Number of vehicles used
summary.getDelay();             // Total delay (time window violations)
summary.getOverloads();         // Total overloads (capacity violations)
summary.getCost();              // Total cost
summary.getWaitingTime();       // Total waiting time
```

---

## Benchmarks

As a general purpose solver, XFVRP is not fully compatible with single problem solvers. But even though it can prove its relevance by [benchmarks](BENCHMARKS.md).

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m 'Add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

---

## License

This software is licensed under the [MIT License](https://opensource.org/licenses/MIT).

Copyright (c) 2012-2026 Holger Schneider

---

## Change Log

### 11.7.0
- Reduction of duplicate checks during neighborhood search with a cache. This improves speed by ca. 25%.

### 11.6.1
- Latest release
- Bug fixes and stability improvements

### 11.6.0
- Update to Java 21
- Update of Gradle
- Update of other dependencies
- Update of Copyright
- Changed publish mechanism

### 11.5.0
- Removed giant route based features completely (2-Opt, 3-Opt, ...). This reduces the complexity of code drastically.
- Randomized solution builder as base for multi-start-optimizations. It is realized as a randomized First-First construction heuristic.
- New Border-Segment-Exchange operator. Strong operator with neighborhood complexity O(n²)
- Licence update (year 2023)

### 11.4.6
- New constraint: Max (preferred) number of routes per depot.
  - Optimization gets the info, what should be the acceptable number of routes per depot. As XFVRP cannot work with invalid routes, the optimization is guided to solutions with valid number of routes.
    ```java
    xfvrp.addDepot()
         .setExternID(depotId)
         .setXlong(xlong)
         .setYlat(ylat)
         .setMaxNbrRoutes(nbrOfMaxRoutesAtThisDepot) 
    ```
  - Disclaimer:
    - This is no limit for number of routes at all depots. For multi-depot instances, this constraint is not tight.
    - Even with given constraint, a solution may have more routes than accepted. In these cases, the optimization routes were not strong enough to detect a solution with appropriate number.
- Refactoring of preset solution builder with lots of bugfixes
- Refactoring of First Best heuristic → 3 to 10 times faster for bigger instances
- Refactoring of reverse operations to reduce change-operator complexity
- Added Solomon (VRPTW) and Christofides (CVRP) instances for benchmarking
- Licence update (year 2022)
- Updated libs for security fixes

### 11.4.5
#### Breaking Changes
- Renamed XFVRPOptType >> XFVRPOptTypes
  - Changed optimization types from enum to simple list. With this, it is possible to inject own optimization logic into XFVRP.

#### Changes
- Introduced compartments as explicit resource. User can control the way, how demands are checked for capacity constraint. Default compartment is the mixed pickup and delivery.
  ```java
  xfvrp.addCompartment(CompartmentType.PICKUP);
  xfvrp.addVehicle().setCapacity(new float[]{50, 5, 10});
  xfvrp.addCustomer().setLoadType(LoadType.PICKUP).setDemand(new float[]{13, 4}); 
  ```
  In example, a compartment is declared, where only pickups shall happen. But the vehicle capacity is declared with 3 compartments, so 2 additional compartments are added with default compartment.
  The demand of the customer has only 2 compartments declared, which means, that third compartment is filled with default value = 0.
- Reverted some of the changes for compartments from 11.4.0 due to many side-effects. If someone needs this feature, please ping us.
- More refactorings due to giant route
- Fixed, that construction heuristic does not consider allowed-depots constraint correctly
- Fixed, that evaluation was not considering disallowed replenishment correctly

### 11.4.4.1
- Introduced new Mixed Fleet Heuristic
- Several Refactorings done by Fraunhofer IML

### 11.4.2 - 11.4.4
- Fixed irregular behaviour, when node.externId is not unique. Precheck method checks this now.
- Fixed error when checking vehicle types in blocks
- Fixed error when no nodes is allowed for a certain vehicle type. Then input must be corrected by caller.

### 11.4.1
- Fixed in report the summary per vehicle type, so that it considers multi compartments correctly as well.
- Fixed error, when customer demands and vehicle capacity have a different number of compartments

### 11.4.0
- Add more multi-compartment constraints
  - Considering more than 3 compartments (no limitation)
  - Vehicle capacity can be defined separately per compartment for PICKUP, DELIVERY or MIXED.
  - Replenishment nodes must not replenish (reset capacity) for every compartment.
- Add instance checks for MDVRPTW (of Vidal et al.)
- Internal restructuring to reduce complexity for data-import classes

### 11.3.0
- Drastically increased performance for some neighborhood searches by changing to route-based change operators and improved result sorting
  - Single node Move and Segment Move
  - Single node Swap and Segment Swap
  - Segment Exchange (Swap & move)
- Removed obsolete giant-route based neighborhoods, which are replaced by route-based neighborhoods

### 11.2.0
- Changed exception handling
  - No Runtime Exceptions (like IllegalState) anymore
- Added benchmark section in Readme
- Added benchmark for VRPTW with large instances to repo (more will be added in future)
- Minor cleanups in code
- First route-based neighborhood search
  - Increased performance by ca. 34% (tested on 60 large VRPTW instances)

### 11.1.0
- Cleanup of repository
- Add ReportBuilder (it compiles again :-) )
- Update of gradle 6.7 and Java 11
- Added SpotBug
- Cleaned obsolete code

### 11.0.0
- Automatical test with 70% code coverage of lines
- Removed unusual features (i.e. XFLP)
- A lot of code rework as preparation of further improvements
- Solved a lot of bugs, due to testing ;-)

