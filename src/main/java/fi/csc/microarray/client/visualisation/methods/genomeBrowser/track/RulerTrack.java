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
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat.Content;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.AreaResult;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.Region;

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
		Region region = getView().getBpRegion();
		
		long magnitude = (long)Math.pow(10, (int)Math.log10(region.getLength()));

		final long start = region.start - region.start % magnitude;
		final int steps = (int)Math.ceil(region.getLength() / magnitude) + 1;
		final long end = start + steps * magnitude;
		
		//System.out.println("Start: " + start + " , steps " + steps + ", end " + end  + ", magnitude " + magnitude);
		
		for (long bp = start; bp <= end; bp += magnitude){

			int x = getView().bpToTrack(bp);
			String text = Utils.toHumanReadable(bp);
			
			drawables.add(new TextDrawable( x, textY, text, Color.black));
		}
			
		drawables.addAll(getRuler(start, end, 
				steps * MINOR_STEPS , start / magnitude % 2 == 1));		

		return drawables;
	}
	
	private Collection<Drawable> getRuler(long startBp, long endBp, int steps, boolean whiteStart){

		Collection<Drawable> drawables = getEmptyDrawCollection();

		boolean isWhite = whiteStart;	
		final int boxHeight = 5;
		
		double increment = (endBp - startBp) / (double)steps;
		double boxBp = startBp;
		int boxX;
		int lastBoxX = getView().bpToTrack(startBp);
		
		info.clear();

		for (int i = 0; i < steps ; i++){			
									
			Color c = (isWhite = !isWhite) ? Color.white : Color.black;				
			boxBp += increment;
			
			info.add((long)boxBp);
			
			boxX = getView().bpToTrack((long)boxBp);
			
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

	public void processAreaResult(AreaResult areaResult) {
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
	public Collection<Content> getDefaultContents() {
		return Arrays.asList(new Content[] {}); 
	}
	
	@Override
	public boolean isConcised() {
		return false;
	}
}