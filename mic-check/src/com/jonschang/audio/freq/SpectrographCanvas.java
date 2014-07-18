package com.jonschang.audio.freq;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class SpectrographCanvas extends Canvas {

	public Color playheadColor = Color.green;
	
	private int[] sampleData;
	private boolean firstPaint = true;
	private int dftWidth = (int)((20000.0/1000.0)*20.0);
	private OrganOfCorti organOfCorti;
	private double maxSample;
	private BufferedImage spectrographImage;
	private int playheadPosition;
	
	public SpectrographCanvas(GraphicsConfiguration config, OrganOfCorti organ, int[] data) {
		super(config);
		this.organOfCorti = organ;
		updateWith(data);
	}
	
	public SpectrographCanvas(OrganOfCorti organ, int[] data) {
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
		
		spectrographImage = renderSpectrograph();
		
		repaint();
	}
	
	public void setPlayhead(int dataPosition) {
		this.playheadPosition = dataPosition;
		this.repaint();
	}
	
	public void setPlayheadX(int x) {
		playheadPosition = getSamplePos(x);
		this.repaint();
	}
	
	public OrganOfCorti.Response getResponseAtPlayhead() {
		int samplePos = playheadPosition;
		int[] dftData = Arrays.copyOfRange(this.sampleData, samplePos, (int)(samplePos+(this.dftWidth*2)));
		return organOfCorti.calculate(dftData);
	}
	
	public OrganOfCorti.Response getResponseAtX(int x) {
		int samplePos = getSamplePos(x);
		int[] dftData = Arrays.copyOfRange(this.sampleData, samplePos, (int)(samplePos+(this.dftWidth*2)));
		return organOfCorti.calculate(dftData);
	}
	
	private int getSamplePos(int x) {
		double width = (double)getWidth();
		double len = sampleData.length;
		return (int)Math.floor( (len/width) * x );
	}
	
	private int getScreenPos(int x) {
		double width = (double)getWidth();
		double len = sampleData.length;
		return (int)Math.floor( (width/len) * x );
	}

	@Override
	public void paint(Graphics g) {
		
		super.paint(g);
		
		if(this.firstPaint) {
			this.firstPaint = false;
			createBufferStrategy(2);
		}

		g.drawImage(spectrographImage, 0, 0, null);
		drawPlayhead(g);
	}
	
	private void drawPlayhead(Graphics g) {
		if(playheadPosition==-1 || this.sampleData==null)
			return;
		int height = getHeight()-1;
		double width = (double)getWidth();
		g.setColor(playheadColor);
		int playheadX = getScreenPos((int)Math.floor(playheadPosition/dftWidth)*dftWidth);
		int playheadWidth = (int)(dftWidth * ( width / (double)sampleData.length ));
		g.drawRect(playheadX, 0, playheadWidth, height);
	}
		
	public BufferedImage renderSpectrograph() {
		
		BufferedImage img = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		
		if(sampleData==null) {
			return img;
		}
		
		int responsesExpected = (sampleData.length/dftWidth);
		int numCilia = organOfCorti.getStereocilia().size();
		OrganOfCorti.Response[] dftRes = new OrganOfCorti.Response[responsesExpected];
		int startIdx = 0;
		double canvasWidth = (1.0*getWidth()/sampleData.length) * dftWidth;
		double canvasHeight = (1.0*getHeight()/organOfCorti.getStereocilia().size());
		
		// first evaluate frequency response for each sample section
		startIdx=0;
		double maxAmp = 0;
		for(int i=0; startIdx<this.sampleData.length-this.dftWidth; i++, startIdx+=this.dftWidth) {
			int[] dftData = Arrays.copyOfRange(this.sampleData, startIdx, (int)(startIdx+(this.dftWidth)));
			dftRes[i] = organOfCorti.calculate(dftData);
			maxAmp = Math.max(dftRes[i].getAmplitudeMultiplier(),maxAmp);
		}
		
		// draw the response for each sample across the canvas
		int j = 0;
		int halfHeight = (int) (canvasHeight/2);
		double[] responses = new double[dftRes[0].getAmplitudes().length];
		double[] lastResponses;
		for(OrganOfCorti.Response thisSegment : dftRes) {
			
			// transform startIdx to canvas coords
			double canvasX = (1.0*getWidth()/sampleData.length) * j;
			
			for(int d=numCilia-1, i=0; d>=0; d--, i++) {
				if(thisSegment==null || thisSegment.getAmplitudes()==null)
					continue;
				double response = Math.abs(thisSegment.getAmplitudes()[d]);
				responses[i] = response;
			}
			Arrays.sort(responses);
			for(int k=0, l=responses.length-1; k < responses.length / 2; k++, l--) {
				double t = responses[k];
				responses[k] = responses[l];
				responses[l] = t;
			}
			
			for(int d=numCilia-1, i=0; d>=0; d--, i++) {

				if(thisSegment==null || thisSegment.getAmplitudes()==null)
					continue;
				
				double response = Math.abs(thisSegment.getAmplitudes()[d]);
				double canvasY = ( 1.0 * getHeight() / numCilia ) * i;
				
				float v = 0;
				v = (float)(response*thisSegment.getAmplitudeMultiplier()/maxAmp);
				if(v<.01) {
					continue;
				}
				v = 1.0f - v;
				if(responses[0]==response) {
					g.setColor(new Color(1.0f,0.0f,0.0f,1.0f));
				} else if(responses[1]==response) {
					g.setColor(new Color(0.0f,0.0f,1.0f,1.0f));
				} else if(responses[2]==response) {
					g.setColor(new Color(0.0f,0.75f,0.0f,1.0f));
				} else {
					g.setColor(new Color(v,v,v,1.0f));
					continue;
				}
				g.fillRect((int)canvasX, (int)canvasY, (int)canvasWidth, (int)canvasHeight);
			}
			
			lastResponses = responses;
			j+=dftWidth;
		}
		
		return img;
	}
}