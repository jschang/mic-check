package com.jonschang.audio;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;


public final class AudioUtil {
	
	private final static boolean DEBUG = false;
	
	private AudioUtil() {
	}
	
	public static interface EventListener {
		public void updatePosition(int x);
		public void stopped();
	}
	
	public static OutputStream getFileOutputStream(String file) throws AudioUtil.Exception {
		try {
			return new BufferedOutputStream(new FileOutputStream(new File(file)));
		} catch(java.lang.Exception e) {
			throw new AudioUtil.Exception(e);
		}
	}
	
	public static InputStream getFileInputStream(String file) throws AudioUtil.Exception {
		try {
			return new BufferedInputStream(new FileInputStream(new File(file)));
		} catch(java.lang.Exception e) {
			throw new AudioUtil.Exception(e);
		}
	}
	
	public static AudioFormat getCurrentPreferredAudioFormat() throws AudioUtil.Exception {
		AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
		float sampleRate = 20000;
	    int sampleSizeInBits = 16;
	    int channels = 1;
	    boolean bigEndian = false;
	    AudioFormat sourceFormat = AudioUtil.getAudioFormat(encoding,sampleRate,sampleSizeInBits,channels,bigEndian,sampleSizeInBits/8);
	    if(DEBUG) {
		    System.out.println(
					String.format("%d %d %d %s %s", 
							(int)sourceFormat.getSampleRate(), 
							(int)sourceFormat.getSampleSizeInBits(), 
							(int)sourceFormat.getChannels(),
							(String)sourceFormat.getEncoding().toString(),
							sourceFormat.isBigEndian()?"big endian":"little endian") 
						);
	    }
	    return sourceFormat;
	}
	
	public static AudioFormat getAudioFormat(AudioFormat.Encoding encoding, float sampleRate, 
			int sampleSizeInBits, int channels, boolean bigEndian,
			int frameSizeInBytes)  throws AudioUtil.Exception {

		AudioFormat desiredFormat = new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSizeInBytes, sampleRate, !bigEndian);
		AudioFormat[] formats = AudioSystem.getTargetFormats(encoding,desiredFormat);
		AudioFormat sourceFormat = null;
		for(AudioFormat format : formats) {
			if( format.isBigEndian() == bigEndian
					&& format.getEncoding() == encoding
					&& format.getSampleSizeInBits() == sampleSizeInBits
					&& format.getChannels() == channels
					&& format.getSampleRate() == sampleRate ) {
					sourceFormat = format;
					break;
			}
		}
		if(sourceFormat==null) {
			throw new AudioUtil.Exception(new UnsupportedAudioFormatException(String.format("The format \"%s\" is unsupported",desiredFormat)));
		}
		return sourceFormat;
	}
	
	public static void loop(AudioFormat sourceFormat, ValueAccessor<Boolean> stopper, EventListener listener, byte[] bytes) throws AudioUtil.Exception  {
		_validateAudioFormat(sourceFormat);
		try {
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, sourceFormat);
			SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(info);
			sourceLine.open(sourceFormat);
			sourceLine.start();
			int read;
			int totalRead=0;
			int bufferSize = 256;
			byte buffer[] = new byte[bufferSize];
			outerLoop:
			do {
				ByteArrayInputStream baos = new ByteArrayInputStream(bytes);
				totalRead = 0;
				while ( (read=baos.read(buffer,0,buffer.length))!=(-1) ) {
					totalRead+=read;
					sourceLine.write(buffer, 0, read);
					if(listener!=null) {
						listener.updatePosition(totalRead/sourceFormat.getFrameSize());
					}
					if(stopper!=null && stopper.value()) {
						if(listener!=null) listener.stopped();
						break outerLoop;
					}
				}
			} while(true);
			sourceLine.drain();
			sourceLine.close();
		} catch(java.lang.Exception e){
			throw new AudioUtil.Exception(e);
		}
	}
	
	public static void play(AudioFormat sourceFormat, ValueAccessor<Boolean> stopper, EventListener listener, InputStream sourceStream, OutputStream outStream) throws AudioUtil.Exception  {
		_validateAudioFormat(sourceFormat);
		try {
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, sourceFormat);
			SourceDataLine sourceLine = (SourceDataLine) AudioSystem.getLine(info);
			sourceLine.open(sourceFormat);
			sourceLine.start();
			int read;
			int totalRead=0;
			int bufferSize = 256;
			byte buffer[] = new byte[bufferSize];
			while ( (read=sourceStream.read(buffer,0,buffer.length))!=(-1) ) {
				totalRead+=read;
				sourceLine.write(buffer, 0, read);
				if(outStream!=null) {
					outStream.write(buffer, 0, read);
				}
				if(listener!=null) {
					listener.updatePosition(totalRead/sourceFormat.getFrameSize());
				}
				if(stopper!=null && stopper.value()) {
					if(listener!=null) listener.stopped();
					break;
				}
			}
			sourceLine.drain();
			sourceLine.close();
		} catch(java.lang.Exception e){
			throw new AudioUtil.Exception(e);
		}
	}
	
	public static void read(AudioFormat sourceFormat, InputStream sourceStream, OutputStream outStream) throws AudioUtil.Exception {
		_validateAudioFormat(sourceFormat);
		try {
			int read;
			int bufferSize = (int)sourceFormat.getSampleRate() * sourceFormat.getFrameSize();
			byte buffer[] = new byte[bufferSize];
			while ( (read=sourceStream.read(buffer,0,buffer.length))!=(-1) ) {
				outStream.write(buffer, 0, read);
			}
		} catch(java.lang.Exception e){
			throw new AudioUtil.Exception(e);
		}
	}
	
	public static void record(AudioFormat sourceFormat, int seconds, OutputStream out) throws AudioUtil.Exception  {
		
		_validateAudioFormat(sourceFormat);
		try {
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, sourceFormat);
			TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(sourceFormat);
			line.start();
			int bufferSize = (int)sourceFormat.getSampleRate() * sourceFormat.getFrameSize();
			byte buffer[] = new byte[bufferSize];
			int i = 0;
			while (i<seconds) {
				int count = line.read(buffer, 0, buffer.length);
				if (count > 0) {
					out.write(buffer,0,count);
				}
				i++;
			}
			line.close();
		} catch(java.lang.Exception e){
			throw new AudioUtil.Exception(e);
		}
	}
	
	public static void record(AudioFormat sourceFormat, ValueAccessor<Boolean> stopper, OutputStream out) throws AudioUtil.Exception  {
		
		_validateAudioFormat(sourceFormat);
		try {
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, sourceFormat);
			TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(sourceFormat);
			line.start();
			int bufferSize = 256;
			byte buffer[] = new byte[bufferSize];
			while (!stopper.value()) {
				int count = line.read(buffer, 0, buffer.length);
				if (count > 0) {
					out.write(buffer,0,count);
				}
			}
			line.close();
		} catch(java.lang.Exception e){
			throw new AudioUtil.Exception(e);
		}
	}
	
	public static int frames(AudioFormat format, int dataLength) {
		int frameSize = format.getFrameSize();
		return dataLength / frameSize;
	}
	
	public static float seconds(AudioFormat format, int dataLength) {
		int frames = frames(format,dataLength);
		float frameRate = format.getFrameRate();
		return frames / frameRate;
	}
	
	public static int[] intsFromBytes(AudioFormat format, byte[] data) 
			throws AudioUtil.Exception {
		
		_validateAudioFormat(format);
		ByteBuffer byteBuf = ByteBuffer.wrap(data);
		ShortBuffer shortBuf = byteBuf
				.order(format.isBigEndian()?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN)
				.asShortBuffer();
		int limit = shortBuf.limit();
		int[] ret = new int[limit];
		for(int i = 0; i<limit; i++) {
			ret[i] = (int)shortBuf.get(i);
		}
		return ret;
	}
	
	public static byte[] bytesFromInts(AudioFormat format, int[] data) 
			throws AudioUtil.Exception {
		
		_validateAudioFormat(format);
		short[] shorts = new short[data.length];
		for(int i=0, ie=data.length; i<ie; i++) {
			shorts[i] = (short)data[i];
		}
		int size = data.length * (Short.SIZE/8);
		ByteBuffer byteBuf = ByteBuffer.allocate(size);
		byteBuf.order(format.isBigEndian()?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN)
			.asShortBuffer().put(shorts);
		byte[] ret = new byte[size];
		byteBuf.get(ret);
		return ret;
	}
	
	public static int[] normalize(int[] data) {
		int[] tmp = new int[data.length];
		double max=0;
		for(int i=0; i<data.length; i++) {
			int t = Math.abs(data[i]);
			if(t>max) {
				max = t;
			}
		} 
		double scale = 32677.0 / max;
		for(int i=0; i<data.length; i++) {
			tmp[i] = (int)(((double)data[i]) * scale);
		}
		return tmp;
	}
	
	public static int[] smoothWithEMA(int[] data, int period) {
		
		// EMA = Price(t) * k + EMA(y) * (1 - k)
		// t = today, y = yesterday, N = number of days in EMA, k = 2/(N+1)
		
		double k = 2.0 / ((double)period + 1.0);
		double[] tmp = new double[data.length];
		tmp[0] = (double)data[0];
		for(int i=1; i<data.length; i++) {
			double in = (double)data[i];
			double last = tmp[i-1];
			tmp[i] = in * k + last * (1.0-k);
		}
		int[] ret = new int[data.length];
		for(int i=0; i<data.length; i++) {
			ret[i] = (int)tmp[i];
		}
		return ret;
	}
	
	private static void _validateAudioFormat(AudioFormat format) throws AudioUtil.Exception {
		if(format.getSampleSizeInBits()!=16
			|| format.getChannels()!=1
			|| format.getEncoding()!=AudioFormat.Encoding.PCM_SIGNED) {
			throw new AudioUtil.Exception(new UnsupportedAudioFormatException());
		}
	}
	
	public static class Exception extends java.lang.Exception {

		private static final long serialVersionUID = -4584859239336683704L;
		public Exception() {
			super();
		}
		public Exception(String message, Throwable cause,
				boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}
		public Exception(String message, Throwable cause) {
			super(message, cause);
		}

		public Exception(String message) {
			super(message);
		}

		public Exception(Throwable cause) {
			super(cause);
		}
		
	}
}
