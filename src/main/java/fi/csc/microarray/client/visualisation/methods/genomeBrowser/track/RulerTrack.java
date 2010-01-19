package fi.csc.microarray.client.visualisation.methods.genomeBrowser.track;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import fi.csc.microarray.client.visualisation.methods.genomeBrowser.View;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.drawable.Drawable;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.drawable.LineDrawable;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.drawable.RectDrawable;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.drawable.TextDrawable;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat.ColumnType;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.AreaResult;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.BpCoord;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.BpCoordDouble;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.BpCoordRegion;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.RegionContent;

public class RulerTrack extends Track{

	private static final int textY = 10;
	private final static int MINOR_STEPS = 5;
	
	List<Long> info = new ArrayList<Long>();
	
	public RulerTrack(View view) {
		super(view, null);
	}

	@Override
	public Collection<Drawable> getDrawables() {

		Collection<Drawable> drawables = getEmptyDrawCollection();
		BpCoordRegion region = getView().getBpRegion();
		
		long magnitude = (long)Math.pow(10, (int)Math.log10(region.getLength()));

		final long start = region.start.bp - region.start.bp % magnitude;
		final int steps = (int)Math.ceil(region.getLength() / magnitude) + 1;
		final long end = start + steps * magnitude;
		
		//System.out.println("Start: " + start + " , steps " + steps + ", end " + end  + ", magnitude " + magnitude);
		
		for (long bp = start; bp <= end; bp += magnitude){

			int x = getView().bpToTrack(new BpCoord(bp, region.start.chr));
			String text = Utils.toHumanReadable(bp);
			
			drawables.add(new TextDrawable( x, textY, text, Color.black));
		}
			
		drawables.addAll(getRuler(new BpCoordRegion(start, end, region.start.chr), 
				steps * MINOR_STEPS , start / magnitude % 2 == 1));		

		return drawables;
	}
	
	private Collection<Drawable> getRuler(BpCoordRegion bpRegion, int steps, boolean whiteStart){

		Collection<Drawable> drawables = getEmptyDrawCollection();

		boolean isWhite = whiteStart;	
		final int boxHeight = 5;
		
		double increment = bpRegion.getLength() / (double)steps;
		BpCoordDouble boxBp = new BpCoordDouble(bpRegion.start);
		int boxX;
		int lastBoxX = getView().bpToTrack(bpRegion.start);
		
		info.clear();

		for (int i = 0; i < steps ; i++){			
									
			Color c = (isWhite = !isWhite) ? Color.white : Color.black;				
			boxBp = boxBp.move(increment);
			
			info.add((long)(double)boxBp.bp);
			
			boxX = getView().bpToTrack(boxBp.asBpCoord());
			
			drawables.add(new RectDrawable(lastBoxX, textY, boxX - lastBoxX, boxHeight, c, Color.black));
					
			Color lineColor =  (i % MINOR_STEPS == MINOR_STEPS - 1) ? 
					new Color(0,0,0,64) : new Color(0,0,0,32);
					
			drawables.add(new LineDrawable(
					boxX, -getView().getHeight() + getMaxHeight(), 
					boxX, getMaxHeight(), lineColor));
			
			lastBoxX = boxX;
		}
		return drawables;

	}

	public void processAreaResult(AreaResult<RegionContent> areaResult) {
		// No data
	}

	public List<Long> getRulerInfo() {
		return info;		
	}
	
	@Override
	public int getMaxHeight(){
		return textY * 2;
	}
	
	@Override
	public Collection<ColumnType> getDefaultContents() {
		return Arrays.asList(new ColumnType[] {}); 
	}
	
	@Override
	public boolean isConcised() {
		return false;
	}
}
