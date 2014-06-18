package com.jonschang.audio;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

/**
 * Very simple display of the wave form, using the specified format.
 * @author schang
 */
public class AudioSampleCanvas extends Canvas {

	public Color lineColor = Color.red;
	public Color sampleColor = Color.blue;
	public Color playheadColor = Color.green;
	public Color selectColor = Color.black;
	
	private int[] data;
	private int playheadPosition = 0;
	private int selectStartX = 0;
	private int selectEndX = 0;
	
	private boolean firstPaint = true;
	private BufferedImage waveformImage;
	
	private float halfHeight=0;
	private float verticalScale=-1;
	private float horizontalScale=-1;
	
	public AudioSampleCanvas(int[] data) {
		super();
		updateWith(data);
	}
	
	public AudioSampleCanvas(GraphicsConfiguration config, int[] data) {
		super(config);
		updateWith(data);
	}
	
	public void setSelectedRegion(int startViewX, int endViewX) {
		if(data==null) {
			return;
		}
		selectStartX = canvasXToSampleX(startViewX);
		selectEndX = canvasXToSampleX(endViewX);
		this.repaint();
	}
	public void clearSelectedRegion() {
		selectStartX = 0;
		selectEndX = 0;
		this.repaint();
	}
	public int[] getSelectedRegionData() {
		return Arrays.copyOfRange(data, selectStartX, selectEndX);
	}
	public int[] getSelectedRegion() {
		return new int[] {selectStartX,selectEndX};
	}
	
	public void setPlayhead(int dataPosition) {
		this.playheadPosition = dataPosition;
		this.repaint();
	}
	
	public int[] getData() {
		return data;
	}
	
	public void updateWith(int[] data) {
		
		this.data = data;
		if(data==null) {
			repaint();
			return;
		}
		this.waveformImage = renderWaveform(); 
		
		clearSelectedRegion();
		
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		
		super.paint(g);
		
		//boolean firstPaint = this.firstPaint;
		if(this.firstPaint) {
			this.firstPaint = false;
			createBufferStrategy(2);
		}
		
		if(data==null) {
			return;
		}
		
		g.drawImage(waveformImage, 0, 0, null);
		drawPlayhead(g);
		drawSelectRegion(g);
	}
	
	private void updateVerticalScale() {
		int maxAbs = 0;
		for(int i=0; i<data.length; i++) {
			int thisAbs = Math.abs(data[i]);
			if(thisAbs > maxAbs) {
				maxAbs = thisAbs;
			}
		}
		halfHeight = (float)(getHeight())/(float)2.0;
		verticalScale = (float)(halfHeight)/(float)maxAbs;
	}
	
	private void drawPlayhead(Graphics g) {
		int height = getHeight();
		g.setColor(playheadColor);
		int playheadX = (int)(horizontalScale*playheadPosition);
		g.drawLine(playheadX, 0, playheadX, height);
	}
	
	private void drawSelectRegion(Graphics g) {
		if(selectStartX!=selectEndX) {
			int startX = sampleXToCanvasX(selectStartX);
			int endX = sampleXToCanvasX(selectEndX);
			int y = (int)(halfHeight-10.0);
			g.setColor(selectColor);
			g.drawLine(
					startX, y, 
					endX, y
				);
			g.drawLine(startX, y-50, startX, y+50);
			g.drawLine(endX, y-50, endX, y+50);
		}
	}
	
	private BufferedImage renderWaveform() {
		try {
			int height = getHeight();
			int width = getWidth();
			horizontalScale = (float)width/(float)data.length;
			
			if(verticalScale==-1) {
				updateVerticalScale();
			}
			
			BufferedImage img = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = img.createGraphics();
			graphics.setColor(new Color(0,0,0,0));
			graphics.fillRect(0, 0, width, height);
			
			// draw the horizontal line
			graphics.setColor(lineColor);
			graphics.drawLine(0, height/2, width, height/2);
			
			// draw sample data
			int lastSampleX = 0;
			int lastSampleY = height/2;
			graphics.setColor(sampleColor);
			for(int i=0; i<data.length; i++) {
				int sampleX = sampleXToCanvasX(i);
				int sampleY = sampleYToCanvasY(data[i]);
				graphics.drawLine(lastSampleX, lastSampleY, sampleX, sampleY);
				lastSampleX = sampleX;
				lastSampleY = sampleY;
			}
			
			return img;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private int sampleXToCanvasX(int x) {
		return (int)(horizontalScale*x);
	}
	
	private int sampleYToCanvasY(int y) {
		return (int)((verticalScale*y)+(halfHeight));
	}
	
	private int canvasXToSampleX(int x) {
		int width = getWidth();
		horizontalScale = (float)width/(float)data.length;
		return (int)Math.abs(x/horizontalScale);
	}
}
