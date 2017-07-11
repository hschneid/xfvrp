package xf.xfvrp.opt.evaluation

import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType

class TestNode {

	int globalIdx = 0;
	String externID = "";
	SiteType siteType = SiteType.CUSTOMER;
	float xlong = 0;
	float ylat = 0;
	int geoId = 0;
	float[] demand = [1];
	float[][] timeWindow = [[0, 9999999]];
	float serviceTime = 0;
	float serviceTimeForSite = 0;
	LoadType loadType = LoadType.DELIVERY;
	int presetBlockPos = 0;
	int presetBlockRank = 0;
	String shipID = "";
	int nbrOfPackages = 1;
	int heightOfPackage = 1;
	int widthOfPackage = 1;
	int lengthOfPackage = 1;
	float weightOfPackage = 1;
	float loadBearingOfPackage = 1;
	int stackingGroupOfPackage = 1;
	int containerTypeOfPackage = 1;
	
	Node getNode() {
		return new Node(
			globalIdx,
			externID,
			siteType,
			xlong,
			ylat,
			geoId,
			demand,
			timeWindow,
			serviceTime,
			serviceTimeForSite,
			loadType,
			presetBlockPos,
			presetBlockRank,
			shipID,
			nbrOfPackages,
			heightOfPackage,
			widthOfPackage,
			lengthOfPackage,
			weightOfPackage,
			loadBearingOfPackage,
			stackingGroupOfPackage,
			containerTypeOfPackage
			);
	}
}
