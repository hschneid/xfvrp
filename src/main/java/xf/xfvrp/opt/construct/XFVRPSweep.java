package xf.xfvrp.opt.construct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.opt.XFVRPLPBridge;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.XFVRP2Opt;


/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Sweep optimization procedure.
 * 
 * @author hschneid
 *
 */
public class XFVRPSweep extends XFVRPOptBase {

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Node[] execute(Node[] giantTour) {
		final XFVRPOptBase opt2 = new XFVRP2Opt();
		final List<Node> nodeList = new ArrayList<>();
		final Node depot = giantTour[0];

		Set<String> loadingFootprint = null;

		List<double[]> thetaList = new ArrayList<>();
		{
			int idx = 0;
			for (int i = 1; i < giantTour.length; i++) {
				if(giantTour[i].getSiteType() == SiteType.DEPOT)
					continue;

				double[] d = new double[2];

				d[0] = idx++;
				d[1] = Math.atan2(giantTour[i].getYlat(),giantTour[i].getXlong());

				// We want theta in [0 , 2pi]
				// If y<0, add 2pi
				if(giantTour[i].getYlat() < 0)
					d[1] += 2*Math.PI;

				thetaList.add(d);
				nodeList.add(giantTour[i]);
			}

			Collections.sort(thetaList, new Comparator<double[]>() {
				@Override
				public int compare(double[] arg0, double[] arg1) {
					return (int)((arg0[1] - arg1[1])*10000);
				}
			});
		}

		// We will start at a random place in this list and then "wrap around"
		// Pick a starting point 
		int start = new Random(1234).nextInt(thetaList.size());

		int depotId = 0;

		// Erzeuge Giant-Tour
		List<Node> giantList = new ArrayList<>();
		giantList.add(Util.createIdNode(depot, depotId++));

		// Erzeuge Tour
		List<Node> tour = new ArrayList<>();
		tour.add(Util.createIdNode(depot, depotId++));

		// Eröffne eine Tour
		tour.add(nodeList.get((int)thetaList.get(start)[0]));
		tour.add(Util.createIdNode(depot, depotId++));
		
		int currentIdx = (start + 1) % thetaList.size();
		while(currentIdx != start) {
			Node insertNode = nodeList.get((int)thetaList.get(currentIdx)[0]);

			tour.add(tour.size() - 1, insertNode);

			Node[] gT = tour.toArray(new Node[tour.size()]);
			
			Quality q = check(gT);
			if(q.getPenalty() == 0) {
				// Efficient Load Prüfung 
				XFVRPLPBridge.check(gT, loadingFootprint, model, q);
				loadingFootprint = XFVRPLPBridge.getFootprints(gT);
			}

			if(q.getPenalty() > 0) {
				tour.remove(tour.size() - 2);

				// Setze bisherige Tour in GiantTour ein
				Node lastDepot = tour.remove(0);
				giantList.addAll(tour);

				// Mache neue Tour auf
				tour.clear();

				tour.add(lastDepot);
				tour.add(insertNode);
				tour.add(Util.createIdNode(depot, depotId++));
			} else if((currentIdx % 10) == 0) {
				gT = opt2.execute(gT, model, statusManager);
				tour = new ArrayList<>(Arrays.asList(gT));
			}

			currentIdx = (currentIdx + 1) % thetaList.size();
		}

		// Den letzten Rest der Tour in GiantTour eintragen
		tour.remove(0);
		giantList.addAll(tour);

		return Util.normalizeRoute(giantList.toArray(new Node[0]), model);
	}
}
