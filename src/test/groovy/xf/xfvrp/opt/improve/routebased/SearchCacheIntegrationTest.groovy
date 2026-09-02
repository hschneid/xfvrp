package xf.xfvrp.opt.improve.routebased

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove
import xf.xfvrp.opt.improve.routebased.move.XFVRPSegmentMove
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSingleSwap

/**
 * Integration tests for the SearchCache mechanism.
 *
 * Verifies that the optimization produces correct results with the cache,
 * and that the cache is populated after improvements.
 */
class SearchCacheIntegrationTest extends Specification {

    private XFVRPModel createMultiRouteModel() {
        def v = new TestVehicle(name: "V1", capacity: [10, 10]).getVehicle()

        def depot = new TestNode(
                globalIdx: 0, externID: "D1", geoId: 0,
                siteType: SiteType.DEPOT,
                xlong: 0, ylat: 0,
                demand: [0, 0],
                timeWindow: [[0, 999]],
                loadType: LoadType.DELIVERY
        ).getNode()

        def c1 = createCustomer(1, "C1", -2, 1, 1)
        def c2 = createCustomer(2, "C2", -2, 2, 2)
        def c3 = createCustomer(3, "C3", -2, 3, 3)
        def c4 = createCustomer(4, "C4", 2, 1, 4)
        def c5 = createCustomer(5, "C5", 2, 2, 5)
        def c6 = createCustomer(6, "C6", 2, 3, 6)
        def c7 = createCustomer(7, "C7", 0, 4, 7)
        def c8 = createCustomer(8, "C8", 1, 5, 8)
        def c9 = createCustomer(9, "C9", -1, 5, 9)

        depot.setIdx(0)
        c1.setIdx(1); c2.setIdx(2); c3.setIdx(3)
        c4.setIdx(4); c5.setIdx(5); c6.setIdx(6)
        c7.setIdx(7); c8.setIdx(8); c9.setIdx(9)

        def nodes = [depot, c1, c2, c3, c4, c5, c6, c7, c8, c9]
        return TestXFVRPModel.get(nodes, v)
    }

    private Node createCustomer(int idx, String id, float x, float y, int geoId) {
        return new TestNode(
                globalIdx: idx, externID: id, geoId: geoId,
                xlong: x, ylat: y,
                demand: [1, 1],
                timeWindow: [[0, 999]],
                loadType: LoadType.DELIVERY
        ).getNode()
    }

    def "SingleMove finds improvement with cache"() {
        given:
        def model = createMultiRouteModel()
        def n = model.getNodes()
        def sol = Helper.set(model,
                [n[0], n[3], n[1], n[2], n[0], n[6], n[4], n[5], n[0], n[9], n[7], n[8], n[0]] as Node[])

        def service = new XFVRPSingleMove()
        service.setModel(model)

        when:
        def quality = service.check(sol)
        def result = service.improve(sol, quality)

        then:
        result != null
        result.getFitness() < quality.getFitness()
    }

    def "Cache is populated after improvement"() {
        given:
        def model = createMultiRouteModel()
        def n = model.getNodes()
        def sol = Helper.set(model,
                [n[0], n[3], n[1], n[2], n[0], n[6], n[4], n[5], n[0], n[9], n[7], n[8], n[0]] as Node[])

        def service = new XFVRPSingleMove()
        service.setModel(model)

        when:
        def quality = service.check(sol)
        service.improve(sol, quality)

        then:
        service.searchCache.isInitialized()
        service.searchCache.getChangedRoutes().size() > 0
    }

    def "Full execute with SingleMove produces valid result"() {
        given:
        def model = createMultiRouteModel()
        def n = model.getNodes()
        def sol = Helper.set(model,
                [n[0], n[3], n[1], n[2], n[0], n[6], n[4], n[5], n[0], n[9], n[7], n[8], n[0]] as Node[])

        def service = new XFVRPSingleMove()
        service.setModel(model)
        def qualityBefore = service.check(sol)

        when:
        def result = service.execute(sol)

        then:
        result != null
        result.getQuality().getFitness() <= qualityBefore.getFitness()
    }

    def "Full execute with SegmentMove produces valid result"() {
        given:
        def model = createMultiRouteModel()
        def n = model.getNodes()
        def sol = Helper.set(model,
                [n[0], n[3], n[1], n[2], n[0], n[6], n[4], n[5], n[0], n[9], n[7], n[8], n[0]] as Node[])

        def service = new XFVRPSegmentMove()
        service.setModel(model)
        def qualityBefore = service.check(sol)

        when:
        def result = service.execute(sol)

        then:
        result != null
        result.getQuality().getFitness() <= qualityBefore.getFitness()
    }

    def "Full execute with SingleSwap produces valid result"() {
        given:
        def model = createMultiRouteModel()
        def n = model.getNodes()
        def sol = Helper.set(model,
                [n[0], n[3], n[1], n[2], n[0], n[6], n[4], n[5], n[0], n[9], n[7], n[8], n[0]] as Node[])

        def service = new XFVRPSingleSwap()
        service.setModel(model)
        def qualityBefore = service.check(sol)

        when:
        def result = service.execute(sol)

        then:
        result != null
        result.getQuality().getFitness() <= qualityBefore.getFitness()
    }
}
