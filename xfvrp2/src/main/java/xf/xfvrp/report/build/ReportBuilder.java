package xf.xfvrp.report.build;

import xf.xfvrp.base.*;
import xf.xfvrp.opt.XFVRPSolution;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;

public class ReportBuilder {

    public Report getReport(XFVRPSolution solution) {
        ReportBuildContext context = new ReportBuildContext(solution);
        XFVRPModel model = solution.getModel();
        Report rep = new Report(solution.getSolution(), model);

        Node[] giantRoute = context.getGiantRoute();

        // Wenn die Giant Route Kunden enthält
        if(findNextCustomer(giantRoute, 0) != -1) {
            createRouteDepotServiceMap(context);

            Node lastNode = giantRoute[0];
            Node currNode;

            context.setDelay(0);
            context.setDrivingTime(0);

            Node lastDepot = lastNode;

            RouteReport tRep = createNewRoute(lastNode, giantRoute[findNextCustomer(giantRoute, 0)], context);

            boolean routeHasCustomers = false;
            for (int i = 1; i < giantRoute.length; i++) {
                currNode = giantRoute[i];

                if(currNode == null)
                    throw new IllegalStateException("customer is null at position in giant tour: "+i);

                // Der letzte Knoten ist ein DEPOT gewesen
                if(currNode.getSiteType() == SiteType.DEPOT) {
                    // Bewerte Kante zu?ck zum Ausgangsdepot in lastDepot
                    eval(lastNode, lastDepot, tRep, context);

                    // Beende alte Tour durch Einf?gen in Report und Zur?cksetzen der laufenden Variabeln
                    if(routeHasCustomers) {
                        rep.add(tRep);
                    }

                    context.setDelay(0);
                    context.setDrivingTime(0);
                    routeHasCustomers = false;

                    // Beginne eine neue Tour, leere Touren werden hier ?bersprungen
                    i = skipToNextDepot(i, giantRoute);
                    currNode = giantRoute[i];

                    // Wenn es noch weitere Kunden gibt, erzeuge weitere RouteReports.
                    int nextCustomerIdx = findNextCustomer(giantRoute, i);
                    nextCustomerIdx = (nextCustomerIdx == -1) ? i : nextCustomerIdx;
                    tRep = createNewRoute(currNode, giantRoute[nextCustomerIdx], context);

                    lastDepot = currNode;
                } else {
                    // Bewerte Schritt vom letzten Knoten zu diesem Knoten
                    eval(lastNode, currNode, tRep, context);
                    if(currNode.getSiteType() == SiteType.CUSTOMER)
                        routeHasCustomers = true;
                }

                lastNode = currNode;
            }
        }

        return rep;
    }

    /**
     * Evaluates the edge from lastNode to currNode and
     * updates the progressive values.
     */
    private void eval(Node lastNode, Node currNode, RouteReport tRep, ReportBuildContext context) {
        // Der letzte Knoten ist ein KUNDE gewesen
        // Tour fortführen

        float[] metric = context.getDistanceAndTime(lastNode, currNode);
        context.addTime(metric[1]);
        context.addDrivingTime(metric[1]);

        // Driving time restriction
        // A pause of waitingTimeBetweenShifts minutes after maxDrivingTimePerShift minutes
        if(context.getDrivingTime() >= tRep.getVehicle().maxDrivingTimePerShift) {
            context.setDrivingTime(0);
            context.addTime(tRep.getVehicle().waitingTimeBetweenShifts);

            Node waitingNode = currNode.copy();
            waitingNode.setSiteType(SiteType.PAUSE);
            Event e = new Event(waitingNode); // Driver Pause
            e.setDuration(tRep.getVehicle().waitingTimeBetweenShifts);
            e.setLoadType(LoadType.PAUSE);
            tRep.add(e);
        }

        // Depot service time at end depot
        float depotServiceTime = 0;
        if(context.getParameter().isWithUnloadingTimeAtDepot() && currNode.getSiteType() == SiteType.DEPOT) {
            depotServiceTime = context.getRouteDepotServiceMap()[currNode.getDepotId()][1];
            context.addTime(depotServiceTime);
        }

        float[] demand = currNode.getDemand();
        Event e = new Event(currNode);
        e.setAmount(demand[0]);
        if (demand.length > 1) e.setAmount2(demand[1]);
        if (demand.length > 2) e.setAmount3(demand[2]);
        e.setDistance(metric[0]);
        e.setTravelTime(metric[1]);
        e.setArrival(context.getTime());

        context.addDelay(Math.max(0, - currNode.getTimeWindow(context.getTime())[1]));
        float waitingTime = (lastNode.getSiteType() != SiteType.DEPOT) ?
                Math.max(
                        0,
                        currNode.getTimeWindow(context.getTime())[0] - context.getTime()
                ) : 0;
        float serviceTime = (metric[0] == 0) ? currNode.getServiceTime() : currNode.getServiceTime() + currNode.getServiceTimeForSite();
        context.setTime(Math.max(context.getTime(), currNode.getTimeWindow(context.getTime())[0]));

        context.addTime(serviceTime);

        e.setDeparture(context.getTime());
        e.setService(serviceTime + depotServiceTime);
        e.setWaiting(waitingTime);
        e.setLoadType(currNode.getLoadType());
        e.setDelay(context.getDelay());
        e.setDuration(e.getTravelTime() + waitingTime + e.getService());

        tRep.add(e);
    }

    /**
     * Creates a new route report.
     */
    private RouteReport createNewRoute(Node depotNode, Node nextNode, ReportBuildContext context) {
        if (depotNode.getSiteType() != SiteType.DEPOT)
            throw new IllegalStateException("Found unexpected site type for depot ("+depotNode.getSiteType().toString()+")");

        final float depotOpeningTime = depotNode.getTimeWindow(0)[0];

        RouteReport tRep = new RouteReport(context.getVehicle());

        float nextNodeTime = 0;
        if(nextNode != null)
            nextNodeTime = context.getMetricTime(depotNode, nextNode);

        float loadingTime = 0;
        if(context.getParameter().isWithLoadingTimeAtDepot())
            loadingTime = context.getRouteDepotServiceMap()[depotNode.getDepotId()][0];

        context.setTime(Math.max(
                depotOpeningTime + loadingTime,
                nextNode.getTimeWindow(context.getTime())[0] - nextNodeTime
        ));

        Event e = new Event(depotNode);
        e.setAmount(0);
        e.setAmount2(0);
        e.setAmount3(0);
        e.setDistance(0);
        e.setArrival(depotOpeningTime);
        e.setDeparture(context.getTime());
        e.setService(loadingTime);
        e.setWaiting(context.getTime() - depotOpeningTime - loadingTime);
        e.setDelay(0);
        e.setLoadType(null);
        e.setTravelTime(0);
        tRep.add(e);
        return tRep;
    }

    private int findNextCustomer(Node[] giantRoute, int pos) {
        for (int i = pos + 1; i < giantRoute.length; i++) {
            if(giantRoute[i].getSiteType() == SiteType.CUSTOMER)
                return i;
        }

        return -1;
    }

    /**
     * Skips depot nodes if empty routes are available.
     *
     * @param pos Position of the last found depot
     * @param giantRoute Current giant tour
     * @return Position of the next valid depot
     */
    private int skipToNextDepot(int pos, Node[] giantRoute) {
        int nextDepot = pos;
        for (int j = pos + 1; j < giantRoute.length; j++) {
            if(giantRoute[j].getSiteType() == SiteType.DEPOT)
                nextDepot = j;
            else
                break;
        }
        return nextDepot;
    }

    private void createRouteDepotServiceMap(ReportBuildContext context) {
        Node[] giantRoute = context.getGiantRoute();
        XFVRPParameter parameter = context.getParameter();

        if(parameter.isWithLoadingTimeAtDepot() || parameter.isWithUnloadingTimeAtDepot()) {
            int maxDepotId = 0;
            for (int i = 1; i < giantRoute.length; i++)
                maxDepotId = Math.max(maxDepotId, giantRoute[i].getDepotId());
            float[][] list = new float[maxDepotId + 1][];

            float pickupServiceTime = 0;
            float deliverySericeTime = 0;

            int lastDepotId = giantRoute[0].getDepotId();
            for (int i = 1; i < giantRoute.length; i++) {
                final SiteType siteType = giantRoute[i].getSiteType();
                if(siteType == SiteType.DEPOT) {
                    list[lastDepotId] = new float[]{deliverySericeTime, pickupServiceTime};
                    pickupServiceTime = deliverySericeTime = 0;
                    lastDepotId = giantRoute[i].getDepotId();
                } else if(siteType == SiteType.CUSTOMER) {
                    final LoadType loadType = giantRoute[i].getLoadType();

                    if(loadType == LoadType.PICKUP)
                        pickupServiceTime += giantRoute[i].getServiceTime();
                    else if(loadType == LoadType.DELIVERY)
                        deliverySericeTime += giantRoute[i].getServiceTime();
                    else
                        throw new IllegalStateException("Found unexpected load type ("+loadType.toString()+")");
                } else
                    throw new IllegalStateException("Found unexpected site type ("+siteType.toString()+")");
            }
            context.setRouteDepotServiceMap(list);
        }
    }
}
