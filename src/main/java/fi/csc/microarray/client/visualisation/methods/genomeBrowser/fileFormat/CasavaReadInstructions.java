package fi.csc.microarray.client.visualisation.methods.genomeBrowser.fileFormat;

import java.util.Arrays;

public class CasavaReadInstructions extends ConstantLengthReadInstructions{
	
	/* Pipeline CASAVA output
	 * 1. Machine (Parsed from Run Folder name)
2. Run Number (Parsed from Run Folder name)
3. Lane
4. Tile
5. X Coordinate of cluster
6. Y Coordinate of cluster
7. Index string (Blank for a non-indexed run)
8. Read number (1 or 2 for paired-read analysis, blank for a single-read analysis)
9. Read
10.Quality string—In symbolic ASCII format (ASCII character code = quality value + 64)
11.Match chromosome—Name of chromosome match OR code indicating why no
match resulted
12.Match Contig—Gives the contig name if there is a match and the match chromosome
is split into contigs (Blank if no match found)
13.Match Position—Always with respect to forward strand, numbering starts at 1 (Blank if
no match found)
14.Match Strand—“F” for forward, “R” for reverse (Blank if no match found)
15.Match Descriptor—Concise description of alignment (Blank if no match found)
• A numeral denotes a run of matching bases
• A letter denotes substitution of a nucleotide:
For a 35 base read, “35” denotes an exact match and “32C2” denotes substitution of
a “C” at the 33rd position
16.Single-Read Alignment Score—Alignment score of a single-read match, or for a
paired read, alignment score of a read if it were treated as a single read. Blank if no
match found; any scores less than 4 should be considered as aligned to a repeat
17.Paired-Read Alignment Score—Alignment score of a paired read and its partner,
taken as a pair. Blank if no match found; any scores less than 4 should be considered
as aligned to a repeat
18.Partner Chromosome—Name of the chromosome if the read is paired and its partner
aligns to another chromosome (Blank for single-read analysis)
19.Partner Contig—Not blank if read is paired and its partner aligns to another
chromosome and that partner is split into contigs (Blank for single-read analysis)
20.Partner Offset—If a partner of a paired read aligns to the same chromosome and
contig, this number, added to the Match Position, gives the alignment position of the
partner (Blank for single-read analysis)
21.Partner Strand—To which strand did the partner of the paired read align? “F” for
forward, “R” for reverse (Blank if no match found, blank for single-read analysis)
22.Filtering—Did the read pass quality filtering? “Y” for yes, “N” for no

	 
	int[] fieldLengths = new int[] { 16, 8, 8, 8, 16, 16, 2, 2, 64, 64, 32, 16, 16, 2, 16, 16, 2, 2, 2, 2, 2, 2, 2, 2};
	
	*/

	public static FileDefinition fileDef = new FileDefinition(
			Arrays.asList(
					new DataFieldDef[] {

							new DataFieldDef(Content.SKIP, Type.STRING, 16),
							new DataFieldDef(Content.SKIP, Type.STRING, 8),
							new DataFieldDef(Content.SKIP, Type.STRING, 8),
							new DataFieldDef(Content.SKIP, Type.STRING, 8),
							new DataFieldDef(Content.SKIP, Type.STRING, 16),
							new DataFieldDef(Content.SKIP, Type.STRING, 16),
							new DataFieldDef(Content.SKIP, Type.STRING, 2),
							new DataFieldDef(Content.SKIP, Type.STRING, 2),
							new DataFieldDef(Content.SEQUENCE, Type.STRING, 64),
							new DataFieldDef(Content.SKIP, Type.STRING, 64),
							new DataFieldDef(Content.CHROMOSOME, Type.STRING, 32),
							new DataFieldDef(Content.SKIP, Type.STRING, 16),						
							new DataFieldDef(Content.BP_START, Type.LONG, 16),
							new DataFieldDef(Content.STRAND, Type.STRING, 2),
							new DataFieldDef(Content.QUALITY, Type.STRING, 16),
							new DataFieldDef(Content.SKIP, Type.STRING, 16),
							new DataFieldDef(Content.SKIP, Type.STRING, 2),
							new DataFieldDef(Content.SKIP, Type.STRING, 2),
							new DataFieldDef(Content.SKIP, Type.STRING, 2),
							new DataFieldDef(Content.SKIP, Type.STRING, 2),
							new DataFieldDef(Content.SKIP, Type.STRING, 2),
							new DataFieldDef(Content.SKIP, Type.STRING, 2),
							new DataFieldDef(Content.SKIP, Type.NEWLINE, 1)
							
			}));

	

	public CasavaReadInstructions() {
		super(fileDef);		
	}
}
