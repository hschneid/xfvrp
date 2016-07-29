package xf.xfpdp.opt;

import java.util.Arrays;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;

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
public class PreCheckMethod {

	private Context context = new Context();
	private Context storedContext;
	private XFVRPModel model;

	private float[] workAmountVal;
	private float[] workTime = new float[1];

	/**
	 * 
	 * @param model
	 */
	public PreCheckMethod(XFVRPModel model) {
		this.model = model;

		if(model.getNbrOfNodes() > model.getNbrOfDepots() + model.getNbrOfReplenish()) {
			Node customer = model.getNodeArr()[model.getNbrOfDepots() + model.getNbrOfReplenish() + 1];
			context.amountVal = new float[customer.getDemand().length * 3];
			workAmountVal = new float[customer.getDemand().length * 3];
		}
	}
	
	/**
	 * 
	 * @param node
	 */
	public void addDepot(Node node) {
		context.lastNode = node;
		context.time[0] = node.getTimeWindow(0)[0];
	}

	/**
	 * 
	 * @param node
	 */
	public boolean add(Node node) {
		boolean isValid = true;
		isValid &= checkCapactiy(node, context.amountVal);
		isValid &= checkTimeWindow(node, context.lastNode, context.time);
		
		context.lastNode = node;
		
		return isValid;
	}

	/**
	 * 
	 * @param newNode
	 * @param nextNode
	 * @return false means invalid solution, true is good
	 */
	public boolean addVirtual(Node newNode, Node nextNode) {
		System.arraycopy(context.amountVal, 0, workAmountVal, 0, context.amountVal.length);
		if(!checkCapactiy(newNode, workAmountVal)) return false;
		
		workTime[0] = context.time[0];
		if(!checkTimeWindow(newNode, context.lastNode, workTime)) return false;
		if(!checkTimeWindow(nextNode, newNode, workTime)) return false;
		
		return true;
	}
	
	/**
	 * 
	 * @param newNode
	 * @param nextNode
	 * @return false means invalid solution, true is good
	 */
	public boolean addVirtual(Node newNode, Node[] route, int startPos) {
		System.arraycopy(context.amountVal, 0, workAmountVal, 0, context.amountVal.length);
		if(!checkCapactiy(newNode, workAmountVal)) return false;
		
		workTime[0] = context.time[0];
		if(!checkTimeWindow(newNode, context.lastNode, workTime)) return false;
		if(!checkTimeWindow(route[startPos], newNode, workTime)) return false;
		
		if(!checkDuration(route, startPos + 1, workTime)) return false;
		
		return true;
	}

	private boolean checkDuration(Node[] route, int pos, float[] workTime) {
		float maxDuration = model.getVehicle().maxRouteDuration;
		float duration = workTime[0];
		float time = workTime[0];
		for (int p = pos; p < route.length; p++) {
			Node currNode = route[p];
			if(currNode.getSiteType() == SiteType.DEPOT)
				break;
			
			float[] metric = model.getDistanceAndTime(route[p - 1], currNode);
			duration += metric[1];
			time += metric[1];
			
			float[] tw = currNode.getTimeWindow(time);

			// Waiting times
			float waitingTime = (time < tw[0]) ? tw[0] - time : 0;
			// Service times
			float serviceTime = (metric[0] == 0) ? currNode.getServiceTime() : currNode.getServiceTime() + currNode.getServiceTimeForSite();
			
			time = (time > tw[0]) ? time : tw[0];
			time += serviceTime;
			duration += serviceTime + waitingTime;
			
			if(duration > maxDuration)
				return false;
		}
		
		return false;
	}

	/**
	 * 
	 */
	public void store() {
		storedContext = context.copy();
	}

	/**
	 * 
	 */
	public void reload() {
		context = storedContext;
	}
	
	public void reset() {
		context = new Context();
		context.amountVal = new float[workAmountVal.length];
	}

	/**
	 * 
	 * @param node
	 * @param amountVal
	 * @return
	 */
	private boolean checkCapactiy(Node node, float[] amountVal) {
		Vehicle vehicle = model.getVehicle();
	
		for (int i = 0; i < amountVal.length / 3; i++) {
			float delivery = (node.getLoadType() == LoadType.DELIVERY) ? node.getDemand()[i] : 0;
			float pickup = (node.getLoadType() == LoadType.PICKUP) ? node.getDemand()[i] : 0;
			float unloadOnRoute = (pickup < 0) ? pickup : 0; 
			
			amountVal[i*3] = Math.max(amountVal[i*3] + pickup, amountVal[i*3] + delivery) + unloadOnRoute;
			amountVal[i*3+1] += pickup;
			amountVal[i*3+2] += delivery;

			for (int j = 0; j < 3; j++)
				if(amountVal[i*3+j] > vehicle.capacity[i])
					return false;
		}

		return true;
	}

	/**
	 * 
	 * @param node
	 * @param lastNode
	 * @param time
	 * @return
	 */
	private boolean checkTimeWindow(Node node, Node lastNode, float[] time) {
		Vehicle vehicle = model.getVehicle();
		
		float[] metric = model.getDistanceAndTime(lastNode, node);
		time[0] += metric[1];
		
		float[] tw = node.getTimeWindow(time[0]);
		if(time[0] > tw[1]) 
			return false;

		// Wenn der letzte Knoten ein Depot war, wird die
		// Wartezeit nicht mitberechnet, die er h�tte sp�ter abfahren k�nnen
		float waiting = (time[0] < tw[0]) ? tw[0] - time[0] : 0;

		// Check maxWaiting penalty
		if(waiting > vehicle.maxWaitingTime)
			return false;

		float serviceTime = (metric[0] == 0) ? node.getServiceTime() : node.getServiceTime() + node.getServiceTimeForSite();
		time[0] = (time[0] > tw[0]) ? time[0] : tw[0];
		time[0] += serviceTime;
		
		return true;
	}

	/**
	 * 
	 * @author hschneid
	 *
	 */
	private class Context {

		public Node lastNode;
		public float[] amountVal;
		public float[] time = new float[1];

		/**
		 * 
		 * @return
		 */
		public Context copy() {
			Context newC = new Context();
			newC.lastNode = lastNode;
			newC.time = time;
			newC.amountVal = Arrays.copyOf(amountVal, amountVal.length);

			return newC;
		}
	}

}
