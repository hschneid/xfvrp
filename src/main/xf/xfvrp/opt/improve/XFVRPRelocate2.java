package xf.xfvrp.opt.improve;

import java.util.Arrays;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;

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
public class XFVRPRelocate2 extends XFVRPOptImpBase {

	private boolean found;
	private int bestI = -1; 
	private int bestJ = -1;
	private Quality bestResult;

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Node[] giantTour, Quality bestResult) {
		final Set<String> loadingFootprint = getLoadingFootprint(giantTour);

		if(model.getNbrOfDepots() > 1)
			throw new UnsupportedOperationException(XFVRPRelocate2.class.getName()+" supports no multi depot");

		boolean f = false;

		Quality bR = bestResult;
		Node[] gT = Arrays.copyOf(giantTour, giantTour.length);

		int bI = -1; int bJ = -1;

		for (int i = 1; i < giantTour.length; i++) {
			for (int j = 1; j < gT.length - 1; j++) {
				// Start darf kein Depot sein
				if(gT[j].getSiteType() == SiteType.DEPOT)
					continue;
				if(j == i)
					continue;

				move(gT, i, j);
				
				Quality result = check(giantTour, loadingFootprint);
				if(result != null) {
					bR = result;
					bI = i;
					bJ = j;
					f = true;
					return result;
				}					

				if(i > j)
					move(gT, j, i - 1);
				else
					move(gT, j+1, i);
			}
		}

		// sync
		setResult(bR, bI, bJ, f);

		if(!found)
			return null;

		int k = bestI;
		int i = bestJ;
		move(giantTour, k, i);

		// Update Datenstrukturen
		bestResult = check(giantTour);

		return bestResult;
	}

	/**
	 * 
	 * @param bR
	 * @param bI
	 * @param bJ
	 * @param f
	 */
	private void setResult(Quality bR, int bI, int bJ, boolean f) {
		if(f && bR.getFitness() < bestResult.getFitness()) {
			this.bestResult = bR;
			this.bestI = bI;
			this.bestJ = bJ;
			this.found = f;
		}
	}
}
