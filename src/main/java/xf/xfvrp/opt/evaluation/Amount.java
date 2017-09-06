package xf.xfvrp.opt.evaluation;

public class Amount {

	private float[] amounts;
	
	public Amount() {
	}

	public void add(float[] otherAmount) {
		init(otherAmount);
		
		for (int i = 0; i < amounts.length; i++) {
			amounts[i] += otherAmount[i];
		}
	}
	
	public float getAmount(int idx) {
		return (amounts != null && idx < amounts.length) ? amounts[idx] : 0;
	}

	public float[] getAmounts() {
		return amounts;
	}

	public void setAmounts(float[] amounts) {
		this.amounts = amounts;
	}
	
	private void init(float[] otherAmount) {
		if(amounts == null)
			amounts = new float[otherAmount.length];
	}

	public boolean hasAmount() {
		return (amounts != null);
	}
}
