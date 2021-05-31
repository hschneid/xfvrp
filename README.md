[![MIT License](https://img.shields.io/apm/l/atomic-design-ui.svg?)](https://github.com/tterb/atomic-design-ui/blob/master/LICENSEs)
[![BCH compliance](https://bettercodehub.com/edge/badge/hschneid/xfvrp?branch=master)](https://bettercodehub.com/)
![alt text](https://img.shields.io/static/v1?label=version&message=11.4.2&color=-)

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
 
Additional requirements are the useage and maintainability of the API like
- User shall change only the necessary values. The rest is done by default values.
- The user API shall be as easy as possible to understand. Users of xfvrp need only to know one class.
- No parameter tuning (i.e. mutation rate, popultation size, annealing temperature) 
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
      <version>11.3.0-RELEASE</version>
    </dependency>
    ```
  * Gradle:
    ```
    implementation 'com.github.hschneid:xfvrp:11.3.0-RELEASE'
    ```

A simple example for a capacitated vehicle route planning:
``` 
XFVRP xfvrp = new XFVRP();
xfvrp.addDepot().setXlong(5.667);
xfvrp.addCustomer().setXlong(1.002).setDemand(new float[]{1.5, 0, 2.3});
xfvrp.setVehicle().setName("Truck").setCapacity(new float[]{3, 2, 5});
xfvrp.setMetric(new EucledianMetric());
xfvrp.setOptType(XFVRPOptType.RELOCATE);

Report report = xfvrp.executeRoutePlanning();
report.getSummary().getDistance();
```

## Benchmarks
As a general purpose solver, XFVRP is not fully compatable with single problem solvers. But even though it can prove its relevance by [benchmarks](BENCHMARKS.md).

## Change log

### 11.4.2
- Fixed irregular behaviour, when node.externId is not unique. Precheck method checks this now. 

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
