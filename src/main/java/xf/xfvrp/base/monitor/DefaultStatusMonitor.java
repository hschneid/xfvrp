package xf.xfvrp.base.monitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * The default status monitor, which prints the messages to the 
 * command line.
 * 
 * @author hschneid
 *
 */
public class DefaultStatusMonitor implements StatusMonitor {

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xfvrp.base.monitor.StatusMonitor#getMessage(de.fhg.iml.vlog.xfvrp.base.monitor.StatusCode, java.lang.String)
	 */
	@Override
	public void getMessage(StatusCode code, String message) {
		System.out.println(
				"("+ LocalDateTime.now().format(formatter) +")"+
				"["+code.name()+"] "+message);
	}
	
}
