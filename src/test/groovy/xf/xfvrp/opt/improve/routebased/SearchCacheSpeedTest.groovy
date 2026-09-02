package xf.xfvrp.opt.improve.routebased

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSingleSwap

/**
 * Verifies that the SearchCache reduces the number of searchRoutePair evaluations.
 *
 * Uses a NoOpSearchCache (needsEvaluation always true) as baseline and compares
 * searchRoutePair call counts against the real cache.
 */
class SearchCacheSpeedTest extends Specification {

    /**
     * A cache that never skips - every pair is always re-evaluated.
     * Used as a no-caching baseline for comparison.
     */
    static class NoOpSearchCache extends SearchCache {
        @Override
        boolean needsEvaluation(int routeA, int routeB) {
            return true
        }
    }

    /**
     * Counting wrapper for XFVRPSingleMove that tracks searchRoutePair invocations.
     */
    static class CountingSingleMove extends XFVRPSingleMove {
        int searchRoutePairCalls = 0

        @Override
        protected void searchRoutePair(Solution solution, Queue<float[]> queue, int routeIdxA, int routeIdxB) {
            searchRoutePairCalls++
            super.searchRoutePair(solution, queue, routeIdxA, routeIdxB)
        }
    }

    /**
     * Counting wrapper for XFVRPSingleSwap that tracks searchRoutePair invocations.
     */
    static class CountingSingleSwap extends XFVRPSingleSwap {
        int searchRoutePairCalls = 0

        @Override
        protected void searchRoutePair(Solution solution, Queue<float[]> queue, int routeIdxA, int routeIdxB) {
            searchRoutePairCalls++
            super.searchRoutePair(solution, queue, routeIdxA, routeIdxB)
        }
    }

    private XFVRPModel createModel() {
        def v = new TestVehicle(name: "V1", capacity: [10, 10]).getVehicle()

        def depot = new TestNode(
                globalIdx: 0, externID: "D1", geoId: 0,
                siteType: SiteType.DEPOT,
                xlong: 0, ylat: 0,
                demand: [0, 0],
                timeWindow: [[0, 999]],
                loadType: LoadType.DELIVERY
        ).getNode()

        // 12 customers in a deliberately bad initial order to ensure multiple improvement iterations
        def customers = []
        def positions = [
                [-3, 1], [-3, 2], [-3, 3], [-3, 4],
                [3, 1],  [3, 2],  [3, 3],  [3, 4],
                [0, 5],  [1, 6],  [-1, 6], [0, 7]
        ]
        positions.eachWithIndex { pos, i ->
            customers << new TestNode(
                    globalIdx: i + 1, externID: "C${i + 1}", geoId: i + 1,
                    xlong: pos[0] as float, ylat: pos[1] as float,
                    demand: [1, 1],
                    timeWindow: [[0, 999]],
                    loadType: LoadType.DELIVERY
            ).getNode()
        }

        depot.setIdx(0)
        customers.eachWithIndex { c, i -> c.setIdx(i + 1) }

        def nodes = [depot] + customers
        return TestXFVRPModel.get(nodes, v)
    }

    /**
     * Creates a deliberately bad initial solution with interleaved customers
     * across routes, so the optimizer has multiple improving moves to find.
     */
    private Solution createBadSolution(XFVRPModel model) {
        def n = model.getNodes()
        // 4 routes with customers mixed across geographic clusters
        return Helper.set(model,
                [n[0], n[4], n[1], n[8], n[0], n[2], n[5], n[9], n[0], n[6], n[3], n[10], n[0], n[7], n[11], n[12], n[0]] as Node[])
    }

    def "Cache reduces searchRoutePair evaluations for SingleMove"() {
        given: "Two identical operators - one with real cache, one without"
        def model = createModel()

        def withCache = new CountingSingleMove()
        withCache.setModel(model)

        def withoutCache = new CountingSingleMove()
        withoutCache.searchCache = new NoOpSearchCache()
        withoutCache.setModel(model)

        def sol1 = createBadSolution(model)
        def sol2 = sol1.copy()

        when: "Both optimize the same initial solution"
        withCache.execute(sol1)
        withoutCache.execute(sol2)

        then: "Cache version evaluates significantly fewer route pairs"
        withCache.searchRoutePairCalls < withoutCache.searchRoutePairCalls
        // Both should produce equally good solutions
        sol1.getQuality().getFitness() == sol2.getQuality().getFitness()
    }

    def "Cache reduces searchRoutePair evaluations for SingleSwap"() {
        given: "Two identical operators - one with real cache, one without"
        def model = createModel()

        def withCache = new CountingSingleSwap()
        withCache.setModel(model)

        def withoutCache = new CountingSingleSwap()
        withoutCache.searchCache = new NoOpSearchCache()
        withoutCache.setModel(model)

        def sol1 = createBadSolution(model)
        def sol2 = sol1.copy()

        when: "Both optimize the same initial solution"
        withCache.execute(sol1)
        withoutCache.execute(sol2)

        then: "Cache version evaluates significantly fewer route pairs"
        withCache.searchRoutePairCalls < withoutCache.searchRoutePairCalls
        // Both should produce equally good solutions
        sol1.getQuality().getFitness() == sol2.getQuality().getFitness()
    }
}
