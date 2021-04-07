package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;

import java.util.Arrays;
import java.util.Objects;

public class FeasibilityAnalzer {

	public static void checkFeasibility(Node[] route) {
		// Es kann hier leere Touren geben, weil es gelöschte Touren geben kann! :-(
		if(route == null || route.length == 0)
			throw new IllegalStateException("Empty route is not allowed to report");
		if(route[0].getSiteType() != SiteType.DEPOT)
			throw new IllegalStateException("First node in giant route is not a depot.");
		if(route[route.length - 1].getSiteType() != SiteType.DEPOT)
			throw new IllegalStateException("Last node in giant route is not a depot.");
		if(Arrays.stream(route).filter(Objects::isNull).count() > 0)
			throw new IllegalStateException("Route contains NullPointer!");
	}
}
