package xf.xfvrp.opt.evaluation;

import java.util.Arrays;
import java.util.Objects;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;

public class FeasibilityAnalzer {

	public static void checkFeasibility(Node[] giantRoute) {
		// Es kann hier leere Touren geben, weil es gelöschte Touren geben kann! :-(
		if(giantRoute == null || giantRoute.length == 0)
			throw new IllegalStateException("Empty route is not allowed to report");
		if(giantRoute[0].getSiteType() != SiteType.DEPOT)
			throw new IllegalStateException("First node in giant route is not a depot.");
		if(giantRoute[giantRoute.length - 1].getSiteType() != SiteType.DEPOT)
			throw new IllegalStateException("Last node in giant route is not a depot.");
		if(Arrays.stream(giantRoute).filter(Objects::isNull).count() > 0)
			throw new IllegalStateException("Route contains NullPointer!");
	}
}
