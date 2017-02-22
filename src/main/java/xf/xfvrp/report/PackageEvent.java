package xf.xfvrp.report;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public class PackageEvent {

	private String id = "";
	private int x = -1;
	private int y = -1;
	private int z = -1;
	private int stackId = -1;
	private boolean isInvalid = false;
	
	private float usedVolumeInContainer = 0;
	private int nbrStacksInContainer = 0;
	
	/**
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * 
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getX() {
		return x;
	}
	
	/**
	 * 
	 * @param x
	 */
	public void setX(int x) {
		this.x = x;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getY() {
		return y;
	}
	
	/**
	 * 
	 * @param y
	 */
	public void setY(int y) {
		this.y = y;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getZ() {
		return z;
	}
	
	/**
	 * 
	 * @param z
	 */
	public void setZ(int z) {
		this.z = z;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getStackId() {
		return stackId;
	}
	
	/**
	 * 
	 * @param stackId
	 */
	public void setStackId(int stackId) {
		this.stackId = stackId;
	}

	/**
	 * @param isInvalid the isInvalid to set
	 */
	public void setInvalid(boolean isInvalid) {
		this.isInvalid = isInvalid;
	}

	/**
	 * @return the isInvalid
	 */
	public boolean isInvalid() {
		return isInvalid;
	}

	/**
	 * @return the usedVolumeInContainer
	 */
	public float getUsedVolumeInContainer() {
		return usedVolumeInContainer;
	}

	/**
	 * @param usedVolumeInContainer the usedVolumeInContainer to set
	 */
	public void setUsedVolumeInContainer(float usedVolumeInContainer) {
		this.usedVolumeInContainer = usedVolumeInContainer;
	}

	/**
	 * @return the nbrStacksInContainer
	 */
	public int getNbrStacksInContainer() {
		return nbrStacksInContainer;
	}

	/**
	 * @param nbrStacksInContainer the nbrStacksInContainer to set
	 */
	public void setNbrStacksInContainer(int nbrStacksInContainer) {
		this.nbrStacksInContainer = nbrStacksInContainer;
	}
}
