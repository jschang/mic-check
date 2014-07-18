package com.jonschang.audio.freq;

import java.awt.Canvas;
import java.awt.GraphicsConfiguration;

public class SpectrogramCanvas extends Canvas {
	
	private OrganOfCorti.Response sampleData;
	private OrganOfCorti organOfCorti;
	
	public SpectrogramCanvas(GraphicsConfiguration config, OrganOfCorti organ, OrganOfCorti.Response data) {
		super(config);
		this.organOfCorti = organ;
		updateWith(data);
	}
	
	public SpectrogramCanvas(OrganOfCorti organ, OrganOfCorti.Response data) {
		super();
		this.organOfCorti = organ;
		updateWith(data);
	}
	
	public void updateWith(OrganOfCorti.Response data) {
		
		this.sampleData = data;
		
		repaint();
	}
}
