package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.Node;

public class RouteInfo {

	private final Node depot;
	
	private float loadingServiceTime = 0;
	private float unLoadingServiceTime = 0;
	
	private Amount deliveryAmount = new Amount();
	private Amount pickupAmount = new Amount();
	
	public RouteInfo(Node depot) {
		this.depot = depot;
	}
	
	public void addLoadingServiceTime(float time) {
		loadingServiceTime += time;
	}
	
	public void addUnLoadingServiceTime(float time) {
		unLoadingServiceTime += time;
	}

	public void addPickUpAmount(float[] demand) {
		pickupAmount.add(demand);
	}

	public void addDeliveryAmount(float[] demand) {
		deliveryAmount.add(demand);
	}

	public float getLoadingServiceTime() {
		return loadingServiceTime;
	}

	public float getUnLoadingServiceTime() {
		return unLoadingServiceTime;
	}

	public Node getDepot() {
		return depot;
	}

	public Amount getDeliveryAmount() {
		return deliveryAmount;
	}
}
