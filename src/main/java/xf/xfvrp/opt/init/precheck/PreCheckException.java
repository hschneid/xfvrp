package xf.xfvrp.opt.init.precheck;

public class PreCheckException extends Exception {

	public PreCheckException(String msg) {
		super(msg);
	}
	
	public PreCheckException(String msg, Throwable t) {
		super(msg, t);
	}

}
