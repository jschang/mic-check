package com.jonschang.audio.freq;

public class SuperStereocilia extends GenericStereocilia {
	
	@Override
	public double[] calculate(int[] data) {
		double freq = getFrequency();
		if(freq<3.0) {
			return super.calculate(data);
		}
		double[] ret = {0.0,0.0};
		for(double i=freq-2, j=freq+2; i<=j; i++) {
			setFrequency(i);
			double[] resp = super.calculate(data);
			ret[0] = (ret[0] + resp[0])/2.0;
			ret[1] = (ret[1] + resp[1])/2.0;
		}
		setFrequency(freq);
		return ret;
	}
}
