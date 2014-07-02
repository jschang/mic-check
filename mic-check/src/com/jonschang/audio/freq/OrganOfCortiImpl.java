package com.jonschang.audio.freq;

import java.util.ArrayList;
import java.util.List;

public class OrganOfCortiImpl implements OrganOfCorti {

	ArrayList<Stereocilia> stereocilia = new ArrayList<Stereocilia>();
	
	public static OrganOfCorti buildPianoScaleOrganOfCorti(double sampleRate) {
		OrganOfCorti o = new OrganOfCortiImpl();
		for(double i=0; i<88; i++) {
			double freq = Math.pow(2,(i-49.0)/12.0) * 440;
			Stereocilia thisOne = new StereociliaImpl();
			thisOne.setFrequency(freq);
			thisOne.setSampleRate(sampleRate);
			o.addStereocilia(thisOne);
		}
		return o;
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
	public double[][] calculate(int[] data) {
		double[][] ret = new double[stereocilia.size()][2];
		for(int i=0; i<stereocilia.size(); i++) {
			Stereocilia thisOne = stereocilia.get(i);
			ret[i] = thisOne.calculate(data);
		}
		return ret;
	}

}
