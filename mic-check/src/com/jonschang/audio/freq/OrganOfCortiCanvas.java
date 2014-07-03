package com.jonschang.audio.freq;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.util.ArrayList;
import java.util.Arrays;

public class OrganOfCortiCanvas extends Canvas {

	private int[] sampleData;
	private boolean firstPaint = true;
	private int dftWidth = 400;
	private OrganOfCorti organOfCorti;
	private double maxSample;
	
	public OrganOfCortiCanvas(GraphicsConfiguration config, OrganOfCorti organ, int[] data) {
		super(config);
		this.organOfCorti = organ;
		updateWith(data);
	}
	
	public OrganOfCortiCanvas(OrganOfCorti organ, int[] data) {
		super();
		this.organOfCorti = organ;
		updateWith(data);
	}

	public void updateWith(int[] data) {
		
		this.sampleData = data;
		
		if(data==null) {
			repaint();
			return;
		} 
		
		for(int y : data) {
			maxSample = Math.max((double)y, maxSample);
		}
		
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		
		super.paint(g);
		
		if(this.firstPaint) {
			this.firstPaint = false;
			createBufferStrategy(2);
		}
		
		if(sampleData==null) {
			return;
		}
		
		double[][] dftRes;
		int startIdx = 0;
		double canvasWidth = (1.0*getWidth()/sampleData.length) * dftWidth;
		double canvasHeight = (1.0*getHeight()/organOfCorti.getStereocilia().size());
		
		// first evaluate frequency response for each sample section
		double max = 1.0;
		double[][] values = new double[(sampleData.length/dftWidth)+1][organOfCorti.getStereocilia().size()];
		startIdx=0;
		for(int i=0; startIdx<this.sampleData.length; i++, startIdx+=this.dftWidth) {
			int[] dftData = Arrays.copyOfRange(this.sampleData, startIdx, startIdx+this.dftWidth);
			dftRes = organOfCorti.calculate(dftData);
			int j=0;
			for(double[] value : dftRes) {
				values[i][j] = value[0];
				max = Math.max(value[0], max);
				j++;
			}
		}
		
		// draw the response for each sample across the canvas
		int j = 0;
		int halfHeight = (int) (canvasHeight/2);
		for(double[] thisSegment : values) {
			
			// transform startIdx to canvas coords
			double canvasX = (1.0*getWidth()/sampleData.length) * j;
			
			//for(int d=thisSegment.length-1, i=0; d>=0; d--, i++) {
			for(int d=0, i=0; d<thisSegment.length; d++, i++) {
				
				double response = thisSegment[d];
				double canvasY = ( 1.0 * getHeight() / thisSegment.length ) * i;
				
				g.setColor(Color.black);
				g.drawLine(0, (int)canvasY+halfHeight, getWidth(), (int)canvasY+halfHeight);
				
				float v = (float)Math.abs(response);
				//v = v > .5f ? 1.0f : 0.0f;
				v = (float)max - v;
				g.setColor(
						new Color(
								v,v,v,1.0f
							)
						);
				g.fillRect((int)canvasX, (int)canvasY, (int)canvasWidth, (int)canvasHeight);
			}
			
			j+=dftWidth;
		}
	}
}