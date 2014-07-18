package com.jonschang.audio.freq;

import java.util.ArrayList;
import java.util.List;

public class OrganOfCortiImpl implements OrganOfCorti {

	ArrayList<Stereocilia> stereocilia = new ArrayList<Stereocilia>();
	
	public static OrganOfCorti buildExpScaleOrganOfCorti(double sampleRate, int number) {
		OrganOfCorti o = new OrganOfCortiImpl();
		double m = Math.sqrt((sampleRate/2)/Math.pow(number,2));
		for(int i=1; i<number; i++) {
			double x = (double)i;
			double freq = Math.max(Math.pow(m*x, 2),1.0);
			Stereocilia thisOne = new SuperStereocilia();
			System.out.println("Freq:"+freq+"Hz");
			thisOne.setFrequency(freq);
			thisOne.setSampleRate(sampleRate);
			o.addStereocilia(thisOne);
		}
		return o;
	}
	
	public static OrganOfCorti buildLogScaleOrganOfCorti(double sampleRate, int number) {
		OrganOfCorti o = new OrganOfCortiImpl();
		double stretchX = (sampleRate/400);
		for(int i=0; i<number; i++) {
			double x = (double)i;
			double freq = Math.pow( Math.E, (x+number) / (number/3.0) ) * stretchX;
			Stereocilia thisOne = new GenericStereocilia();
			thisOne.setFrequency(freq);
			thisOne.setSampleRate(sampleRate);
			o.addStereocilia(thisOne);
		}
		return o;
	}
	
	public static OrganOfCorti buildPianoScaleOrganOfCorti(double sampleRate) {
		OrganOfCorti o = new OrganOfCortiImpl();
		for(double i=0; i<88; i+=1) {
			double freq = Math.pow(2,(i-49.0)/12.0) * 440;
			Stereocilia thisOne = new GenericStereocilia();
			thisOne.setFrequency(freq);
			thisOne.setSampleRate(sampleRate);
			o.addStereocilia(thisOne);
		}
		return o;
	}
	
	public static OrganOfCorti buildLinearScaleOrganOfCorti(double sampleRate, double freqLow, double freqHigh, int number) {
		OrganOfCorti o = new OrganOfCortiImpl();
		double freq = freqLow;
		double stepFreq = (freqHigh-freqLow) / (double)number;
		for(double i=0; i<number; i++) {
			Stereocilia thisOne = new GenericStereocilia();
			thisOne.setFrequency(freq);
			thisOne.setSampleRate(sampleRate);
			o.addStereocilia(thisOne);
			freq += stepFreq;
		}
		return o;
	}
	
	public static OrganOfCorti buildLogScaleOrganOfCorti(double sampleRate, double freqLow, double freqHigh, int number) {
		throw new UnsupportedOperationException("Not supported yet.");
		/*OrganOfCorti o = new OrganOfCortiImpl();
		double freq = freqLow;
		for(double i=0; i<number; i++) {
			Stereocilia thisOne = new StereociliaImpl();
			thisOne.setFrequency(freq);
			thisOne.setSampleRate(sampleRate);
			o.addStereocilia(thisOne);
		}
		return o;*/
	}
	
	@Override
	public void setSampleRate(double sampleRate) {
		for(Stereocilia thisOne : stereocilia) {
			thisOne.setSampleRate(sampleRate);
		}
	}

	@Override
	public void addStereocilia(Stereocilia toAdd) {
		if(!stereocilia.contains(toAdd)) {
			stereocilia.add(toAdd);
		}
	}
	
	@Override
	public List<Stereocilia> getStereocilia() {
		return new ArrayList<Stereocilia>(stereocilia);
	}

	@Override
	public Response calculate(int[] data) {
		final double[] reals = new double[stereocilia.size()];
		final double[] imags = new double[stereocilia.size()];
		double maxReal = 0;
		double maxImag = 0;
		for(int i=0; i<stereocilia.size(); i++) {
			Stereocilia thisOne = stereocilia.get(i);
			double resp[] = thisOne.calculate(data);
			reals[i] = resp[GenericStereocilia.AMPLITUDE_IDX];
			imags[i] = resp[GenericStereocilia.PHASE_IDX];
			maxReal = Math.max(Math.abs(reals[i]),maxReal);
			maxImag = Math.max(Math.abs(imags[i]),maxImag);
		}
		for(int i=0; i<stereocilia.size(); i++) {
			reals[i] = reals[i] / maxReal;
			imags[i] = imags[i] / maxImag;
		}
		final double realMult = maxReal;
		final double imagMult = maxImag;
		return new OrganOfCorti.Response() {
			@Override
			public double[] getAmplitudes() {
				return reals;
			}
			@Override
			public double[] getPhases() {
				return imags;
			}
			@Override
			public double getAmplitudeMultiplier() {
				return realMult;
			}
			public double getPhaseMultiplier() {
				return imagMult;
			}
		};
	}

}
