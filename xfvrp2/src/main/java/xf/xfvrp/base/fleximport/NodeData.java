package xf.xfvrp.base.fleximport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class NodeData {

	protected String externID = "";
	protected int geoId = -1;

	protected float xlong = 0;
	protected float ylat = 0;
	protected List<float[]> timeWindowList = new ArrayList<>();

	protected float open1 = 0;
	protected float close1 = Integer.MAX_VALUE;
	protected float open2 = 0;
	protected float close2 = Integer.MAX_VALUE;


	/**
	 * Internal method to check the time window list.
	 * 
	 * If no time windows are given, a default time window
	 * is inserted.
	 * 
	 * This method is to clear data structure for internal use. 
	 */
	protected void checkTimeWindows() {
		if(timeWindowList.size() < 2)
			if(open1 != 0 || close1 != Integer.MAX_VALUE)
				timeWindowList.add(new float[]{open1, close1});
		if(timeWindowList.size() < 2)
			if(open2 != 0 || close2 != Integer.MAX_VALUE)
				timeWindowList.add(new float[]{open2, close2});

		// Bigger time window overrides smaller ones.
		Collections.sort(timeWindowList, new Comparator<float[]>() {
			@Override
			public int compare(float[] o1, float[] o2) {
				return (int)((o1[0] - o2[0]) * 1000f);
			}
		});
		
		// If no time window is given, insert the default time window
		if(timeWindowList.size() == 0)
			timeWindowList.add(new float[]{0, Integer.MAX_VALUE});
	}
	
	/**
	 * @return the externID
	 */
	public String getExternID() {
		return externID;
	}

	/**
	 * @return the geoId
	 */
	public int getGeoId() {
		return geoId;
	}

	/**
	 * @return the xlong
	 */
	public float getXlong() {
		return xlong;
	}

	/**
	 * @return the ylat
	 */
	public float getYlat() {
		return ylat;
	}

	/**
	 * @return the timeWindowList
	 */
	public List<float[]> getTimeWindowList() {
		return timeWindowList;
	}
}
