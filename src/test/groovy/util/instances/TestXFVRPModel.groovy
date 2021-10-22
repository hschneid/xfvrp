package util.instances

import xf.xfvrp.base.Node
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.compartment.CompartmentInitializer
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.InternalMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator

class TestXFVRPModel {

    public static XFVRPModel get(List<Node> nodes, Vehicle vehicle) {
        def nodeArr = nodes.toArray(new Node[0])

        def parameter = new XFVRPParameter()
        def metric = new EucledianMetric()
        def iMetric = new AcceleratedMetricTransformator().transform(metric, nodeArr, vehicle)

        return get(
                nodeArr,
                iMetric,
                iMetric,
                vehicle,
                parameter
        )
    }


    public static XFVRPModel get(Node[] nodeArr, InternalMetric metric, InternalMetric optMetric, Vehicle vehicle, XFVRPParameter parameter) {
        List<CompartmentType> types = new ArrayList<>()
        CompartmentInitializer.check(nodeArr, types, new Vehicle[]{vehicle})

        return new XFVRPModel(
                nodeArr,
                types.toArray(new CompartmentType[0]),
                metric,
                optMetric,
                vehicle,
                parameter
        )
    }
}
