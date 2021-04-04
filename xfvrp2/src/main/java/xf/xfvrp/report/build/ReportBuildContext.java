package xf.xfvrp.report.build;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.opt.XFVRPSolution;

public class ReportBuildContext {

    private final XFVRPModel model;
    private final Node[] giantRoute;

    private float[][] routeDepotServiceMap;

    private float time = 0;
    private float drivingTime = 0;
    private float delay = 0;

    public ReportBuildContext(XFVRPSolution solution) {
        this.model = solution.getModel();
        this.giantRoute = solution.getSolution().getGiantRoute();
    }

    public Node[] getGiantRoute() {
        return giantRoute;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public float getDrivingTime() {
        return drivingTime;
    }

    public void setDrivingTime(float drivingTime) {
        this.drivingTime = drivingTime;
    }

    public float getDelay() {
        return delay;
    }

    public void setDelay(float delay) {
        this.delay = delay;
    }

    public XFVRPParameter getParameter() {
        return model.getParameter();
    }

    public void setRouteDepotServiceMap(float[][] routeDepotServiceMap) {
        this.routeDepotServiceMap = routeDepotServiceMap;
    }

    public float[][] getRouteDepotServiceMap() {
        return routeDepotServiceMap;
    }

    public float[] getDistanceAndTime(Node n1, Node n2) {
        return model.getDistanceAndTime(n1, n2);
    }

    public void addTime(float value) {
        time += value;
    }

    public void addDrivingTime(float value) {
        drivingTime += value;
    }

    public float getMetricTime(Node n1, Node n2) {
        return model.getTime(n1, n2);
    }

    public Vehicle getVehicle() {
        return model.getVehicle();
    }

    public void addDelay(float value) {
        delay += value;
    }
}
