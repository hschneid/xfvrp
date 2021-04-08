package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.ShipmentConverter;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.*;

import java.util.ArrayList;
import java.util.List;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * FlexiImporter is the class which holds the concept for flexible and adaptive
 * import of data into the XFVRP suite. The general points are
 * - abstraction of data handling between chaotic user and stable algorithms
 * - abstraction of import sequence (which data is first?)
 * - Easy-to-fill data objects by eliminating import methods (set/add) with static parameters
 * - Handling of vehicles (default or fleet)
 * - Assiging the vehicle priority by sorting the vehicles with their capacities
 * 
 * After import collected data can be accessed XFVRP suite. 
 * 
 * @author hschneid
 *
 */
public class FlexiImporter {

	private final List<InternalDepotData> depotList = new ArrayList<>();
	private final List<InternalCustomerData> customerList = new ArrayList<>();
	private final List<InternalReplenishData> replenishList = new ArrayList<>();
	private final List<InternalVehicleData> vehicleList = new ArrayList<>();

	public final InternalVehicleData defaultVehicle = InternalVehicleData.createDefault();
	public final InternalVehicleData invalidVehicle = InternalVehicleData.createInvalid();

	private InternalDepotData lastDepotData = null;
	private InternalCustomerData lastCustomerData = null;
	private InternalReplenishData lastReplenishData = null;
	private InternalVehicleData lastVehicleData = null;

	/**
	 * Standard constructor
	 * 
	 * Here default vehicle is added to data.
	 */
	public FlexiImporter() {
		vehicleList.add(defaultVehicle);
	}

	/**
	 * Transforms the imported nodes (depots, customers and replenishments)
	 * into an array. This is used by internal algorithm to gather the
	 * imported data.
	 */
	public Node[] getNodes(Vehicle[] vehicles, StatusManager statusManager) {
		int idx = 0;
		Node[] nodes = new Node[depotList.size() + replenishList.size() + customerList.size()];
	
		// Depots
		for (InternalDepotData dep : depotList) {
			nodes[idx] = dep.createDepot(idx);
			idx++;
		}
		
		// Replenishing depots
		for (InternalReplenishData rep : replenishList) {
			nodes[idx] = rep.createReplenishment(idx);
			idx++;
		}
	
		// Customers
		for (InternalCustomerData cust : customerList) {
			nodes[idx] = cust.createCustomer(idx);
			idx++;
		}
		
		// Convert external node data to internal node data
		// Indexing, replacing, transforming
		BlockedVehicleListConverter.convert(nodes, customerList, vehicles, statusManager);
		BlockNameConverter.convert(nodes, customerList);
		BlockPositionConverter.convert(nodes, customerList);
		PresetBlacklistedNodeConverter.convert(nodes, customerList);
		PresetDepotConverter.convert(nodes, customerList, statusManager);
		ShipmentConverter.convert(nodes, customerList);
		
		return nodes;
	}

	/**
	 * If XFVRP suite begins execution, the import process is finished
	 * and all achieved data objects are finalized and inserted into the
	 * data lists.
	 */
	public void finishImport() {
		if(lastDepotData != null)
			depotList.add(lastDepotData);
		if(lastCustomerData != null)
			customerList.add(lastCustomerData);
		if(lastReplenishData != null)
			replenishList.add(lastReplenishData);
		if(lastVehicleData != null)
			vehicleList.add(lastVehicleData);

		lastDepotData = null;
		lastCustomerData = null;
		lastReplenishData = null;
		lastVehicleData = null;
	}

	/**
	 * Achieve a depot data object, where user can import data in any
	 * sequence. The call of this method means, that the last achieved
	 * depot data object is finalized and added to the internal depot list.
	 * 
	 * @return Depot data, where user can import data in any sequence
	 */
	public DepotData getDepotData() {
		if(lastDepotData != null)
			depotList.add(lastDepotData);

		lastDepotData = new InternalDepotData();

		return lastDepotData;
	}

	/**
	 * Achieve a customer data object, where user can import data in any
	 * sequence. The call of this method means, that the last achieved
	 * customer data object is finalized and added to the internal customer list.
	 * 
	 * @return Customer data, where user can import data in any sequence
	 */
	public CustomerData getCustomerData() {
		if(lastCustomerData != null)
			customerList.add(lastCustomerData);

		lastCustomerData = new InternalCustomerData();

		return lastCustomerData;
	}

	/**
	 * Returns a data object, which describes a replenishment node. At
	 * a replenishment node, the loaded amounts are cleared.
	 *
	 * Replenishment nodes are optional.
	 */
	public ReplenishData getReplenishData() {
		if(lastReplenishData != null)
			replenishList.add(lastReplenishData);

		lastReplenishData = new InternalReplenishData();

		return lastReplenishData;
	}

	/**
	 * Achieve a vehicle data object, where user can import data in any
	 * sequence. The call of this method means, that the last achieved
	 * vehicle data object is finalized and added to the internal vehicle list.
	 * 
	 * By achieving a vehicle data object, the default vehicle is put out of
	 * vehicle list. So the default vehicle parameter have to be announced in
	 * specific own vehicle data.
	 * 
	 * @return Container data, where user can import data in any sequence
	 */
	public VehicleData getVehicleData() {
		// Zur Sicherheit wird das default-vehicle entfernt, auch wenn es
		// offensichtlich nicht mehr im Set drin ist.
		vehicleList.remove(defaultVehicle);

		if(lastVehicleData != null) {
			vehicleList.add(lastVehicleData);
		}

		lastVehicleData = new InternalVehicleData();

		return lastVehicleData;
	}

	/**
	 * Clears all internal data lists and reset the internal fields.
	 */
	public void clear() {
		depotList.clear();
		customerList.clear();
		vehicleList.clear();

		lastDepotData = null;
		lastCustomerData = null;
		lastVehicleData = null;
	}

	/**
	 * Clears all imported customers
	 */
	public void clearCustomers() {
		customerList.clear();
	}

	/**
	 * Clears all imported depots.
	 */
	public void clearDepots() {
		depotList.clear();
	}

	/**
	 * Removes all inserted vehicles and reset the planning parameters to default.
	 * (See setCapacity() and setMaxRouteDuration() )
	 */
	public void clearVehicles() {
		vehicleList.clear();
		vehicleList.add(defaultVehicle);
	}

	/**
	 * 
	 * @return the collected depots
	 */
	public List<InternalDepotData> getDepotList() {
		return depotList;
	}

	/**
	 * 
	 * @return the collected customers
	 */
	public List<InternalCustomerData> getCustomerList() {
		return customerList;
	}

	/**
	 * 
	 * @return the collected customers
	 */
	public List<InternalReplenishData> getReplenishList() {
		return replenishList;
	}

	/**
	 * 
	 * @return the collected vehicles
	 */
	public Vehicle[] getVehicles() throws XFVRPException {
		Vehicle[] vehicles = new Vehicle[vehicleList.size()];

		int idx = 0;
		for (InternalVehicleData veh : vehicleList) {
			vehicles[idx] = veh.createVehicle(idx);
			idx++;
		}

		return vehicles;
	}
}
