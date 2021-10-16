package xf.xfvrp.base.metric;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.opt.XFVRPOptBase;

public enum Metrics {

    EUCLEDIAN(EucledianMetric.class);

    private final Class clazz;

    Metrics(Class clazz) {
        this.clazz = clazz;
    }

    public Metric get() {
        try {
            return (Metric) Class.forName(clazz.getName()).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "no copy of metric is possible", e);
        }
    }
}
