package xf.xfvrp.base.exception;

/**
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFVRPException extends Exception {

    private final XFVRPExceptionType type;

    public XFVRPException(XFVRPExceptionType type, String message) {
        super(message);
        this.type = type;
    }

    public XFVRPException(XFVRPExceptionType type, String message, Exception e) {
        super(message, e);
        this.type = type;
    }

    public XFVRPExceptionType getType() {
        return type;
    }
}
