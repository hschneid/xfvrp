package xf.xfvrp.opt.init.precheck;

public class PreCheckException extends Exception {

	private static final long serialVersionUID = 1L;

	public PreCheckException(String msg) {
		super(msg);
	}
	
	public PreCheckException(String msg, Throwable t) {
		super(msg, t);
	}

}
