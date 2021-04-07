package xf.xfvrp.base.exception;

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
