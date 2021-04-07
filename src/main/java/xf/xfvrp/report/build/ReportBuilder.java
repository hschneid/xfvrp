package xf.xfvrp.report.build;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.XFVRPSolution;
import xf.xfvrp.opt.evaluation.*;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;

/**
 * Copyright (c) 2012-present Holger Schneider
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

    public Report getReport(XFVRPSolution solution) {
        XFVRPModel model = solution.getModel();
        Report rep = new Report(solution.getSolution(), model);

        Context context = ContextBuilder.build(model);
        for (Node[] route : solution.getSolution()) {
            // Feasibility check
            FeasibilityAnalzer.checkFeasibility(route);

            if(route.length <= 2) {
                continue;
            }

            RouteReport routeReport = getRouteReport(route, context);
            rep.add(routeReport);
        }

        return rep;
    }

    private RouteReport getRouteReport(Node[] route, Context context) {
        RouteReport routeReport = new RouteReport(context.getModel().getVehicle());

        route = ActiveNodeAnalyzer.getActiveNodes(route);
        context.setRouteInfos(RouteInfoBuilder.build(route, context.getModel()));

        context.setCurrentNode(route[0]);
        routeReport.add(
                beginRoute(route, context)
        );
        context.setNextNode(route[0]);
        for (int i = 1; i < route.length; i++) {
            context.setNextNode(route[i]);

            Event event = createEvent(context);

            drive(context, routeReport);

            fillEvent(event, context);

            routeReport.add(event);
        }

        return routeReport;
    }

    private Event beginRoute(Node[] route, Context context) {
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
                LoadType.PICKUP
        );

        return e;
    }

    private void drive(Context context, RouteReport report) {
        float[] dist = context.getModel().getDistanceAndTime(context.getLastNode(), context.getCurrentNode());
        context.drive(dist);

        // check max driving time per shift restrictions
        if(context.getDrivingTime() >= context.getModel().getVehicle().maxDrivingTimePerShift) {
            context.resetDrivingTime();

            Node waitingNode = context.getCurrentNode().copy();
            waitingNode.setSiteType(SiteType.PAUSE);
            Event e = new Event(waitingNode); // Driver Pause
            e.setDuration(context.getModel().getVehicle().waitingTimeBetweenShifts);
            e.setLoadType(LoadType.PAUSE);

            report.add(e);
        }
    }

    private Event createEvent(Context context) {
        Event e = new Event(context.getCurrentNode());

        switch(context.getCurrentNode().getSiteType()) {
            case REPLENISH:
                setAmountsToEvent(e,
                        context.getRouteInfo().getDeliveryAmount().getAmounts(),
                        LoadType.PICKUP
                );
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
            if (amounts.length > 0) e.setAmount(amounts[0]);
            if (amounts.length > 1) e.setAmount2(amounts[1]);
            if (amounts.length > 2) e.setAmount3(amounts[2]);
        }
    }

    //////////////////////////////////
//
//
//    public Report getReport(XFVRPSolution solution) {
//        ReportBuildContext context = new ReportBuildContext(solution);
//        XFVRPModel model = solution.getModel();
//        Report rep = new Report(solution.getSolution(), model);
//
//        Node[] giantRoute = context.getGiantRoute();
//
//        // Wenn die Giant Route Kunden enthält
//        if(findNextCustomer(giantRoute, 0) != -1) {
//            createRouteDepotServiceMap(context);
//
//            Node lastNode = giantRoute[0];
//            Node currNode;
//
//            context.setDelay(0);
//            context.setDrivingTime(0);
//
//            Node lastDepot = lastNode;
//
//            RouteReport tRep = createNewRoute(lastNode, giantRoute[findNextCustomer(giantRoute, 0)], context);
//
//            boolean routeHasCustomers = false;
//            for (int i = 1; i < giantRoute.length; i++) {
//                currNode = giantRoute[i];
//
//                if(currNode == null)
//                    throw new IllegalStateException("customer is null at position in giant tour: "+i);
//
//                // Der letzte Knoten ist ein DEPOT gewesen
//                if(currNode.getSiteType() == SiteType.DEPOT) {
//                    // Bewerte Kante zu?ck zum Ausgangsdepot in lastDepot
//                    eval(lastNode, lastDepot, tRep, context);
//
//                    // Beende alte Tour durch Einf?gen in Report und Zur?cksetzen der laufenden Variabeln
//                    if(routeHasCustomers) {
//                        rep.add(tRep);
//                    }
//
//                    context.setDelay(0);
//                    context.setDrivingTime(0);
//                    routeHasCustomers = false;
//
//                    // Beginne eine neue Tour, leere Touren werden hier ?bersprungen
//                    i = skipToNextDepot(i, giantRoute);
//                    currNode = giantRoute[i];
//
//                    // Wenn es noch weitere Kunden gibt, erzeuge weitere RouteReports.
//                    int nextCustomerIdx = findNextCustomer(giantRoute, i);
//                    nextCustomerIdx = (nextCustomerIdx == -1) ? i : nextCustomerIdx;
//                    tRep = createNewRoute(currNode, giantRoute[nextCustomerIdx], context);
//
//                    lastDepot = currNode;
//                } else {
//                    // Bewerte Schritt vom letzten Knoten zu diesem Knoten
//                    eval(lastNode, currNode, tRep, context);
//                    if(currNode.getSiteType() == SiteType.CUSTOMER)
//                        routeHasCustomers = true;
//                }
//
//                lastNode = currNode;
//            }
//        }
//
//        return rep;
//    }
//
//    /**
//     * Evaluates the edge from lastNode to currNode and
//     * updates the progressive values.
//     */
//    private void eval(Node lastNode, Node currNode, RouteReport tRep, ReportBuildContext context) {
//        // Der letzte Knoten ist ein KUNDE gewesen
//        // Tour fortführen
//
//        float[] metric = context.getDistanceAndTime(lastNode, currNode);
//        context.addTime(metric[1]);
//        context.addDrivingTime(metric[1]);
//
//        // Driving time restriction
//        // A pause of waitingTimeBetweenShifts minutes after maxDrivingTimePerShift minutes
//        if(context.getDrivingTime() >= tRep.getVehicle().maxDrivingTimePerShift) {
//            context.setDrivingTime(0);
//            context.addTime(tRep.getVehicle().waitingTimeBetweenShifts);
//
//            Node waitingNode = currNode.copy();
//            waitingNode.setSiteType(SiteType.PAUSE);
//            Event e = new Event(waitingNode); // Driver Pause
//            e.setDuration(tRep.getVehicle().waitingTimeBetweenShifts);
//            e.setLoadType(LoadType.PAUSE);
//            tRep.add(e);
//        }
//
//        // Depot service time at end depot
//        float depotServiceTime = 0;
//        if(context.getParameter().isWithUnloadingTimeAtDepot() && currNode.getSiteType() == SiteType.DEPOT) {
//            depotServiceTime = context.getRouteDepotServiceMap()[currNode.getDepotId()][1];
//            context.addTime(depotServiceTime);
//        }
//
//        float[] demand = currNode.getDemand();
//        Event e = new Event(currNode);
//        e.setAmount(demand[0]);
//        if (demand.length > 1) e.setAmount2(demand[1]);
//        if (demand.length > 2) e.setAmount3(demand[2]);
//        e.setDistance(metric[0]);
//        e.setTravelTime(metric[1]);
//        e.setArrival(context.getTime());
//
//        context.setDelay(Math.max(0, context.getTime() - currNode.getTimeWindow(context.getTime())[1]));
//        float waitingTime = (lastNode.getSiteType() != SiteType.DEPOT) ?
//                Math.max(
//                        0,
//                        currNode.getTimeWindow(context.getTime())[0] - context.getTime()
//                ) : 0;
//        float serviceTime = (metric[0] == 0) ? currNode.getServiceTime() : currNode.getServiceTime() + currNode.getServiceTimeForSite();
//        context.setTime(Math.max(context.getTime(), currNode.getTimeWindow(context.getTime())[0]));
//
//        context.addTime(serviceTime);
//
//        e.setDeparture(context.getTime());
//        e.setService(serviceTime + depotServiceTime);
//        e.setWaiting(waitingTime);
//        e.setLoadType(currNode.getLoadType());
//        e.setDelay(context.getDelay());
//        e.setDuration(e.getTravelTime() + waitingTime + e.getService());
//
//        tRep.add(e);
//    }
//
//    /**
//     * Creates a new route report.
//     */
//    private RouteReport createNewRoute(Node depotNode, Node nextNode, ReportBuildContext context) {
//        if (depotNode.getSiteType() != SiteType.DEPOT)
//            throw new IllegalStateException("Found unexpected site type for depot ("+depotNode.getSiteType().toString()+")");
//
//        final float depotOpeningTime = depotNode.getTimeWindow(0)[0];
//
//        RouteReport tRep = new RouteReport(context.getVehicle());
//
//        float nextNodeTime = 0;
//        if(nextNode != null)
//            nextNodeTime = context.getMetricTime(depotNode, nextNode);
//
//        float loadingTime = 0;
//        if(context.getParameter().isWithLoadingTimeAtDepot())
//            loadingTime = context.getRouteDepotServiceMap()[depotNode.getDepotId()][0];
//
//        context.setTime(Math.max(
//                depotOpeningTime + loadingTime,
//                nextNode.getTimeWindow(context.getTime())[0] - nextNodeTime
//        ));
//
//        Event e = new Event(depotNode);
//        e.setAmount(0);
//        e.setAmount2(0);
//        e.setAmount3(0);
//        e.setDistance(0);
//        e.setArrival(depotOpeningTime);
//        e.setDeparture(context.getTime());
//        e.setService(loadingTime);
//        e.setWaiting(context.getTime() - depotOpeningTime - loadingTime);
//        e.setDelay(0);
//        e.setLoadType(null);
//        e.setTravelTime(0);
//        tRep.add(e);
//        return tRep;
//    }
//
//    private int findNextCustomer(Node[] giantRoute, int pos) {
//        for (int i = pos + 1; i < giantRoute.length; i++) {
//            if(giantRoute[i].getSiteType() == SiteType.CUSTOMER)
//                return i;
//        }
//
//        return -1;
//    }
//
//    /**
//     * Skips depot nodes if empty routes are available.
//     *
//     * @param pos Position of the last found depot
//     * @param giantRoute Current giant tour
//     * @return Position of the next valid depot
//     */
//    private int skipToNextDepot(int pos, Node[] giantRoute) {
//        int nextDepot = pos;
//        for (int j = pos + 1; j < giantRoute.length; j++) {
//            if(giantRoute[j].getSiteType() == SiteType.DEPOT)
//                nextDepot = j;
//            else
//                break;
//        }
//        return nextDepot;
//    }
//
//    private void createRouteDepotServiceMap(ReportBuildContext context) {
//        XFVRPParameter parameter = context.getParameter();
//
//        if(parameter.isWithLoadingTimeAtDepot() || parameter.isWithUnloadingTimeAtDepot()) {
//            Node[] giantRoute = context.getGiantRoute();
//            int maxDepotId = 0;
//            for (int i = 0; i < giantRoute.length; i++) {
//                maxDepotId = Math.max(maxDepotId, giantRoute[i].getDepotId());
//            }
//            float[][] list = new float[maxDepotId + 1][];
//
//            float pickupServiceTime = 0;
//            float deliverySericeTime = 0;
//
//            int lastDepotId = giantRoute[0].getDepotId();
//            for (int i = 1; i < giantRoute.length; i++) {
//                final SiteType siteType = giantRoute[i].getSiteType();
//                if(siteType == SiteType.DEPOT) {
//                    list[lastDepotId] = new float[]{deliverySericeTime, pickupServiceTime};
//                    pickupServiceTime = deliverySericeTime = 0;
//                    lastDepotId = giantRoute[i].getDepotId();
//                } else if(siteType == SiteType.CUSTOMER) {
//                    final LoadType loadType = giantRoute[i].getLoadType();
//
//                    if(loadType == LoadType.PICKUP)
//                        pickupServiceTime += giantRoute[i].getServiceTime();
//                    else if(loadType == LoadType.DELIVERY)
//                        deliverySericeTime += giantRoute[i].getServiceTime();
//                    else
//                        throw new IllegalStateException("Found unexpected load type ("+loadType.toString()+")");
//                } else
//                    throw new IllegalStateException("Found unexpected site type ("+siteType.toString()+")");
//            }
//            context.setRouteDepotServiceMap(list);
//        }
//    }
//
}
