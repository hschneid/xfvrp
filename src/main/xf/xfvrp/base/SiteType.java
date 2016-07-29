package xf.xfvrp.base;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This is the enumeration of site types.
 * 
 * DEPOTs are nodes where routes can start or end.
 * CUSTOMERs are visited and has demands, which have
 * to be satisfied.
 * 
 * @author hschneid
 *
 */
public enum SiteType {

	DEPOT("DEPOT"),
	CUSTOMER("CUSTOMER"),
	REPLENISH("REPLENISH"),
	PAUSE("PAUSE");
	
	private String name;
	
	/**
	 * 
	 * @param name
	 */
	private SiteType(String name) {
		this.name = name;
	}
	
	/**
	 * Checks whether the given name is equal to this site type name.
	 * 
	 * @param name
	 * @return
	 */
	public boolean equals(String name) {
		return this.name.equals(name);
	}
}
