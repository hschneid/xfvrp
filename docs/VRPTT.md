# VRPTT — Vehicle Routing Problem with Trailers and Transshipments

XFVRP supports planning with **lorries and trailers**, **transshipment locations**
and **customer accessibility** constraints. This guide shows the model, the API
and a complete example case. For the algorithmic design (decomposition approach,
synchronization checks, benchmark results) see [VRPTT-SPEC.md](VRPTT-SPEC.md).

---

## 1. The model

### Vehicles: lorries and trailers

| Vehicle kind | Declaration | Meaning |
|---|---|---|
| **Lorry** | `setAutonomous(true)` (default) | Moves on its own. May pull a trailer. |
| **Trailer** | `setAutonomous(false)` | Cannot move on its own; must be coupled to a lorry. |
| **Coupling** | `setTrailerId("<trailer name>")` on the lorry | The lorry pulls exactly this trailer. |

A coupled lorry–trailer combination has the **additive capacity** of both
vehicles (per compartment). While the trailer is not coupled, it does not
consume costs and does not move.

### Customers: lorry vs. trailer accessible

- **Trailer customers** (default, `setTrailerAccessible(true)`) can be served by
  any vehicle — with or without a coupled trailer.
- **Lorry customers** (`setTrailerAccessible(false)`) must be served by a lorry
  *without* trailer: the trailer would have to be parked somewhere, which is not
  modeled. A customer that no vehicle may serve is rejected as illegal input.

### Transshipment locations (TLs)

A transshipment location is a site where

- load can be transferred between vehicles (a lorry drops amounts for another
  vehicle that picks them up there), and
- load can change the service mode of a customer: instead of driving a far-away
  trailer customer directly, a lorry brings the load to a nearby TL and a second
  vehicle completes the delivery.

TLs may have a **time window** (the transfer must finish before it closes) and a
**fixed transfer time** that is consumed by the transferring vehicle.

### How the optimizer decides

For every trailer-accessible customer, the optimizer chooses between the two
service modes:

```
DIRECT(c)        the vehicle serves the customer directly
VIA_TDP(c, t)    a supplier vehicle drops the load at TL t,
                 another vehicle picks it up there and serves c
```

The decision is made by a construction heuristic and improved by an
iterated local search (ILS), including mode changes and TL re-assignments.
Transfers between *different* vehicles are represented as **transshipment
records**: a pair of visits at the same TL where the dropped amount on one side
exactly matches the picked-up amount on the other side. The evaluation checks
every record for

- **load consistency** (amount out = amount in, both vehicles stay within their capacity), and
- **temporal consistency** (both vehicles are at the TL within the transfer duration and the TL time window).

Solutions that violate these constraints get a penalty, so the optimization
converges to synchronization-feasible plans.

---

## 2. Example case

### Scenario

A depot ships to two customers on a long corridor:

```
DEP(0) ---- T1(1) ------------------- C1(10) ---- C2(11)
            TL                        trailer
                                     customer    lorry
                                                 customer
```

- `C1` is a trailer customer far away from the depot: serving it directly costs
  2 × 10 = 20 distance units. Serving it *via* the nearby TL `T1` costs
  1 (drop) + 1 (pick up and serve) + 10 (return of the supplier route) — with a
  suitable pairing the transshipment is clearly cheaper.
- `C2` is a lorry customer: it must be served by a lorry **without** trailer.
- Fleet: one trailer and one lorry that pulls it when coupled.

### Code

```java
XFVRP xfvrp = new XFVRP();

// Depot, transshipment location and customers
// (index = declaration order: 0 = DEP, 1 = T1, 2 = C1, 3 = C2)
xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0);
xfvrp.addTransshipment()
        .setExternID("T1")
        .setXlong(1).setYlat(0)
        .setTimeWindow(0, 100)     // optional: transfers must finish before 100
        .setFixedTransferTime(5);  // optional: transfer takes 5 time units
xfvrp.addCustomer().setExternID("C1").setXlong(10).setYlat(0).setDemand(3f);
xfvrp.addCustomer().setExternID("C2").setXlong(11).setYlat(0).setDemand(1f)
        .setTrailerAccessible(false);  // lorry customer

// Fleet: one trailer and one pulling lorry (additive capacity 5 + 10)
xfvrp.addVehicle().setName("TRAILER1").setAutonomous(false).setCapacity(new float[]{5});
xfvrp.addVehicle().setName("LORRY1").setTrailerId("TRAILER1").setCapacity(new float[]{10});

// Distances = times, default 10, along the corridor: DEP-T1 = 1, T1-C1 = 1
DirectMatrixMetric metric = new DirectMatrixMetric(4, 1);
for (int i = 0; i < 4; i++)
    for (int j = 0; j < 4; j++)
        metric.add(i, j, 10f, 10f);
metric.add(0, 1, 1f, 1f); metric.add(1, 0, 1f, 1f);
metric.add(1, 2, 1f, 1f); metric.add(2, 1, 1f, 1f);
metric.add(2, 3, 1f, 1f); metric.add(3, 2, 1f, 1f);
xfvrp.setMetric(metric);

// Optimization
xfvrp.addOptType(XFVRPOptTypes.VRPTT);      // construction
xfvrp.addOptType(XFVRPOptTypes.VRPTT_ILS);  // improvement (recommended)

xfvrp.executeRoutePlanning();
Report report = xfvrp.getReport();
```

With these distances, serving `C1` directly is a round trip of 20, while the
transshipment over `T1` (drop: 1, pick up and serve: 1 + 10) is cheaper — the
optimizer picks `VIA_TDP(C1, T1)`.

### What the plan looks like

The optimizer compares the service modes and produces a plan where the
trailer-accessible customer is served via the TL:

```
LORRY1:   DEP → T1(drop 3) → ... → DEP     brings the load to the TL
take:     DEP → T1(take 3) → C1 → DEP      completes the delivery from the TL
LORRY1:   DEP → C2 → DEP                   lorry customer, no trailer allowed
```

(The concrete vehicle assignment depends on the instance; with more lorries the
drop and take can be done by different vehicles.)

In the report, transshipment visits appear as events with site type
`TRANSSHIPMENT`:

```java
for (Route route : report.getRoutes()) {
    for (Event event : route.getEvents()) {
        if (event.getSiteType() == SiteType.TRANSSHIPMENT) {
            // event.getID()        e.g. "T1"
            // event.getArrival()   arrival time (must respect the TL time window)
            // event.getService()   consumed transfer time
            // event.getDeparture() arrival + service
        }
    }
}
```

The `SynchronizationAnalyzer` guarantees that the drop and take amounts match
and that both vehicles are at `T1` within the transfer duration; the TL time
window `setTimeWindow(0, 100)` is respected by both visits.

### When does the transshipment pay off?

`VIA_TDP` is chosen only when it is *cheaper* than `DIRECT`. That is typically
the case when

- the customer is far from the depot but near a TL,
- the direct way to the customer is inaccessible to the pulling lorry (e.g.
  weight-restricted roads, modeled by the metric), or
- a second vehicle is already nearby, so the pickup detour is small.

Otherwise the optimizer keeps the customer on `DIRECT` mode — no needless TL
detours.

---

## 3. API summary

| Element | API |
|---|---|
| Transshipment location | `xfvrp.addTransshipment().setExternID(...)`, `.setXlong/setYlat`, `.setTimeWindow(open, close)`, `.setFixedTransferTime(seconds)`, `.setParkingAllowed(boolean)` |
| Lorry customer | `customer.setTrailerAccessible(false)` |
| Trailer | `vehicle.setAutonomous(false)` |
| Coupling | `lorry.setTrailerId("<trailer name>")` |
| Optimization | `xfvrp.addOptType(XFVRPOptTypes.VRPTT)` and `VRPTT_ILS` |
| Report | `SiteType.TRANSSHIPMENT` events with arrival/service/departure times |

Notes:

- Coupling is fixed per planning run: a trailer keeps its declared pulling
  lorry (TTRP-style semantics). Changing the pulling lorry at a TL is not
  supported.
- `setParkingAllowed` is currently declared but inert — trailer drop-and-pickup
  pairs (park a loaded trailer at the TL and fetch it later) are not modeled.
- When a heterogeneous fleet is planned, the VRPTT machinery activates
  automatically as soon as transshipment locations are present; existing
  optimization types behave unchanged.

---

## 4. Benchmark

The implementation is validated on 109 synthetic benchmark instances following
the recipe of Drexl's VRPTT study (the original instances are not publicly
available; the generator and a README live in
`src/test/resources/vrptt/`). Run them with:

```
mvn test -P benchmarks
```

Across the three implementation phases the total cost on these instances
improved from 93,325 (direct service only) to 76,833 (with coupled capacity and
inter-vehicle transshipment records) — see [VRPTT-SPEC.md](VRPTT-SPEC.md), §6,
for the full per-class results.
