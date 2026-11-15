#!/usr/bin/env python3
"""
Deterministic generator for synthetic VRPTT benchmark instances.

The original VRPTT benchmark instances of Michael Drexl (Working Paper
"Branch-and-Cut Algorithms for the Vehicle Routing Problem with Trailers and
Transshipments", Gutenberg School of Management and Economics, JGU Mainz,
LM-2012-1210, section 6.1) were never published for download. This generator
reproduces the instance generation recipe, which is fully described in the
paper, to obtain synthetic instances of the same structure:

- Customer and transshipment locations are randomly selected on a 100 x 100 km
  grid with the depot located in the centre (50, 50).
- Supplies are chosen randomly from [1000; 10000].
- Loading/service time at a customer is 2 minutes per 1000 supply units.
- Load transfer time at a transshipment location is 2 minutes per 1000 units
  of transferred supply. As the Phase 1 MVP only supports a fixed transfer
  time, the amount-dependent transfer time is rounded to the expected mean
  transferred amount (2 min x mean supply / 1000).
- Planning horizon is 1320 minutes; transshipment locations have a
  non-restrictive time window [0; 1320].
- Fleet per Table 1 of the paper: two lorry classes (capacities 10000/15000,
  fixed costs 180/200, costs per km 0.65/0.70) and two trailer classes
  (capacities 10000/15000, fixed costs 20/25, costs per km 0.04).
  Distances in the paper are Euclidean distances multiplied by a distance
  factor of 1.3; here the factor is folded into the variable costs
  (costs per km x 1.3), which yields identical objective values.
- "x y z" instances: x customers (x/2 lorry customers, x/2 trailer customers),
  y = x transshipment locations (y/2 co-located with a trailer customer,
  y/2 pure transshipment locations), z vehicles (z/4 per fleet class).

Phase-1 adaptation of the fleet (documented deviation):
  In the XFVRP Phase-1 MVP every trailer needs its own declared pulling lorry
  and at least one lorry must remain trailer-free, because lorry customers can
  only be served by pure lorries. Therefore
  - class 4 instances (z = 4): only the class-1 trailer is declared, pulled by
    the class-2 lorry; the class-1 lorry stays pure.
  - class 8 instances (z = 8): z/8 (rounded down, at least 1) trailers of each
    class are declared; class-2 trailers are pulled by class-1 lorries,
    class-1 trailers by class-2 lorries, so a pure lorry of each class remains.

The instance classes follow Table 2 of the paper, giving 109 instances in
total: 2 2 4 (30), 4 4 4 (20), 6 6 4 (10), 4 4 8 (7), 6 6 8 (23), 8 8 8 (19).

Usage: python3 generate_instances.py [target-dir]
"""
import os
import random
import sys

GRID = 100.0
DEPOT = (50.0, 50.0)
HORIZON = 1320
SUPPLY_MIN, SUPPLY_MAX = 1000, 10000
TRANSFER_MIN_PER_1000 = 2.0
DISTANCE_FACTOR = 1.3

# (capacity, fixed costs, costs per km) - paper Table 1
LORRY_CLASSES = {"L1": (10000, 180, 0.65), "L2": (15000, 200, 0.70)}
TRAILER_CLASSES = {"T1": (10000, 20, 0.04), "T2": (15000, 25, 0.04)}

# (x = nbr of customers, z = nbr of vehicles, nbr of instances) - paper Table 2
INSTANCE_CLASSES = [
    (2, 4, 30),
    (4, 4, 20),
    (6, 4, 10),
    (4, 8, 7),
    (6, 8, 23),
    (8, 8, 19),
]


def fleet(x, z):
    """Builds the fleet (lorry ids, trailer ids with pulling lorry)."""
    n = z // 4
    lorries = ["L1_%d" % i for i in range(n)] + ["L2_%d" % i for i in range(n)]
    trailers = []
    if z == 4:
        # Phase-1 adaptation: one trailer only, so that a pure lorry remains.
        trailers.append(("T1_0", "T1", "L2_0"))
    else:
        nbr = max(1, n // 2)
        for i in range(nbr):
            trailers.append(("T2_%d" % i, "T2", "L1_%d" % i))
        for i in range(nbr):
            trailers.append(("T1_%d" % i, "T1", "L2_%d" % i))
    return lorries, trailers


def gen_instance(x, z, seq):
    rng = random.Random(1000 * x + z * 10 + seq)
    name = "%d%d%d_%02d" % (x, x, z, seq)

    lines = [
        "# Synthetic VRPTT benchmark instance (NOT an original Drexl instance).",
        "# Structure faithfully generated per M. Drexl (2012), LM-2012-1210, section 6.1.",
        "# See README.md in this folder for the generation rules and deviations.",
        "NAME %s" % name,
        "HORIZON %d" % HORIZON,
        "DEPOT DEP %.2f %.2f" % DEPOT,
    ]

    # x customers: first half lorry customers, second half trailer customers
    customers = []
    for i in range(x):
        cid = "C%03d" % (i + 1)
        loc = (round(rng.uniform(0, 100), 2), round(rng.uniform(0, 100), 2))
        supply = rng.randint(SUPPLY_MIN, SUPPLY_MAX)
        trailer_accessible = i >= x // 2
        service_time = int(TRANSFER_MIN_PER_1000 * supply / 1000)
        customers.append((cid, loc, supply, trailer_accessible))
        lines.append("CUSTOMER %s %.2f %.2f %d %d %d" % (
            cid, loc[0], loc[1], supply, 1 if trailer_accessible else 0, service_time))

    # y = x transshipment locations: y/2 co-located with a trailer customer,
    # y/2 pure transshipment locations
    mean_supply = sum(c[2] for c in customers) / x
    transfer_time = int(round(TRANSFER_MIN_PER_1000 * mean_supply / 1000))
    trailer_customer_locs = [c[1] for c in customers if c[3]]
    for t in range(x):
        if t < x // 2:
            loc = trailer_customer_locs[t % len(trailer_customer_locs)]
        else:
            loc = (round(rng.uniform(0, 100), 2), round(rng.uniform(0, 100), 2))
        lines.append("TL TL%02d %.2f %.2f %d" % (t + 1, loc[0], loc[1], transfer_time))

    lorries, trailers = fleet(x, z)
    for lid in lorries:
        cls = "L1" if lid.startswith("L1") else "L2"
        cap, fix, var = LORRY_CLASSES[cls]
        lines.append("LORRY %s %d %d %.3f" % (lid, cap, fix, var * DISTANCE_FACTOR))
    for tid, cls, puller in trailers:
        cap, fix, var = TRAILER_CLASSES[cls]
        lines.append("TRAILER %s %d %d %.3f %s" % (tid, cap, fix, var * DISTANCE_FACTOR, puller))

    return name, "\n".join(lines) + "\n"


def main():
    target = sys.argv[1] if len(sys.argv) > 1 else "."
    os.makedirs(target, exist_ok=True)
    count = 0
    for x, z, nbr in INSTANCE_CLASSES:
        for seq in range(1, nbr + 1):
            name, content = gen_instance(x, z, seq)
            with open(os.path.join(target, name + ".txt"), "w") as f:
                f.write(content)
            count += 1
    print("Generated %d instances in %s" % (count, os.path.abspath(target)))


if __name__ == "__main__":
    main()
