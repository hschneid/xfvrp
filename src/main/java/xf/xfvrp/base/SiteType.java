package xf.xfvrp.base;

/** 
 * Copyright (c) 2012-2022 Holger Schneider
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

	DEPOT,
	CUSTOMER,
	REPLENISH,
	PAUSE
}
