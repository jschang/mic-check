package com.jonschang.audio;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.LinkedList;

import com.jonschang.audio.AudioSampleCanvas.Annotator;
import com.jonschang.audio.AudioSampleCanvas.Context;

public class LocalMinMaxAnnotator implements Annotator {

	public Color circleColor = Color.red;
	public int circleRadius = 3;
	public int totalCount = 0;
	public LinkedList<Datum> data = new LinkedList<Datum>();
	
	public static class Datum {
		int shift;
		int period;
		public Datum(int shift,int period) {
			this.shift = shift;
			this.period = period;
		}
	}
	
	private int SAMPLE_TOL = 0;
	private int SAMPLE_DEV_TOL = 0;
	private enum Direction {
		DOWN,
		UP
	};
	private int lastSample;
	private int lastSampleMaxMin = -1;
	private int lastSampleIdx = 0;
	private Direction lastDir = null;
	
	@Override
	public void onAudioSampleIteration(Graphics2D g, Context ctx, int idx) {
		g.setColor(circleColor);
		if(lastSample==-1) {
			lastSample = ctx.data[idx];
			return;
		}
		int thisSample = ctx.data[idx];
		Direction thisDir = evalDir(lastSample, thisSample);
		if(lastDir==null) {
			lastDir = thisDir;
			return;
		}
		if(thisDir!=lastDir) {
			int evalIdx = idx-1;
			for(int i=1; i<=SAMPLE_TOL; i++) {
				if( ctx.data.length<evalIdx+(i+1) || (evalIdx-1)==-1 ) {
					return;
				}
				try {
					if( thisDir != evalDir(ctx.data[evalIdx+(i-1)],ctx.data[evalIdx+i]) ) {
						return;
					}
				} catch(ArrayIndexOutOfBoundsException e) {
					;
				}
			}
			if(lastSampleMaxMin==-1 || Math.abs(lastSampleMaxMin-lastSample)>SAMPLE_DEV_TOL) {
				int x = ctx.sampleXToCanvasX(evalIdx);
				int y = ctx.sampleYToCanvasY(lastSample);
				g.drawOval(x,y,circleRadius,circleRadius);
				if(totalCount>0) {
					data.add(new Datum(Math.abs(lastSampleMaxMin-lastSample),Math.abs(evalIdx-lastSampleIdx)));
				}
				totalCount++;
				lastSampleMaxMin = lastSample;
				lastSampleIdx = evalIdx;
			}
		}
		lastDir = thisDir;
		lastSample = thisSample;
	}
	
	public int[] lastData() {
		if(data.size()==0) { 
			return null;
		}
		int bufferSize = 0;
		for(Datum datum : data) {
			bufferSize += datum.period;
		}
		int[] buffer = new int[bufferSize];
		int direction = 1;
		int lastSample = 0;
		int thisIdx = 0;
		for(Datum datum : data) {
			direction = direction==1 ? -1 : 1;
			int sampleY = direction * (int)( (float)datum.shift / (float)datum.period );
			if(sampleY >= Integer.MAX_VALUE || sampleY <= Integer.MIN_VALUE) {
				sampleY = 0;
			}
			for(int idx=0, to=datum.period; idx < to; idx++) {
				lastSample = buffer[thisIdx] = lastSample + sampleY;
				thisIdx++;
			}
		}
		return buffer;
	}
	
	@Override
	public void onAudioSampleDone(Graphics2D g, Context data) {
		System.out.printf("Total Max/Min %d in %d samples\n", totalCount, data.data.length);
	}
	
	@Override
	public void onAudioSampleStart(Graphics2D g, Context data) {
		totalCount = 0;
		lastSampleMaxMin = -1;
		lastSampleIdx = 0;
		this.data.clear();
		SAMPLE_DEV_TOL = 0;//(int) (data.maxSample * .05);
	}
	
	private Direction evalDir(int lastSample, int thisSample) {
		if(lastSample>thisSample) {
			return Direction.DOWN;
		} else {
			return Direction.UP;
		}
	}
}
