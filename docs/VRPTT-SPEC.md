# Spec: Vehicle Routing Problem with Trailers and Transshipments (VRPTT) in XFVRP

Status: Draft v0.1
Approach: **Option 1 — Decomposition** (task splitting into PDP subproblems, following
Drexl's generic heuristic for VRPs with multiple synchronization constraints, Mainz
tech report LM-2014-05), built on XFVRP's existing PDP, replenishment and
mixed-fleet infrastructure.

---

## 1. Problem definition

The VRPTT extends the classic VRP with the following elements:

### 1.1 Vehicles
- **Lorries** (autonomous): move in space on their own, may or may not pull a trailer.
- **Trailers** (non-autonomous): can wait in time, but only move in space while
  coupled to a compatible lorry. No fixed lorry–trailer assignment; coupling may
  change during the plan.
- Each lorry has a lorry capacity; each trailer an additional trailer capacity.
  A lorry without trailer has only its lorry capacity.

### 1.2 Sites
- **Depot(s)**: start and end of all routes.
- **Customers**, partitioned into
  - *lorry customers* — accessible only by a lorry without trailer,
  - *trailer customers* — accessible by lorry with or without trailer.
- **Transshipment locations (TLs)**, where
  - a trailer can be parked (trailer drop point), and
  - load can be transferred between vehicles.
  TLs may have time windows; transshipment time may depend on the transferred amount.

### 1.3 Synchronization constraints
1. **Spatial**: a trailer's movement between two locations must coincide with a
   lorry route segment that couples to it.
2. **Temporal**: vehicles involved in a transshipment visit the TL at the same time
   (within the transfer duration).
3. **Load**: the amount transferred is identical on both sides; vehicle capacity
   (lorry-only vs. lorry+trailer) must hold at all times.

### 1.4 Objective
Minimize total fixed and variable costs of all lorry and trailer movements.

### 1.5 Non-goals (v1)
- Continuous load variables with amount-dependent transfer times are rounded to
  fixed transfer durations (parameterized per TL).
- Multi-trailer (road trains), driver synchronization, and periodic variants are out
  of scope.
- No exact method; the goal is a feasible, cost-efficient heuristic.

---

## 2. Solution approach (Option 1: task decomposition)

The full VRPTT is **not** solved monolithically. Following the generic heuristic
for VRPs with multiple synchronization constraints, each customer request is
decomposed into one or more *ways of service*, each way consisting of ordered
pickup-and-delivery subtasks. The subtasks are planned with a
**PDPTW-with-transshipment** solver built from XFVRP's existing PDP machinery.

### 2.1 Step 1 — Service enumeration (task splitting)
For each trailer customer `c` and each accessible TL `t`, enumerate the possible
service modes:

| Mode | Description |
|------|-------------|
| `DIRECT(c)` | Lorry(+trailer) visits `c` directly. Only for trailer customers. |
| `VIA_TDP(c, t)` | Lorry brings load to TL `t` (with or without trailer); a truck serving `c` picks it up at `t`. Load transshipment at `t`. |

Output: a set of *derived tasks* (lorry tasks and trailer tasks), each of which is
a pickup-and-delivery task in the Drexl sense. Compatibility flags per task:
trailer accessibility of all visited customers, allowed vehicle set.

### 2.2 Step 2 — Subtask planning (PDPTW-T, autonomous fleet view)
Solve a pickup-and-delivery problem with time windows and transshipments over the
derived tasks:

- Fleet: all lorries **and** all trailers, where trailers are treated as autonomous
  vehicles with costs equal to a lorry–trailer combination (as in LM-2014-05), so
  that lorries are not unfairly favored.
- This step decides *which* TLs are used and which lorry serves which customers
  directly vs. via transshipment.
- Implementation: a new initial-solution builder + ILS in the spirit of
  `XFPDPFirstBestInsert` / `XFPDPILS`, operating on shipment pairs
  (pickup = origin TL or depot, delivery = destination TL or customer).

### 2.3 Step 3 — Segment planning (coupling & synchronization)
The step-2 solution induces *route segments* between consecutive transshipment
events (depot or TL). Each segment must be assigned to a concrete lorry such that:

- the segment is driveable by the assigned lorry (trailer accessibility of all
  visited customers, arc-based accessibility is left to the metric),
- coupling events are temporally consistent (a trailer cannot be in two places),
- load transferred at a TL matches on both sides.

This is again a PDP-like assignment problem (each segment = pickup at its start,
delivery at its end) and is solved with the same PDP machinery, using the lorry
fleet only.

### 2.4 Improvement phase
On the combined plan, run route-based neighborhood moves restricted to
synchronization-safe modifications:

- Segment move/swap *within* one vehicle's route (reuse existing
  `XFVRPSingleMove` / `XFVRPSingleSwap` unchanged),
- Mode change for a customer (`DIRECT(c)` ↔ `VIA_TDP(c, t)`),
- TL re-assignment for a customer,
- Coupling-aware ILS: only accept solutions that pass the synchronization
  feasibility check (see §3).

---

## 3. Feasibility checking

New `SynchronizationAnalyzer` (in `xf.xfvrp.opt.evaluation`):

- **Spatial**: trailer route segments must be covered by a compatible lorry segment
  (check via segment assignment from step 3).
- **Temporal**: for each transshipment, compute arrival times of both vehicles;
  transfer time = `f(amount)` (default: constant, configurable per TL).
  Violation → quality penalty (same mechanism as time window penalties in
  `EvaluationService`).
- **Load**: consistency of amounts at each TL; capacity check of the pulling lorry
  vs. trailer load while coupled.

Implementation mirrors `FeasibilityAnalzer`/`Context`: evaluate the plan
vehicle-by-vehicle, but with events at TLs carrying *transshipment records* that
link two vehicle visits.

---

## 4. Integration into XFVRP

### 4.1 Data model (`xf.xfvrp.base.fleximport`)

```java
public class TransshipmentData extends NodeData {
    TransshipmentData setExternID(String id)
    TransshipmentData setXlong(float xlong)
    TransshipmentData setYlat(float ylat)
    TransshipmentData setTimeWindow(float open, float close)   // optional
    TransshipmentData setParkingAllowed(boolean parkingAllowed) // false = pure transshipment point
    TransshipmentData setFixedTransferTime(float seconds)       // default: 0
}
```

- New `SiteType.TRANSSHIPMENT` (or reuse `REPLENISH` with a flag — decision in
  implementation, see §6.1).
- `CustomerData.setTrailerAccessible(boolean)` (default `true`) — defines
  lorry customer vs. trailer customer.
- `VehicleData.setAutonomous(boolean)` — lorries (`true`) vs. trailers (`false`).
  A trailer without allowed lorry makes the input invalid (precheck).

### 4.2 Node / Vehicle model (`xf.xfvrp.base`)

- `Node` gains nothing mandatory; trailer accessibility rides on the existing
  preset-vehicle-list mechanism (`presetBlockVehicleList`): a lorry customer is
  simply blocked for all vehicle types with trailer capability.
- `Vehicle` gains a `trailerId` field (0 = none) for the coupling relation, set
  during import from lorry/trailer declarations.

### 4.3 Orchestration

- New `xf.xfvrp.opt.construct.VRPTTSolutionBuilder` implementing
  `InitialSolutionBuilder` semantics: runs steps 1–3 (§2) and returns a `Solution`
  whose routes are the lorry routes (trailer movements represented as events
  attached to the pulling lorry's route).
- New `XFVRPOptTypes.VRPTT_ILS` for the improvement phase.
- Interplay with `IMixedFleetHeuristic`: v1 plans one heterogeneous fleet per
  mixed-fleet run; the fleet-mix heuristics stay untouched.

### 4.4 Report

- `RouteReport` unchanged for lorries; trailer itineraries are derivable from
  coupling events (`Event` with site type `TRANSSHIPMENT` + amounts).
- `ReportSummary` unchanged (costs of pulled trailer movement count towards the
  pulling lorry's route).

---

## 5. Example

```
Depot D; TLs T1, T2; trailer customers c1..c3; lorry customers L1, L2.

Step 1: c1 → DIRECT or VIA_TDP(T1); c2 → VIA_TDP(T1) or VIA_TDP(T2); ...
Step 2 (PDPTW over subtasks): trailer collects c1@T1, c2@T1, c3@T2;
        lorry pulls trailer D→T1→T2→D.
Step 3: segments D→T1, T1→T2, T2→D assigned to lorry "truck1";
        lorry without trailer serves L1, L2 directly.
```

---

## 6. Implementation plan

### Phase 1 (MVP) — direct service only
- `TransshipmentData` import + `TRANSSHIPMENT` site type + accessibility flags
- Enumeration of service modes (step 1) with fixed candidate set (all TLs within
  a radius parameter)
- Step 2 via existing PDP insert + ILS
- Temporal/load synchronization checks in evaluation (hard penalty)
- Report integration

### Phase 2
- Improvement neighborhoods (mode change, TL reassignment) — **done**, see
  Phase 2 status below
- Coupling optimization (change of pulling lorry at a TL) — **deferred**: the
  trailer keeps its declared pulling lorry (fixed coupling, TTRP semantics)
- Amount-dependent transfer times — **deferred**: fixed transfer durations per TL

### Phase 3 (optional) — see Phase 3 status below
- Simultaneous optimization of step-2/step-3 decisions (full generic heuristic)
- Lorry-flow / connectivity strengthening à la Drexl if solution quality gaps show up

### Phase 2 status (implemented)

**Additive coupled capacity.** `FlexiImporter` computes the train capacity of a
lorry–trailer coupling as the sum of lorry and trailer capacity per compartment
(`addTrainCapacity`). This alone measurably improves the benchmark (see below).

**Transshipment records.** A record is a pair of `TRANSSHIPMENT` node visits in
two routes of one `Solution`: the supplier route carries a drop visit with
`demand[0] = −x` directly after the served customer, the taker route a take
visit with `+x` at the same TL. Records are paired by TL `globalIdx` and exact
amount (epsilon 0.001). The `SynchronizationAnalyzer` checks

- *load*: the amounts of drop and take match; the taker's capacity holds with
  the additional amount, and the supplier's collected load is relieved by the
  drop (TRANSSHIPMENT nodes with amount are treated as `LoadType.PICKUP`),
- *temporal*: a new `RouteTimingSimulator` (mirroring the `EvaluationService`
  time model) computes both arrival times; the transfer must finish before the
  TL time window closes. Approximations: depot loading/unloading times and
  driving time restrictions are not mirrored (both do not shift the relevant
  arrival times).

Partial checks (`EvaluationService.check(solution, a, b)`) re-evaluate the
partner route of each record via `SynchronizationAnalyzer.findRecordPartners`.
Take-side penalties are charged to the taker route, drop-side penalties to the
supplier route.

**Improvement neighborhoods** (`VRPTTTransshipmentMover`, spec §2.4): addRecord,
removeRecord (mode change `VIA_TDP` → `DIRECT`) and TL re-assignment, as random
perturbations of the VRPTT ILS (probabilities 0.2/0.35/0.5, otherwise the base
segment perturbation) and as a greedy construction pass (records with costs
≤ current costs are kept, ties included, because they free supplier capacity).

**Evaluation support.** Stranded transshipment visits (no transferred amount,
no following customer) are deactivated by the `ActiveNodeAnalyzer`, so the
optimization can leave them behind when customers are relocated.

**Within-solution limitation (record semantics).** The mixed-fleet planning
decomposes the problem into one single-vehicle solution per vehicle (the model
has exactly one vehicle). Therefore all record candidates within a solution
span two trips of the *same* vehicle. Such records are synchronization-safe
(the transferred load waits at the TL until the vehicle's later trip picks it
up), but they never reduce costs by themselves: the load still ends at the
depot via the same vehicle's capacity, and the take visit adds a detour. True
inter-vehicle transshipment requires solutions containing several vehicles'
routes at once and is part of Phase 3 (simultaneous optimization).

### Phase 3 status (implemented)

**Joint multi-vehicle plan.** The Phase 3 solution representation stays a
list of per-vehicle solutions (one `Solution` per planning vehicle with its
own model), so the report building and all downstream consumers work
unchanged. The joint plan is processed by `VRPTTMixedFleetHeuristic` (opt-in
via `XFVRPParameter.setMixedFleetHeuristic`): it runs the default mixed fleet
planning and afterwards the joint record optimization
(`VRPTTJointRecordOptimizer`) on the per-vehicle solutions. Without
transshipment locations the heuristic behaves exactly like the
`DefaultMixedFleetHeuristic` (regression-guarded by a unit spec).

**Joint evaluation.** The `EvaluationService` can be given a record context
(foreign routes with their own models): the foreign routes are visible for
the record pairing of the `SynchronizationAnalyzer`, but only own routes are
evaluated. The temporal consistency of a cross-vehicle record is simulated
with the model of each involved route (`SynchronizationAnalyzer.analyze`
with per-route models). Take-side penalties are charged to the taker
solution, drop-side penalties to the supplier solution.

**Joint record moves.** For each collected customer of a supplier route the
nearest transshipment locations and the least-detour take positions of every
other vehicle's routes are candidates. Candidates are ranked by the
transferred amount (the freed supplier capacity drives the consolidation) and
the best one is applied:

- *acceptance*: the record is only kept, if both solutions stay feasible and
  the joint costs stay within the bound (load, temporal and matching
  consistency is checked by the joint evaluation),
- *local re-planning*: the affected solutions are re-optimized with the
  VRPTT ILS, whose evaluation (including the neighborhood moves) sees the
  record context of the joint plan, so the freed supplier capacity can
  consolidate its routes,
- *cleanup*: records, which can be removed without violating the
  synchronization and with lower joint costs, are removed,
- a whole pass is only committed, if the joint plan is feasible and cheaper,
  otherwise it is reverted (bounded by MAX_PASSES = 8, ≤ 60 evaluated
  candidates per pass).

Record visits with transferred amount at the end of a route are legal (the
taker carries the load home), so the structural pass-through rule of the
benchmark only applies to visits without amount (Phase 1 semantics). The
report events of record visits carry their transferred amount.

### Phase 3 benchmark result

On the 109 synthetic Drexl instances (`VRPTTDrexl`, NbrOfILSLoops=10,
maxRunningTimeInSec=30), against the Phase 2 baseline (totals
93324.77 → 80892.56):

| Class | Instances | Ø cost before | Ø cost after | Δ | inter-vehicle records |
|-------|-----------|---------------|--------------|---|-----------------------|
| 224   | 30        | 507.87        | 507.87       | +0.0% | 0 |
| 444   | 20        | 771.53        | 606.81       | −21.4% | 12 |
| 448   | 7         | 649.82        | 589.87       | −9.2% | 2 |
| 664   | 10        | 919.95        | 877.30       | −4.6% | 3 |
| 668   | 23        | 815.47        | 773.85       | −5.0% | 6 |
| 888   | 19        | 989.09        | 987.37       | −0.2% | 2 |
| Total | 109       | 80892.56      | 76832.80     | −5.0% | 25 |

All 109 instances stay feasible. 51 transshipment visits transfer load, of
which 25 are real inter-vehicle records (supplier and taker are different
vehicles). The total runtime grows only marginally (≈ 2.7 s for all 109
instances).

### Phase 2 benchmark result

On the 109 synthetic Drexl-style instances (`VRPTTDrexl`, NbrOfILSLoops=10,
maxRunningTimeInSec=30), against the Phase 1 baseline (commit `c0acc06`):

| Class | Instances | Ø cost before | Ø cost after | Δ |
|-------|-----------|---------------|--------------|---|
| 224   | 30        | 507.87        | 507.87       | +0.0% |
| 444   | 20        | 771.53        | 717.99       | −6.9% |
| 448   | 7         | 739.51        | 649.82       | −12.1% |
| 664   | 10        | 1104.36       | 919.95       | −16.7% |
| 668   | 23        | 1009.61       | 815.47       | −19.2% |
| 888   | 19        | 1221.94       | 989.09       | −19.1% |
| Total | 109       | 93324.77      | 80892.56     | −13.3% |

All 109 instances stay feasible; transshipment visits: 257 → 235, thereof
0 meaningful records. The improvement stems from the additive coupled
capacity; no record is cost-improving under the within-solution limitation
(see above). The record machinery (representation, consistency checks,
neighborhoods) is fully implemented and unit-tested and takes effect with
the Phase 3 joint multi-vehicle solutions (see below).

### Validation
- Unit: synchronization analyzer (temporal/load), service-mode enumeration
- End-to-end: Drexl's public VRPTT instances (109 instances, classes 4–8 customers
  × TLs × vehicles) as new `instances/` benchmark specs, wired into the nightly
  `benchmarks` CI job
- Regression: existing suite must stay green — all changes are additive; no
  existing `XFVRPOptTypes` behavior is modified

### Risks
- Solution representation for coupled movements is the critical design decision;
  v1 keeps trailer movements implicit in the lorry route (no second-level Solution),
  which limits Phase 3 but keeps the optimizer core untouched.
- Non-fixed lorry–trailer coupling is the hardest constraint; MVP restricts a
  trailer to one pulling lorry per planning run (TTRP semantics) if needed, then
  relaxes in Phase 2.
