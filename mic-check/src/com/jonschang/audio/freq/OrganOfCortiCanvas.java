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
	private int dftWidth = 500;
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
		double max = 0;
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
		this.setBackground(Color.black);
		for(double[] thisSegment : values) {
			
			// transform startIdx to canvas coords
			double canvasX = (1.0*getWidth()/sampleData.length) * j;
			
			// evenly distribute results from each cilia
			int i = 0;
			for(double response : thisSegment) {
				double canvasY = ( 1.0 * getHeight() / thisSegment.length ) * i;
				float v = (float)Math.abs(response)/(float)max;
				g.setColor(
						new Color(
								v,v,v,1.0f
							)
						);
				g.fillRect((int)canvasX, (int)canvasY, (int)canvasWidth, (int)canvasHeight);
				g.setColor(Color.GRAY);
				g.drawLine((int)canvasX, 0, (int)canvasX, (int)canvasHeight);
				g.drawLine((int)(canvasX+canvasWidth), 0, (int)(canvasX+canvasWidth), (int)canvasHeight);
				i++;
			}
			
			j+=dftWidth;
		}
	}
}