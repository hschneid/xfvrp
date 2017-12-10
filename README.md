xfvrp
======

There are alot of solvers for the Vehicle Routing Problem (VRP) on github. Some are good at certain features (like CVRP or VRPTW) and some are quite complex.

xfvrp is a fast and easy solver for Rich Vehicle Routing Problems like
- Multi capacities
- Multi time windows
- Multi depots
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
This software is released under [MIT License] (https://opensource.org/licenses/MIT)

## Getting started
Load xfvrp from github, append it to your project and use the API.

A simple example for a capacitated vehicle route planning:
``` XFVRP xfvrp = new XFVRP();
xfvrp.addDepot().setXlong(5.667);
xfvrp.addCustomer().setXlong(1.002).setDemand(new float[]{1.5, 0, 2.3});
xfvrp.setVehicle().setCapacity(new float[]{3, 2, 5});
xfvrp.setMetric(new EucledianMetric());
xfvrp.setOptType(XFVRPOptType.RELOCATE);

Report report = xfvrp.executeRoutePlanning();
report.getSummary().getDistance();
```

## Release nodes
Version 11:
- Automatical test with 70% code coverage of lines
- Removed unusual features (i.e. XFLP)
- A lot of code rework as preparation of further improvements
- Solved a lot of bugs, due to testing ;-)
