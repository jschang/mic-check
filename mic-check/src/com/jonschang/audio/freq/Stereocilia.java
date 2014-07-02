package com.jonschang.audio.freq;


public interface Stereocilia {
	
	/**
	 * Sets the sample rate of data passed into calculate
	 * @param audioFormat
	 */
	public void setSampleRate(double sampleRate);
	
	/**
	 * The frequency to pay attention to.
	 * @param frequency
	 */
	public void setFrequency(double frequency);
	
	public double getFrequency();
	
	/**
	 * @param data The sample data to calculate.
	 * @return The percentage of signal contributed to in this frequency.
	 */
	public double[] calculate(int[] data);
}
