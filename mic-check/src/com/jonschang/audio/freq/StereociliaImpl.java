package com.jonschang.audio.freq;


public class StereociliaImpl implements Stereocilia {

	/**
	 * The real portion of the response from calculate().
	 */
	public static int AMPLITUDE_IDX=0;
	
	/**
	 * The imaginary portion of the response from calculate().
	 */
	public static int PHASE_IDX=1;
	
	double sampleRate;
	double frequency;
	
	@Override
	public void setSampleRate(double sampleRate) {
		this.sampleRate = sampleRate;
	}
	
	@Override
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}
	
	@Override
	public double getFrequency() {
		return this.sampleRate;
	}

	@Override
	public double[] calculate(int[] data) {
		
		double ret[] = new double[2];
		double a, x, t, ca, sa;
		
		// This is an important concept in understanding the basis functions; 
		// the frequency parameter, k, is equal to the number of complete cycles 
		// that occur over the N points of the signal.
		double k = frequency;
		if(k < 1) {
			throw new IndexOutOfBoundsException();
		}
		
		for(int n=0; n<data.length; n++) {
			//t = (double)n / sampleRate;
			x = (double)data[n];
			a = ( (2.0 * Math.PI) / sampleRate ) * n * k;
			sa = Math.sin(a);
			ca = Math.cos(a);
			ret[AMPLITUDE_IDX] += x * sa + x * ca;
			ret[PHASE_IDX]     += x * ca - x * sa;
		}
		ret[AMPLITUDE_IDX] = Math.abs(ret[AMPLITUDE_IDX]);
		ret[PHASE_IDX] = Math.abs(ret[PHASE_IDX]);
		return ret;
	}
}
