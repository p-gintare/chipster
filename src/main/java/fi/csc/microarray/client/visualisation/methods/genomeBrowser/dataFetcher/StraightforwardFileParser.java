package fi.csc.microarray.client.visualisation.methods.genomeBrowser.dataFetcher;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat.ChunkParser;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat.Content;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat.ReadInstructions;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat.TsvChunkParser;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.AreaRequest;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.AreaResult;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.Region;
import fi.csc.microarray.client.visualisation.methods.genomeBrowser.message.RegionContent;


public class StraightforwardFileParser<T> extends AreaRequestHandler{

	private ReadInstructions<?> instructions;

	public StraightforwardFileParser(
			File file, 
			Queue<AreaRequest> areaRequestQueue,
			AreaResultListener areaResultListener, 
			ReadInstructions<?> instructions) {
		
		super(areaRequestQueue, areaResultListener);
		this.instructions = instructions;
		this.file = file;
	}

	private File file;
	
	private ByteChunk chunk;

	private void readFile() {

		chunk = new ByteChunk(instructions.chunker.getChunkByteLength());

		FileInputStream stream;
		try {
			stream = new FileInputStream(file);			
			chunk.content = new byte[(int)file.length()];
			stream.read(chunk.content);
						
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void processAreaRequest(AreaRequest areaRequest) {
		if(chunk == null){
			readFile();
		}  
		
		//List<List<Object>> hits = new LinkedList<List<Object>>();
		ChunkParser parser = instructions.getParser();
		parser.setChunk(chunk);

		while (((TsvChunkParser)parser).readNextLine()){
			
			List<Object> read = parser.getRead(TsvChunkParser.NEXT);
			
			Region readReg = new Region(
					(Long)read.get(instructions.fileDef.indexOf(Content.BP_START)),
					(Long)read.get(instructions.fileDef.indexOf(Content.BP_END)));
			
			Map<Content, Object> values = parser.getValues(
					TsvChunkParser.NEXT, areaRequest.requestedContents);
			
			if("chr1".equals((String)read.get(instructions.fileDef.indexOf(Content.CHROMOSOME))) && 
				areaRequest.intercepts(readReg)){
				
				//hits.add(read);
				
				createAreaResult(new AreaResult<RegionContent>(
						areaRequest.status, new RegionContent(readReg, values)));
			}					
		}
			
		//createAreaResult(new AreaResult<List<List<Object>>>(areaRequest.status, hits, instructions.fileDef));
	}
}