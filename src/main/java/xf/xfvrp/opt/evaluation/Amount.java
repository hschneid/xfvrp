package xf.xfvrp.opt.evaluation;

import java.util.stream.IntStream;

public class Amount {

	private float[] amounts;
	
	public Amount() {
	}

	public void add(float[] otherAmount) {
		init(otherAmount);
		
		IntStream.range(0, amounts.length).forEach(i -> amounts[i] += otherAmount[i]);
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
