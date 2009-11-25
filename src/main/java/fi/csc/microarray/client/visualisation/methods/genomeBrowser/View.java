package fi.csc.microarray.client.visualisation.methods.genomeBrowser;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Timer;

import fi.csc.microarray.client.visualisation.methods.genomeBrowser.dataFetcher.QueueManager;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.drawable.Drawable;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.Region;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.track.RulerTrack;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.track.Track;

public abstract  class View implements MouseListener, MouseMotionListener, MouseWheelListener{
	
	protected Region bpRegion;
	public Region highlight;
	
	public Collection<Track> tracks = new LinkedList<Track>();
	protected Rectangle viewArea = new Rectangle(0, 0, 500, 500);
	private QueueManager queueManager = new QueueManager();
	private Point2D dragStartPoint;
	private boolean dragStarted;
	
	GenomeBrowser parentPlot;
	
	private static final int FPS = 30;
    
    private boolean movable;
    protected boolean zoomable;
    private boolean selectable;
    
    protected final float ZOOM_FACTOR = 1.03f;
    
    private List<RegionListener> listeners = new LinkedList<RegionListener>();
	public int margin = 0;
	protected Float trackHeight;
	private Point2D dragEndPoint;
	private Point2D dragLastStartPoint;
	
	public View(GenomeBrowser parent, boolean movable, boolean zoomable, boolean selectable) {
		parentPlot = parent;
		this.movable = movable;
		this.zoomable = zoomable;
		this.selectable = selectable;
	}

	public void addTrack(Track t){
		tracks.add(t);
	}
	
	protected void drawView(Graphics2D g, boolean isAnimation){

		if(bpRegion == null){
			setBpRegion(new Region(0, 1024*1024*250), false);
		}				

		Rectangle viewClip = g.getClipBounds();
		viewArea = viewClip;

		float y = viewClip.y;	
		int x = viewClip.x;

		for (Track t : tracks){

			if(t.getMaxHeight() > 0){
				//long start = System.currentTimeMillis();
				Collection<Drawable> drawables = t.getDrawables();
				//			//System.out.println("Get  drawables, view: " + this + ", track: " + t + ", " + (System.currentTimeMillis() - start) + " ms ");

				//				g.setPaint(Color.green);
				//				g.drawLine(0, (int)y, 800, (int)y);

				//start = System.currentTimeMillis();
				
				for ( Drawable drawable : drawables){
					
					int maybeReversedY = (int)y;
					
					if(t.isReversed()){
						drawable.upsideDown();
						maybeReversedY += Math.min(getTrackHeight(), t.getMaxHeight());
					}
					
					drawDrawable(g, x, maybeReversedY, drawable);
				}			
				//System.out.println("Draw drawables, view: " + this + ", track: " + t + ", " + (System.currentTimeMillis() - start) + " ms ");
			}
			
			if(t.getMaxHeight() == Integer.MAX_VALUE){
				y += getTrackHeight();
			} else {
				y += t.getMaxHeight();
			}
		}
	}

	protected abstract void drawDrawable(Graphics2D g, int x, int y, Drawable drawable);

	public float getTrackHeight() {

		if(trackHeight == null){
			trackHeight = (getHeight() - getMaxTrackHeightTotal()) / (float)getStretchableTrackCount();
		}
		return trackHeight;
	}
	
	protected int getMaxTrackHeightTotal(){
		int maxHeightTotal = 0;

		for(Track track : tracks){
			if(track.getMaxHeight() != Integer.MAX_VALUE){
				maxHeightTotal += track.getMaxHeight();
			}
		}		
		return maxHeightTotal;
	}
	
	protected int getStretchableTrackCount(){
		int stretchableCount = 0;

		for(Track track : tracks){
			if(track.getMaxHeight() == Integer.MAX_VALUE){					
				stretchableCount++;
			}
		}
		return stretchableCount;
	}


	public int getWidth() {
		return this.viewArea.width;
	}

	public int getHeight() {
		return this.viewArea.height ;
	}

	public QueueManager getQueueManager() {
		return queueManager ;
	}

	
	public void setBpRegion(Region region, boolean disableDrawing) {
		bpRegion = region;
		
		//Bp-region change may change visibility of tracks, calculate sizes again
		trackHeight = null;
		
		if(!disableDrawing){
			for (Track t : tracks){

				t.updateData();
			}
			dispatchRegionChange();
		}
	}
	
	public Region getBpRegion(){
		return bpRegion;
	}
	
	public void mouseClicked(MouseEvent e) {
	}


	public void mouseEntered(MouseEvent e) {
	}


	public void mouseExited(MouseEvent e) {
	}


	public void mousePressed(MouseEvent e) {
				
		stopAnimation();
		dragStartPoint = scale(e.getPoint());
		dragStarted = false;
	}

	public void mouseReleased(MouseEvent e) {
		
		//System.out.println("End: " + dragEndPoint + ", Start: " + dragLastStartPoint);
		
		if(dragStarted && dragEndPoint != null && dragLastStartPoint != null && 
				Math.abs(dragEndPoint.getX() - dragLastStartPoint.getX()) > 10){
			
			
			stopAnimation();
			
			timer = new Timer(1000 / FPS, new ActionListener(){
				
				private int i = 0;
				private int ANIMATION_FRAMES = 30 ;
				private long startTime = System.currentTimeMillis();
				
				public void actionPerformed(ActionEvent arg0) {
					
					double endX = dragEndPoint.getX();
					double startX = dragLastStartPoint.getX();
					
					double newX = endX  - (endX - startX) / (ANIMATION_FRAMES - i) ;
					
					dragEndPoint = new Point2D.Double(newX, dragEndPoint.getY());
					
					boolean skipFrame = (i < (ANIMATION_FRAMES - 1)) 
					&& System.currentTimeMillis() > startTime + (1000 / FPS) * i;    

					if( i < ANIMATION_FRAMES){
						handleDrag(dragLastStartPoint, dragEndPoint, skipFrame);
						i++;
					} else {
						stopAnimation();
					}
				}			
			});
			timer.setRepeats(true);
			timer.start();
			}
	}

	public void mouseDragged(MouseEvent e) {
		if(movable){
			
			dragStarted = true;
			dragEndPoint = scale(e.getPoint());
			
			handleDrag(dragStartPoint, dragEndPoint, false);
						
		}
		dragLastStartPoint = dragStartPoint;
		dragStartPoint = scale(e.getPoint());
	}
	public void mouseMoved(MouseEvent e) {
	}
	
	protected abstract void handleDrag(Point2D start, Point2D end, boolean disableDrawing);
	
	private Timer timer;
	private int timerCounter;

	public void mouseWheelMoved(final MouseWheelEvent e) {

		stopAnimation();
		
		
		timer = new Timer(1000 / FPS, new ActionListener(){
			
			private int i = 0;
			
			//100 ms to give time for slower machines to view couple animation frames also
			private long startTime = System.currentTimeMillis() + 100;
			private int ANIMATION_FRAMES = 15;
			
			public void actionPerformed(ActionEvent arg0) {
				
				boolean skipFrame = (i < (ANIMATION_FRAMES - 1)) 
					&& System.currentTimeMillis() > startTime + (1000 / FPS) * i;
					
				//System.out.println("StartTime: "+  startTime + ", Current: " + System.currentTimeMillis() + ", skip: " + skipFrame);

				if( i < ANIMATION_FRAMES){
					zoom((int)scale(e.getPoint()).getX(), e.getWheelRotation(), skipFrame);
					i++;
				} else {
					stopAnimation();
				}
			}			
		});
		timer.setRepeats(true);
		timer.setCoalesce(false);
		timer.start();
	}
	
	private void stopAnimation(){
		if(timer != null){
			timer.stop();
			timer = null;
		}
	}

	private void zoom(int lockedX, int wheelRotation, boolean disableDrawing){
		if(zoomable){
			if(wheelRotation > 0){
				lockedX = (int)getWidth() - lockedX + getX() * 2 ;
			}

			double pointerBp = trackToBp(lockedX);
			double pointerRelative = trackToRelative(lockedX);

			long startBp = getBpRegion().start;
			long endBp = getBpRegion().end;

			double width = endBp - startBp;
			width *= Math.pow(ZOOM_FACTOR, wheelRotation);			


			startBp = (long)(pointerBp - width * pointerRelative);
			endBp = (long)(pointerBp + width * (1 - pointerRelative));

			if(startBp < 0){
				endBp += -startBp;
				startBp = 0;
			}

			//System.out.println(startBp + ", " + endBp);

			setBpRegion(new Region(startBp, endBp), disableDrawing);
		}		
	}
	
	public int bpToTrack(long bp){
		
    	return (int)(((double)bp - getBpRegion().start) / (getBpRegion().end - getBpRegion().start) * getWidth()) + getX();
    }
	
	public long trackToBp(long d){
		return (long)(trackToRelative(d) * (getBpRegion().end - getBpRegion().start) + getBpRegion().start);
	}
	
	public double trackToRelative(long track){
		return (double)(track - getX()) / getWidth();
	}

	public int getX() {
		return viewArea.x;
	}

	public int getY() {
		return viewArea.y;
	}
	
	public void redraw() {
		parentPlot.redraw();
	}

	public List<Long> getRulerInfo() {
		for (Track t : tracks){
			if (t instanceof RulerTrack) {
				RulerTrack ruler = (RulerTrack) t;
				return ruler.getRulerInfo();
			}
		}
		return null;
	}
	
	public void addRegionListener(RegionListener listener){
		listeners.add(listener);
	}
	
	public void dispatchRegionChange(){
		for (RegionListener listener: listeners){
			listener.RegionChanged(bpRegion);
		}
	}
	
	private Point2D scale(Point2D p){
		return new Point(
				(int)(p.getX() / parentPlot.chartPanel.getScaleX()),
				(int)(p.getY() / parentPlot.chartPanel.getScaleY()));
	}
}