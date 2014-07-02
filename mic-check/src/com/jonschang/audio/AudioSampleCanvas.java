package com.jonschang.audio;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Very simple display of the wave form, using the specified format.
 * @author schang
 */
public class AudioSampleCanvas extends Canvas {

	public static interface Annotator {
		public void onAudioSampleIteration(Graphics2D g, Context data, int position);
		public void onAudioSampleDone(Graphics2D g, Context data);
		public void onAudioSampleStart(Graphics2D g, Context data);
	}
	
	public Color lineColor = Color.red;
	public Color sampleColor = Color.blue;
	public Color playheadColor = Color.green;
	public Color selectColor = Color.black;
	
	private int playheadPosition = 0;
	private int selectStartX = 0;
	private int selectEndX = 0;

	private boolean firstPaint = true;
	private BufferedImage waveformImage;
	
	private Context context = new Context();
	
	public static class Context {
		public int[] data;
		public float height=0;
		public float width=0;
		public float halfHeight=0;
		public float verticalScale=-1;
		public float horizontalScale=-1;
		public int maxSample=0;
		public int sampleXToCanvasX(int x) {
			return (int)(horizontalScale*x);
		}
		public int sampleYToCanvasY(int y) {
			return (int)((verticalScale*y)+(halfHeight));
		}
		public int canvasXToSampleX(int x) {
			horizontalScale = (float)width/(float)data.length;
			return (int)Math.abs(x/horizontalScale);
		}
	}
	
	public AudioSampleCanvas(int[] data) {
		super();
		updateWith(data);
	}
	
	public AudioSampleCanvas(GraphicsConfiguration config, int[] data) {
		super(config);
		updateWith(data);
	}
	
	public LinkedList<Annotator> annotators = new LinkedList<Annotator>();
	
	public void setSelectedRegion(int startViewX, int endViewX) {
		if(context.data==null) {
			return;
		}
		selectStartX = context.canvasXToSampleX(startViewX);
		selectEndX = context.canvasXToSampleX(endViewX);
		this.repaint();
	}
	public void clearSelectedRegion() {
		selectStartX = 0;
		selectEndX = 0;
		this.repaint();
	}
	public int[] getSelectedRegionData() {
		return Arrays.copyOfRange(context.data, selectStartX, selectEndX);
	}
	public int[] getSelectedRegion() {
		return new int[] {selectStartX,selectEndX};
	}
	
	public void setPlayhead(int dataPosition) {
		this.playheadPosition = dataPosition;
		this.repaint();
	}
	
	public int[] getData() {
		return context.data;
	}
	
	public void updateWith(int[] data) {
		
		context.data = data;
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
		
		if(context.data==null) {
			return;
		}
		
		g.drawImage(waveformImage, 0, 0, null);
		drawPlayhead(g);
		drawSelectRegion(g);
	}
	
	private void updateVerticalScale() {
		int maxAbs = 0;
		for(int i=0; i<context.data.length; i++) {
			int thisAbs = Math.abs(context.data[i]);
			if(thisAbs > maxAbs) {
				maxAbs = thisAbs;
			}
		}
		context.maxSample = maxAbs;
		context.halfHeight = (float)(getHeight())/(float)2.0;
		context.verticalScale = (float)(context.halfHeight)/(float)maxAbs;
	}
	
	private void drawPlayhead(Graphics g) {
		int height = getHeight();
		g.setColor(playheadColor);
		int playheadX = (int)(context.horizontalScale*playheadPosition);
		g.drawLine(playheadX, 0, playheadX, height);
	}
	
	private void drawSelectRegion(Graphics g) {
		if(selectStartX!=selectEndX) {
			int startX = context.sampleXToCanvasX(selectStartX);
			int endX = context.sampleXToCanvasX(selectEndX);
			int y = (int)(context.halfHeight-10.0);
			g.setColor(selectColor);
			g.drawLine(
					startX, y, 
					endX, y
				);
			g.drawLine(startX, y-100, startX, y+100);
			g.drawLine(endX, y-100, endX, y+100);
		}
	}
	
	private BufferedImage renderWaveform() {
		try {
			int height = (int) (context.height = getHeight());
			int width = (int) (context.width = getWidth());
			context.horizontalScale = (float)width/(float)context.data.length;
			if(context.verticalScale==-1) {
				updateVerticalScale();
			}
			
			BufferedImage img = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = img.createGraphics();
			graphics.setColor(new Color(0,0,0,0));
			graphics.fillRect(0, 0, width, height);
			
			// draw the horizontal line
			graphics.setColor(lineColor);
			graphics.drawLine(0, height/2, width, height/2);
			
			for(Annotator annotator : annotators) {
				annotator.onAudioSampleStart(graphics, context);
			}
			
			// draw sample data
			int lastSampleX = 0;
			int lastSampleY = height/2;
			for(int i=0; i<context.data.length; i++) {
				int sampleX = context.sampleXToCanvasX(i);
				int sampleY = context.sampleYToCanvasY(context.data[i]);
				graphics.setColor(sampleColor);
				graphics.drawLine(lastSampleX, lastSampleY, sampleX, sampleY);
				for(Annotator annotator : annotators) {
					annotator.onAudioSampleIteration(graphics, context, i);
				}
				lastSampleX = sampleX;
				lastSampleY = sampleY;
			}
			
			for(Annotator annotator : annotators) {
				annotator.onAudioSampleDone(graphics, context);
			}
			
			return img;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
