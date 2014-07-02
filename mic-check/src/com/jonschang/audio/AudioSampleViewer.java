package com.jonschang.audio;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.sound.sampled.AudioFormat;

import com.jonschang.audio.freq.OrganOfCortiCanvas;
import com.jonschang.audio.freq.OrganOfCortiImpl;

/* TODO: New visualizer that draws rectangular regions representing the change
 * from local max to local min over a duration of time.  The sequence of data would
 * be the change in amplitude to the next local max/min over duration of amplitude shift.
 * This is rather than having amplitude samples.  Also whether it's concave, convex, or direct.
 */

public class AudioSampleViewer extends Frame {

	private static int SCOPE_ALL=1;
	private static int SCOPE_WINDOW=2;
	private static boolean loop = false;
	
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
	
	private Panel left;
	private OrganOfCortiCanvas organOfCortiCanvas;
	private AudioSampleCanvas audioSampleCanvas;
	private GridBagLayout leftGB;
	private TextField viewPosField;
	private TextField viewLenField;
	private TextField fileField;
	private Choice bookmarkChoice;
	private AudioFormat format;
	
	private LocalMinMaxAnnotator minMaxAnn = new LocalMinMaxAnnotator();
	
	private String lastLoadedFile;
	private int[] playInts;
	private int viewLength = -1;
	private int viewPosition = 0;
	private int playScope = SCOPE_ALL;
	
	public LinkedList<Point> positionHistory = new LinkedList<Point>();
	public ArrayList<Point> bookmarks = new ArrayList<Point>();
	
	public void load() {
		try {
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			InputStream inputStream = AudioUtil.getFileInputStream(fileField.getText());
			if(!fileField.getText().equals(lastLoadedFile)) {
				bookmarks.clear();
				bookmarkChoice.removeAll();
			}
			lastLoadedFile = fileField.getText();
			AudioUtil.read(format, inputStream, outputStream);
			
			playInts = AudioUtil.intsFromBytes(format,outputStream.toByteArray());
			playInts = AudioUtil.normalize(playInts);
			viewPosition = 0;
			viewLength = playInts.length;
			positionHistory.clear();
			
			updateCanvas();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private ValueAccessor<Boolean> stopper = new ValueAccessor<Boolean>() {
		private Boolean stopFlag = false;
		public Boolean value() { return stopFlag; }
		public void value(Boolean value) { stopFlag = value; }
	};
	private ValueAccessor<Boolean> recordStopper = new ValueAccessor<Boolean>() {
		private Boolean stopFlag = false;
		public Boolean value() { return stopFlag; }
		public void value(Boolean value) { stopFlag = value; }
	};
	private AudioUtil.EventListener playheadListener = new AudioUtil.EventListener() {
		@Override
		public void updatePosition(int x) {
			audioSampleCanvas.setPlayhead(x);
		}
		@Override
		public void stopped() {
		}
	};
	
	public AudioSampleViewer() throws UnsupportedAudioFormatException, IOException, AudioUtil.Exception {
		
		AudioFormat audioFormat = AudioUtil.getCurrentPreferredAudioFormat();
		format = audioFormat;
		
		setSize(1024,1024);
		setResizable(false);
		setLayout(new GridLayout(1,1));
		
		//////////////
		left = new Panel();
		left.setSize(1024,1024);
		left.setBackground(Color.WHITE);
		left.setLayout(leftGB = new GridBagLayout());
		add(left);
		
		GridBagConstraints gbc;
		
		int y = 2;
		
		Label lbl = new Label("File:");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		leftGB.setConstraints(lbl,gbc);
		left.add(lbl);
		TextField fld = fileField = new TextField("last_out.raw");
		fld.setColumns(10);
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		leftGB.setConstraints(fld,gbc);
		left.add(fld);
		y++;
		
		Button btn = new Button("Load");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = y++;
		gbc.gridwidth = 2;
		leftGB.setConstraints(btn,gbc);
		left.add(btn);
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				load();
				invalidate();
			}
		});
		
		btn = new Button("Record");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = y++;
		gbc.gridwidth = 2;
		leftGB.setConstraints(btn,gbc);
		left.add(btn);
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final Button btn = (Button)e.getSource();
				if(btn.getLabel().equals("Stop")) {
					recordStopper.value(true);
					return;
				}
				bookmarks.clear();
				bookmarkChoice.removeAll();
				recordStopper.value(false);
				btn.setLabel("Stop");
				executor.execute(new Runnable() {
					public void run() {
						try {
							File file = new File(fileField.getText());
							OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file));
							AudioUtil.record(format, recordStopper, fileOut);
							fileOut.flush();
							fileOut.close();
							load();
							invalidate();
						} catch(Exception e) {
							e.printStackTrace();
						} finally {
							btn.setLabel("Record");
						}
					}
				});
			}
		});
		
		Choice playChoice = new Choice();
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = y++;
		gbc.gridwidth = 2;
		leftGB.setConstraints(playChoice,gbc);
		left.add(playChoice);
		playChoice.add("Loop Window");
		playChoice.add("Play All");
		playChoice.add("Play Window");
		loop = true;
		playScope = SCOPE_WINDOW;
		playChoice.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.SELECTED) {
					playScope = e.getItem().equals("Play All")
						? SCOPE_ALL
						: SCOPE_WINDOW;
				}
				if(e.getStateChange()==ItemEvent.SELECTED) {
					if(((String)e.getItem()).contains("Loop")) {
						loop = true;
					} else {
						loop = false;
					}
				}
			}
		});
		
		btn = new Button("Play");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = y++;
		gbc.gridwidth = 2;
		leftGB.setConstraints(btn,gbc);
		left.add(btn);
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final Button btn = (Button)e.getSource();
				if(playInts==null || playInts.length==0) {
					return;
				}
				if(btn.getLabel().equals("Stop")) {
					stopper.value(true);
					return;
				}
				stopper.value(false);
				btn.setLabel("Stop");
				executor.execute(new Runnable() {
					public void run() {
						try {
							if(playScope==SCOPE_ALL) {
								AudioUtil.play(format, stopper, playheadListener, new ByteArrayInputStream(
									AudioUtil.bytesFromInts(format,playInts)), null);
							} else {
								if(loop) {
									AudioUtil.loop(format, stopper, playheadListener,AudioUtil.bytesFromInts(format,audioSampleCanvas.getData()));
								} else {
									AudioUtil.play(format, stopper, playheadListener, new ByteArrayInputStream(
											AudioUtil.bytesFromInts(format,audioSampleCanvas.getData())), null);
								}
							}
						} catch(Exception e) {
							e.printStackTrace();
						} finally {
							btn.setLabel("Play");
						}
					}
				});
			}
		});
		
		lbl = new Label("Pos:");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		leftGB.setConstraints(lbl,gbc);
		left.add(lbl);
		fld = viewPosField = new TextField("0");
		fld.setColumns(10);
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		leftGB.setConstraints(fld,gbc);
		left.add(fld);
		y++;
		MyTextFieldListener listener = new MyTextFieldListener(viewPosField,new ValueAccessor<Integer>(){
			@Override
			public Integer value() {
				return viewPosition;
			}
			@Override
			public void value(Integer value) {
				viewPosition = value;
				updateCanvas();
			}
		});
		viewPosField.addKeyListener(listener);
		viewPosField.addTextListener(listener);
		
		lbl = new Label("Len:");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		leftGB.setConstraints(lbl,gbc);
		left.add(lbl);
		fld = viewLenField = new TextField();
		fld.setColumns(10);
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = y;
		gbc.gridwidth = 1;
		leftGB.setConstraints(fld,gbc);
		left.add(fld);
		y++;
		listener = new MyTextFieldListener(viewLenField,new ValueAccessor<Integer>(){
			@Override
			public Integer value() {
				return viewLength;
			}
			@Override
			public void value(Integer value) {
				viewLength = value;
				updateCanvas();
			}
		});
		viewLenField.addKeyListener(listener);
		viewLenField.addTextListener(listener);
		
		btn = new Button("Bookmark");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = y++;
		gbc.gridwidth = 2;
		leftGB.setConstraints(btn,gbc);
		left.add(btn);
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Point mark = new Point(viewPosition, viewLength);
				bookmarks.add(mark);
				bookmarkChoice.add(String.format("%d %d", mark.x, mark.y));
			}
		});
		
		bookmarkChoice = new Choice();
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = y++;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		leftGB.setConstraints(bookmarkChoice,gbc);
		left.add(bookmarkChoice);
		bookmarkChoice.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.SELECTED) {
					Choice choicer = (Choice)e.getSource();
					int idx = choicer.getSelectedIndex();
					Point newPos = bookmarks.get(idx);
					viewPosition = newPos.x;
					viewLength = newPos.y;
					updateCanvas();
				}
			}
		});
		bookmarkChoice.addKeyListener(new KeyAdapter(){
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyReleased(e);
				switch(e.getKeyCode()) {
				case 8: // DELETE
					Choice choicer = (Choice)e.getSource();
					int idx = choicer.getSelectedIndex();
					if(idx!=-1) {
						bookmarks.remove(idx);
						bookmarkChoice.remove(idx);
					}
					break;
				}
			}
		});
		
		audioSampleCanvas = new AudioSampleCanvas(playInts);
		audioSampleCanvas.annotators.add(minMaxAnn);
		audioSampleCanvas.setSize(800,100);
		gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.gridy = 2;
		gbc.gridheight = 1;
		leftGB.setConstraints(audioSampleCanvas, gbc);
		left.add(audioSampleCanvas);
		final Point canvasSelectInfo = new Point();
		canvasSelectInfo.setLocation(0, 0);
		audioSampleCanvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyReleased(e);
				switch(e.getKeyCode()) {
				case 27: // ESC
					if(canvasSelectInfo.y==1) {
						canvasSelectInfo.y=0;
					} else if(positionHistory.size()>0) {
						Point point = positionHistory.pollFirst();
						viewPosition = point.x;
						viewLength = point.y;
						updateCanvas(true);
					}
					break;
				}
			}
			@Override
			public void keyPressed(KeyEvent e) {
				super.keyPressed(e);
				int motion = (int) (viewLength*.10);
				switch(e.getKeyCode()) {
				case 37: // LEFT
					viewPosition -= motion;
					break;
				case 39: // RIGHT
					viewPosition += motion;
					break;
				case 38: // UP
					zoomIn();
					break;
				case 40: // DOWN
					zoomOut();
					break;
				default:
					System.out.printf("key=%d",e.getKeyCode());
					return;
				}
				if(viewPosition < 0) {
					viewPosition = 0;
				}
				if(viewPosition+viewLength > playInts.length-1) {
					viewPosition = playInts.length - viewLength;
				}
				updateCanvas();
			}
		});
		audioSampleCanvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				canvasSelectInfo.y = 1;
				canvasSelectInfo.x = e.getPoint().x;
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				int down = canvasSelectInfo.y;
				canvasSelectInfo.y = 0;
				if(down == 1 && e.getPoint().x!=canvasSelectInfo.x) {
					int[] sel = audioSampleCanvas.getSelectedRegion();
					viewLength = sel[1] - sel[0];
					viewPosition = viewPosition + sel[0];
					updateCanvas();
				}
			}
		});
		audioSampleCanvas.addMouseMotionListener(new MouseMotionAdapter(){
			@Override
			public void mouseDragged(MouseEvent e) {
				super.mouseDragged(e);
				if(canvasSelectInfo.y == 1) {
					int mouseCurrentX = e.getPoint().x;
					if(canvasSelectInfo.x < mouseCurrentX) {
						audioSampleCanvas.setSelectedRegion(canvasSelectInfo.x, mouseCurrentX);
					} else {
						audioSampleCanvas.setSelectedRegion(mouseCurrentX, canvasSelectInfo.x);
					}
				}
			}
		});
		
		organOfCortiCanvas = new OrganOfCortiCanvas(
				OrganOfCortiImpl.buildPianoScaleOrganOfCorti(audioFormat.getSampleRate()),
				playInts);
		organOfCortiCanvas.setSize(800,300);
		//organOfCortiCanvas.setBackground(Color.blue);
		gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.gridy = 3;
		gbc.gridheight = y;
		leftGB.setConstraints(organOfCortiCanvas, gbc);
		left.add(organOfCortiCanvas);
		
		pack();
		setVisible(true);
	}
	
	private void zoomIn() {
		viewLength = viewLength > 0 
				? (int)(1.0*viewLength - viewLength*.1)
				: playInts.length;
		updateCanvas();
	}
	
	private void zoomOut() {
		viewLength = viewLength < playInts.length
				? (int)(1.0*viewLength + viewLength*.1)
				: playInts.length;
		updateCanvas();
	}
	
	public void updateCanvas() {
		updateCanvas(false);
	}
	public void updateCanvas(boolean isPop) {
		
		if(playInts==null) {
			return;
		}
		int len = viewPosition + viewLength < playInts.length  
				? viewLength
				: playInts.length - viewPosition;
		int[] newViewSegment;
		try {
			newViewSegment = Arrays.copyOfRange(playInts, viewPosition, viewPosition + len);
		} catch (Exception e) {
			return;
		}
		audioSampleCanvas.updateWith(newViewSegment);
		organOfCortiCanvas.updateWith(newViewSegment);
		viewLenField.setText(String.valueOf(viewLength));
		viewPosField.setText(String.valueOf(viewPosition));
		
		if(isPop) {
			return;
		}
		Point point = new Point(viewPosition,viewLength);
		positionHistory.addFirst(point);
		while(positionHistory.size()>25) {
			positionHistory.pollLast();
		}
	}
	
	private static class MyTextFieldListener extends KeyAdapter implements TextListener {

		private TextField managedField;
		private String previous;
		private ValueAccessor<Integer> managedValue;
		
		public MyTextFieldListener(TextField field, ValueAccessor<Integer> valueSource) {
			managedField = field;
			previous = managedField.getText();
			managedValue = valueSource;
		}
		
		@Override
		public void keyReleased(KeyEvent e) {
			super.keyReleased(e);
			switch(e.getKeyCode()) {
			case 10:
				managedValue.value(Integer.valueOf(managedField.getText()));
				break;
			}
		}
		@Override
		public void keyPressed(KeyEvent e) {
			super.keyPressed(e);
			int delta = 0;
			switch(e.getKeyCode()) {
			case 38: // UP
				delta = 1;
				break;
			case 40: // DOWN
				delta = -1;
				break;
			}
			if(delta!=0) {
				Integer val = Integer.valueOf(managedField.getText())+delta;
				managedField.setText(String.valueOf(val));
				managedValue.value(val);
			}
		}
		@Override
		public void textValueChanged(TextEvent e) {
			if(!managedField.getText().matches("[0-9]+")) {
				managedField.setText(previous);
			}
		}
	}
	
	public static void main(String[] argv) throws Exception {
		AudioSampleViewer frame = new AudioSampleViewer();
	}
}
