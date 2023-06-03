[![MIT License](https://img.shields.io/apm/l/atomic-design-ui.svg?)](https://github.com/tterb/atomic-design-ui/blob/master/LICENSEs)
[![BCH compliance](https://bettercodehub.com/edge/badge/hschneid/xfvrp?branch=master)](https://bettercodehub.com/)
![alt text](https://img.shields.io/static/v1?label=version&message=11.4.6&color=-)

xfvrp
======

There are a lot of solvers for the Vehicle Routing Problem (VRP) on github. Some are good at certain features (like CVRP or VRPTW) and some are quite complex.

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
- and many more.
 
Additional requirements are the usage and maintainability of the API like
- User shall change only the necessary values. The rest is done by default values.
- The user API shall be as easy as possible to understand. Users of xfvrp need only to know one class as interface.
- No parameter tuning (i.e. mutation rate, population size, annealing temperature) 
- No free lunch: Good results with good performance.

## License
This software is licenced under [MIT License] (https://opensource.org/licenses/MIT).

## Getting started
* Add dependency to your project
  * Maven: 
    ```
    <dependency>
      <groupId>com.github.hschneid</groupId>
      <artifactId>xfvrp</artifactId>
      <version>11.4.6-RELEASE</version>
    </dependency>
    ```
  * Gradle:
    ```
    implementation 'com.github.hschneid:xfvrp:11.4.6-RELEASE'
    ```

A simple example for a capacitated vehicle route planning:
``` 
XFVRP xfvrp = new XFVRP();
xfvrp.addDepot().setXlong(5.667);
xfvrp.addCustomer().setXlong(1.002).setDemand(new float[]{1.5, 0, 2.3});
xfvrp.setVehicle().setName("Truck").setCapacity(new float[]{3, 2, 5});
xfvrp.addCompartment(CompartmentType.DELIVERY)
xfvrp.setMetric(new EucledianMetric());
xfvrp.setOptType(XFVRPOptTypes.RELOCATE);

Report report = xfvrp.executeRoutePlanning();
report.getSummary().getDistance();
```

## Benchmarks
As a general purpose solver, XFVRP is not fully compatable with single problem solvers. But even though it can prove its relevance by [benchmarks](BENCHMARKS.md).

## Change log

### 11.4.6
- New constraint: Max (preferred) number of routes per depot.
  - Optimization gets the info, what should be the acceptable number of routes per depot. As XFVRP cannot work with invalid routes, the optimization is guided to solutions with valid number of routes.
    ```
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
- Refactoring of First Best heuristic &#8594; 3 to 10 times faster for bigger instances
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
  ```
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
