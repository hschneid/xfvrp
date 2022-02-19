package xf.xfvrp.opt.improve.routebased.move

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

class XFVRPSegmentMoveTest extends Specification {

    def service = new XFVRPSegmentMove()

    def n1 = new Node(externID: "1", siteType: SiteType.DEPOT)
    def n2 = new Node(externID: "2", siteType: SiteType.CUSTOMER)
    def n3 = new Node(externID: "3", siteType: SiteType.CUSTOMER)
    def n4 = new Node(externID: "4", siteType: SiteType.CUSTOMER)
    def n5 = new Node(externID: "5", siteType: SiteType.DEPOT)
    def n6 = new Node(externID: "6", siteType: SiteType.CUSTOMER)
    def n7 = new Node(externID: "7", siteType: SiteType.CUSTOMER)
    def n8 = new Node(externID: "8", siteType: SiteType.DEPOT)

    def n9 = new Node(externID: "9", siteType: SiteType.CUSTOMER)
    def n10 = new Node(externID: "10", siteType: SiteType.CUSTOMER)

    XFVRPModel model

    def setup() {
        def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
        model = TestXFVRPModel.get([n1,n5,n8,n2,n3,n4,n6,n7,n9, n10], v)
    }

    def "change - reset - different routes"() {
        def sol = Helper.set(model, [n1, n2, n3, n4, n5, n6, n7, n8] as Node[])
        def parameter = [-1, 0, 1, 1, 2, 2, 0] as float[]

        when:
        XFVRPMoveUtil.change(sol, parameter)
        XFVRPMoveUtil.reverseChange(sol, parameter)
        def result = sol.getGiantRoute()

        then:
        result[0].externID == "1"
        result[1].externID == "2"
        result[2].externID == "3"
        result[3].externID == "4"
        result[4].externID == "5"
        result[5].externID == "6"
        result[6].externID == "7"
        result[7].externID == "1"
    }

    def "change - reset - same routes - src < dst"() {
        def sol = Helper.set(model, [n1, n2, n3, n4, n6, n7, n8] as Node[])
        def parameter = [-1, 0, 0, 2, 6, 1, 0] as float[]

        when:
        XFVRPMoveUtil.change(sol, parameter)
        XFVRPMoveUtil.reverseChange(sol, parameter)
        def result = sol.getGiantRoute()
        then:
        result[0].externID == "1"
        result[1].externID == "2"
        result[2].externID == "3"
        result[3].externID == "4"
        result[4].externID == "6"
        result[5].externID == "7"
        result[6].externID == "1"
    }

    def "change - reset - same routes - src > dst"() {
        def sol = Helper.set(model, [n1, n2, n3, n4, n6, n7, n8] as Node[])
        def parameter = [-1, 0, 0, 3, 2, 1, 0] as float[]

        when:
        XFVRPMoveUtil.change(sol, parameter)
        XFVRPMoveUtil.reverseChange(sol, parameter)
        def result = sol.getGiantRoute()
        then:
        result[0].externID == "1"
        result[1].externID == "2"
        result[2].externID == "3"
        result[3].externID == "4"
        result[4].externID == "6"
        result[5].externID == "7"
        result[6].externID == "1"
    }

    def "change - reset - different routes - with invert"() {
        def sol = Helper.set(model, [n1, n2, n3, n4, n5, n6, n7, n8] as Node[])
        def parameter = [-1, 0, 1, 1, 2, 2, 1] as float[]

        when:
        XFVRPMoveUtil.change(sol, parameter)
        XFVRPMoveUtil.reverseChange(sol, parameter)
        def result = sol.getGiantRoute()

        then:
        result[0].externID == "1"
        result[1].externID == "2"
        result[2].externID == "3"
        result[3].externID == "4"
        result[4].externID == "5"
        result[5].externID == "6"
        result[6].externID == "7"
        result[7].externID == "1"
    }

    def "change - reset - same routes - src < dst - with invert"() {
        def sol = Helper.set(model, [n1, n2, n3, n4, n6, n7, n8] as Node[])
        def parameter = [-1, 0, 0, 2, 6, 1, 1] as float[]

        when:
        XFVRPMoveUtil.change(sol, parameter)
        XFVRPMoveUtil.reverseChange(sol, parameter)
        def result = sol.getGiantRoute()
        then:
        result[0].externID == "1"
        result[1].externID == "2"
        result[2].externID == "3"
        result[3].externID == "4"
        result[4].externID == "6"
        result[5].externID == "7"
        result[6].externID == "1"
    }

    def "change - reset - same routes - src > dst - with invert"() {
        def sol = Helper.set(model, [n1, n2, n3, n4, n6, n7, n8] as Node[])
        def parameter = [-1, 0, 0, 3, 2, 1, 1] as float[]

        when:
        XFVRPMoveUtil.change(sol, parameter)
        XFVRPMoveUtil.reverseChange(sol, parameter)
        def result = sol.getGiantRoute()
        then:
        result[0].externID == "1"
        result[1].externID == "2"
        result[2].externID == "3"
        result[3].externID == "4"
        result[4].externID == "6"
        result[5].externID == "7"
        result[6].externID == "1"
    }

    def "find an improvement"() {
        def model = initScen()
        def n = model.getNodes()
        
        def sol = Helper.set(model, [n[0], n[1], n[2], n[5], n[6], n[3], n[4], n[7], n[0]] as Node[])

        when:
        def newQuality = service.improve(sol, new Quality(cost: Float.MAX_VALUE), model)
        def result = sol.getGiantRoute()
        then:
        newQuality.cost < 10
        result[0].externID == "1"
        result[1].externID == "2"
        result[2].externID == "3"
        result[3].externID == "5"
        result[4].externID == "6"
        result[5].externID == "7"
        result[6].externID == "9"
        result[7].externID == "10"
        result[8].externID == "1"
    }

    def "find an improvement with invert"() {
        def model = initScen()
        def n = model.getNodes()

        
        def sol = Helper.set(model, [n[0], n[1], n[2], n[7], n[6], n[5], n[3], n[4], n[0]] as Node[])

        when:
        def newQuality = service.improve(sol, new Quality(cost: Float.MAX_VALUE), model)
        def result = sol.getGiantRoute()
        then:
        newQuality.cost < 10
        result[0].externID == "1"
        result[1].externID == "2"
        result[2].externID == "3"
        result[3].externID == "5"
        result[4].externID == "6"
        result[5].externID == "7"
        result[6].externID == "9"
        result[7].externID == "10"
        result[8].externID == "1"
    }

    def "find no improvement anymore"() {
        def model = initScen()
        def n = model.getNodes()

        
        def sol = Helper.set(model, [n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[0]] as Node[])

        when:
        def newQuality = service.improve(sol, new Quality(cost: Float.MAX_VALUE), model)
        def result = sol.getGiantRoute()
        then:
        newQuality == null
        result[0].externID == "1"
        result[1].externID == "2"
        result[2].externID == "3"
        result[3].externID == "5"
        result[4].externID == "6"
        result[5].externID == "7"
        result[6].externID == "9"
        result[7].externID == "10"
        result[8].externID == "1"
    }

    XFVRPModel initScen() {
        def v = new TestVehicle(name: "V1", capacity: [15, 15]).getVehicle()

        n1 = new TestNode(
                globalIdx: 1,
                externID: "1",
                geoId: 1,
                siteType: SiteType.DEPOT,
                xlong: 0,
                ylat: 0,
                demand: [0, 0],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        n2 = new TestNode(
                globalIdx: 2,
                externID: "2",
                geoId: 2,
                xlong: -1,
                ylat: 1,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        n3 = new TestNode(
                globalIdx: 3,
                externID: "3",
                geoId: 3,
                xlong: -1,
                ylat: 2f,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        n5 = new TestNode(
                globalIdx: 5,
                externID: "5",
                geoId: 5,
                xlong: -1,
                ylat: 3,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        n6 = new TestNode(
                globalIdx: 6,
                externID: "6",
                geoId: 6,
                xlong: 0,
                ylat: 4,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        n7 = new TestNode(
                globalIdx: 7,
                externID: "7",
                geoId: 7,
                xlong: 1,
                ylat: 3,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        n9 = new TestNode(
                globalIdx: 9,
                externID: "9",
                geoId: 9,
                xlong: 1,
                ylat: 2,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()
        n10 = new TestNode(
                globalIdx: 10,
                externID: "10",
                geoId: 10,
                xlong: 1,
                ylat: 1,
                demand: [1, 1],
                timeWindow: [[0,99]],
                loadType: LoadType.DELIVERY)
                .getNode()

        n1.setIdx(0)
        n2.setIdx(1)
        n3.setIdx(2)
        n5.setIdx(3)
        n6.setIdx(4)
        n7.setIdx(5)
        n9.setIdx(6)
        n10.setIdx(7)

        def nodes = [n1, n2, n3, n5, n6, n7, n9, n10]

        return TestXFVRPModel.get(nodes, v)
    }
}
