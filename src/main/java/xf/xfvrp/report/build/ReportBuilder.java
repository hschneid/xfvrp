package xf.xfvrp.report.build;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.XFVRPSolution;
import xf.xfvrp.opt.evaluation.*;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * @author hschneid
 *
 */
public class ReportBuilder {

    public Report getReport(XFVRPSolution solution) throws XFVRPException {
        XFVRPModel model = solution.getModel();
        Report rep = new Report(solution.getSolution());

        Context context = ContextBuilder.build(model);
        for (Node[] route : solution.getSolution()) {
            // Feasibility check
            FeasibilityAnalzer.checkFeasibility(route);

            if(route.length <= 2) {
                continue;
            }

            rep.add(getRouteReport(route, context));
        }

        return rep;
    }

    private RouteReport getRouteReport(Node[] route, Context context) throws XFVRPException {
        RouteReport routeReport = new RouteReport(context.getModel().getVehicle());

        route = ActiveNodeAnalyzer.getActiveNodes(route);
        context.setRouteInfos(RouteInfoBuilder.build(route, context));

        context.setCurrentNode(route[0]);
        routeReport.add(
                beginRoute(route, context),
                context
        );
        context.setNextNode(route[0]);
        for (int i = 1; i < route.length; i++) {
            context.setNextNode(route[i]);

            Event event = createEvent(context);

            drive(context, routeReport);

            fillEvent(event, context);

            routeReport.add(event, context);
        }

        return routeReport;
    }

    private Event beginRoute(Node[] route, Context context) throws XFVRPException {
        Node newDepot = context.getCurrentNode();
        Node nextNode = findNextCustomerNEW(route);

        XFVRPModel model = context.getModel();

        context.createNewRoute(newDepot);

        float earliestDepartureTime = (nextNode != null) ? nextNode.getTimeWindow(0)[0] - model.getTime(newDepot, nextNode) : 0;

        // If loading time at depot should be considered, service time of all
        // deliveries at the route is added to starting time at depot
        float loadingTimeAtDepot = 0;
        if(model.getParameter().isWithLoadingTimeAtDepot() && nextNode != null)
            loadingTimeAtDepot = context.getLoadingServiceTimeAtDepot();

        context.setDepartureTimeAtDepot(earliestDepartureTime, loadingTimeAtDepot);

        // Add route start event
        Event e = new Event(newDepot);
        e.setDistance(0);
        e.setArrival(context.getFittingTimeWindow()[0]);
        e.setDeparture(context.getRouteVar()[Context.TIME]);
        e.setService(loadingTimeAtDepot);
        e.setWaiting(0);
        e.setDelay(0);
        e.setLoadType(null);
        e.setTravelTime(0);

        Amount deliveryAmount = context.getRouteInfo().getDeliveryAmount();
        setAmountsToEvent(e,
                deliveryAmount.getAmounts(),
                LoadType.PRELOAD_AT_DEPOT
        );

        return e;
    }

    private void drive(Context context, RouteReport report) {
        float[] dist = context.getModel().getDistanceAndTime(context.getLastNode(), context.getCurrentNode());
        context.drive(dist);

        // check max driving time per shift restrictions
        if(context.getDrivingTime() >= context.getModel().getVehicle().getMaxDrivingTimePerShift()) {
            context.resetDrivingTime();

            Node waitingNode = context.getCurrentNode().copy();
            waitingNode.setSiteType(SiteType.PAUSE);
            Event e = new Event(waitingNode); // Driver Pause
            e.setDuration(context.getModel().getVehicle().getWaitingTimeBetweenShifts());
            e.setLoadType(LoadType.PAUSE);

            report.add(e, context);
        }
    }

    private Event createEvent(Context context) {
        Event e = new Event(context.getCurrentNode());

        switch(context.getCurrentNode().getSiteType()) {
            case REPLENISH:
                if(context.getRouteInfo() != null) {
                    setAmountsToEvent(e,
                            context.getRouteInfo().getDeliveryAmount().getAmounts(),
                            LoadType.PRELOAD_AT_DEPOT
                    );
                }
                break;
            case CUSTOMER:
                setAmountsToEvent(e,
                        context.getCurrentNode().getDemand(),
                        context.getCurrentNode().getLoadType()
                );
                break;
        }

        e.setDelay(context.getRouteVar()[Context.DELAY]);

        return e;
    }

    private void fillEvent(Event event, Context context) {
        float[] dist = context.getModel().getDistanceAndTime(context.getLastNode(), context.getCurrentNode());
        event.setDistance(dist[0]);
        event.setTravelTime(dist[1]);
        event.setArrival(context.getRouteVar()[Context.TIME]);

        float[] timeWindow = context.getFittingTimeWindow();

        // Depot service time at end depot
        float totalServiceTime = 0;
        if(context.getModel().getParameter().isWithUnloadingTimeAtDepot() &&
                context.getCurrentNode().getSiteType() == SiteType.DEPOT) {
            float unloadingServiceTimeAtDepot = context.getUnLoadingServiceTimeAtDepot();
            context.addToTime(unloadingServiceTimeAtDepot);
            context.addToDuration(unloadingServiceTimeAtDepot);

            totalServiceTime += unloadingServiceTimeAtDepot;
        }

        context.addDelayWithTimeWindow(timeWindow);
        event.setDelay(context.getDelay() - event.getDelay());

        float waitingTime = context.getWaitingTimeAtTimeWindow(timeWindow);

        float serviceTime = (context.getLastDrivenDistance()[0] == 0) ?
                context.getCurrentNode().getServiceTime() :
                context.getCurrentNode().getServiceTime() + context.getCurrentNode().getServiceTimeForSite();
        totalServiceTime += serviceTime;

        context.setTimeToTimeWindow(timeWindow);
        context.addToTime(serviceTime);
        context.addToDuration(serviceTime + waitingTime);

        event.setDeparture(context.getRouteVar()[Context.TIME]);
        event.setService(event.getService() + totalServiceTime);
        event.setWaiting(waitingTime);
        event.setDuration(event.getTravelTime() + waitingTime + event.getService());
    }

    private Node findNextCustomerNEW(Node[] route) {
        for (int i = 1; i < route.length; i++) {
            if(route[i].getSiteType() == SiteType.CUSTOMER)
                return route[i];
        }
        return null;
    }

    private void setAmountsToEvent(Event e, float[] amounts, LoadType loadType) {
        e.setLoadType(loadType);

        if(amounts != null) {
            e.setAmounts(amounts);
        }
    }
}
