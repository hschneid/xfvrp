package xf.xfvrp.opt.improve.routebased.swap

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.opt.Solution

class XFVRPSegmentSwapTest extends Specification {

    def exchangeService = new XFVRPSegmentExchange()
    def segmentService = new XFVRPSegmentSwap()
    def singleService = new XFVRPSingleSwap()

    def sol

    def "improve by segment swap"() {
        def model = initScen()
        segmentService.setModel(model)
        def n = model.getNodes()
        sol = Helper.set(model, [nd, n[5], n[2], n[4], nd2, n[6], n[3], n[7], nd2] as Node[])

        when:
        def sol2 = segmentService.execute(sol)
        def result = Helper.get(sol2)
        then:
        result[0].externID == "DEP"
        result[1].externID == "1"
        result[2].externID == "2"
        result[3].externID == "3"
        result[4].externID == "DEP"
        result[5].externID == "DEP2"
        result[6].externID == "4"
        result[7].externID == "5"
        result[8].externID == "6"
        result[9].externID == "DEP2"
    }

    def "improve by single swap"() {
        def model = initScen()
        singleService.setModel(model)
        def n = model.getNodes()
        sol = Helper.set(model, [nd, n[5], n[2], n[4], nd2, n[6], n[3], n[7], nd2] as Node[])

        when:
        def sol2 = singleService.execute(sol)
        def result = Helper.get(sol2)
        then:
        result[0].externID == "DEP"
        result[1].externID == "1"
        result[2].externID == "2"
        result[3].externID == "3"
        result[4].externID == "DEP"
        result[5].externID == "DEP2"
        result[6].externID == "4"
        result[7].externID == "5"
        result[8].externID == "6"
        result[9].externID == "DEP2"
    }

    def "improve by segment exchange"() {
        def model = initScen()
        exchangeService.setModel(model)
        def n = model.getNodes()
        sol = Helper.set(model, [nd, n[5], n[3], n[7], nd2, n[6], n[2], n[4], nd2] as Node[])

        when:
        def sol2 = exchangeService.execute(sol)
        def result = Helper.get(sol2)
        then:
        result[0].externID == "DEP"
        result[1].externID == "1"
        result[2].externID == "2"
        result[3].externID == "3"
        result[4].externID == "DEP"
        result[5].externID == "DEP2"
        result[6].externID == "4"
        result[7].externID == "5"
        result[8].externID == "6"
        result[9].externID == "DEP2"
    }

    def nd = new TestNode(
            externID: "DEP",
            globalIdx: 0,
            xlong: -3,
            ylat: 3,
            siteType: SiteType.DEPOT,
            demand: [0, 0],
            timeWindow: [[0,99],[2,99]]
    ).getNode()

    def nd2 = new TestNode(
            externID: "DEP2",
            globalIdx: 5,
            xlong: 3,
            ylat: 3,
            siteType: SiteType.DEPOT,
            demand: [0, 0],
            timeWindow: [[0,99],[2,99]]
    ).getNode()

    XFVRPModel initScen() {
        def v = new TestVehicle(name: "V1", capacity: [4, 4]).getVehicle()

        def n1 = new TestNode(
                globalIdx: 1,
                externID: "1",
                xlong: -2,
                ylat: 4,
                geoId: 1,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        def n2 = new TestNode(
                globalIdx: 2,
                externID: "2",
                xlong: -1,
                ylat: 3,
                geoId: 2,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        def n3 = new TestNode(
                globalIdx: 3,
                externID: "3",
                xlong: -2,
                ylat: 2,
                geoId: 3,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        def n4 = new TestNode(
                globalIdx: 4,
                externID: "4",
                xlong: 2,
                ylat: 4,
                geoId: 4,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        def n5 = new TestNode(
                globalIdx: 6,
                externID: "5",
                xlong: 1,
                ylat: 3,
                geoId: 5,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        def n6 = new TestNode(
                globalIdx: 7,
                externID: "6",
                xlong: 2,
                ylat: 2,
                geoId: 6,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()

        nd.setIdx(0)
        nd2.setIdx(1)
        n1.setIdx(2)
        n2.setIdx(3)
        n3.setIdx(4)
        n4.setIdx(5)
        n5.setIdx(6)
        n6.setIdx(7)

        def nodes = [nd, nd2, n1, n2, n3, n4, n5, n6] as Node[]

        return TestXFVRPModel.get(Arrays.asList(nodes), v)
    }
}
