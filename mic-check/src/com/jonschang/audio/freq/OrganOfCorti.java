package com.jonschang.audio.freq;

import java.util.List;


public interface OrganOfCorti {
	/**
	 * @param sampleRate
	 */
	public void setSampleRate(double sampleRate);
	/**
	 * @param toAdd The Stereocilia configuration to add to the organ.
	 */
	public void addStereocilia(Stereocilia toAdd);
	/**
	 * 
	 */
	public List<Stereocilia> getStereocilia();
	/**
	 * Calculates the signal response for each of the Stereocilia added to the organ.
	 * @param data The signed 16-bit PCM data.  Operates on the whole sample, so 
	 *             the sample should already be trimmed to the smallest possible.
	 * @return The signal response for each Sterocilia, in the order added.
	 */
	public double[][] calculate(int[] data);
}
