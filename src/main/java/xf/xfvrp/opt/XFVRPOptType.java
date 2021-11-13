package xf.xfvrp.opt;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;

public class XFVRPOptType {

    private final Class<? extends XFVRPOptBase> clazz;

    XFVRPOptType(Class<? extends XFVRPOptBase> clazz) {
        this.clazz = clazz;
    }

    public XFVRPOptBase create() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "no copy of optimization procedure possible", e);
        }
    }
}
