# TOOL bowtie2-with-index-building.R: "Bowtie2 for single end reads and own genome" (Bowtie2 aligns reads to genomes or transcriptomes. Results are sorted and indexed bam files, which are ready for viewing in the Chipster genome browser.)
# INPUT reads1.fq: "Reads" TYPE GENERIC
# INPUT genome.txt: "Genome to align against" TYPE GENERIC
# OUTPUT bowtie2.bam 
# OUTPUT bowtie2.bam.bai 
# OUTPUT bowtie2.log 
# OUTPUT OPTIONAL unaligned_1.fq
# PARAMETER strategy: "Alignment strategy to use" TYPE [--very-fast: "Very fast", --fast: "Fast", --sensitive: "Sensitive", --very-sensitive: "Very sensitive", --very-fast-local: "Very fast local", -fast-local: "Fast local", --sensitive-local: "Sensitive local", --very-sensitive-local: "Very sensitive local"] DEFAULT --sensitive (The alignment strategy to be used. Bowtie2 can map the reads using end-to-end or local alignments. When local alignment is used, Bowtie2 might "trim" or "clip" some read characters from one or both ends of the alignment if doing so maximizes the alignment score. Bowtie2 uses heuristics for mapping the reads to the reference genome. Several Bowtie2 parameters affect simultaneously both to the sensitivity and to computing time. In Chipster you can choose the sensitivity level with a set of pre-defined parameter combinations that allow you to tune the balance between the computing time and mapping sensitivity.)
# PARAMETER quality.format: "Quality value format used" TYPE [--phred33: "Sanger - Phred+33", --phred64: "Illumina GA v1.3-1.5 - Phred+66", --ignore-quals: "Fixed 30 for all"] DEFAULT --phred33 (Quality scale used in the fastq-file.)
# PARAMETER alignment.no: "How many valid alignments are reported per read" TYPE [0: "Best based on the mapping quality", 1: "1", 2: "2", 3: "3", 4: "4", 5: "5", 6: "All alignments"] DEFAULT 0 (By default, Bowtie2 reports only the best alignment of the read (based on the mapping quality\). Optionally, if there are several, equally good alignments, you can choose how many of them should be reported?)
# PARAMETER OPTIONAL unaligned.file: "Put unaligned reads to a separate file" TYPE [yes, no] DEFAULT no (Would you like to store unaligned reads to a new fastq file? Note that also multireads will be added to this file, unless you asked them to be put to a separate file.)
# PARAMETER OPTIONAL ma: "Match bonus" TYPE INTEGER FROM 0 TO 10 DEFAULT 2 (Match bonus for a match in local alignment. Default value 2) 
# PARAMETER OPTIONAL mp: "Maximum penalty for mismatch" TYPE INTEGER FROM 0 TO 20 DEFAULT 6 (Maximum penalty for mismatch; lower quality = lower penalty. Default value 6)
# PARAMETER OPTIONAL np: "Penalty for non-ACGTs"  TYPE INTEGER FROM 0 TO 20 DEFAULT 1 ( Sets penalty for positions where the read, reference, or both, contain an ambiguous character such as N. Default: 1.) 
# PARAMETER OPTIONAL rdg.open: "Gap opening penalty for the reads" TYPE INTEGER FROM 0 TO 20 DEFAULT 5 (Gap opening penalty for the reads. Default value: 5. )
# PARAMETER OPTIONAL rdg.ext: "Gap extension penalty for the reads" TYPE INTEGER FROM 0 TO 20 DEFAULT 3 (Gap extension penalty for the reads. Default value: 3. )
# PARAMETER OPTIONAL rfg.open: "Gap opening penalty for the reference" TYPE INTEGER FROM 0 TO 20 DEFAULT 5 (Gap opening penalty for the reference. Default value: 5. )
# PARAMETER OPTIONAL rfg.ext: "Gap extension penalty for the reference" TYPE INTEGER FROM 0 TO 20 DEFAULT 3 (Gap extension penalty for the reference. Default value: 3. )

# KM 23.10.2012
# EK 8.5.2013 replaced samtools -q 1 with Bowtie --no-unal to remove unaligned reads from BAM

# check out if the file is compressed and if so unzip it
source(file.path(chipster.common.path, "zip-utils.R"))
unzipIfGZipFile("reads1.fq")

# bowtie2
bowtie.binary <- c(file.path(chipster.tools.path, "bowtie2", "bowtie2"))
bowtie2.index.binary <- file.path(chipster.module.path, "shell", "check_bowtie2_index.sh")

# Do indexing
print("Indexing the genome...")
system("echo Indexing the genome... > bowtie2.log")
check.command <- paste ( bowtie2.index.binary, "genome.txt| tail -1 ")
genome.dir <- system(check.command, intern = TRUE)
bowtie2.genome <- file.path( genome.dir , "genome.txt")
#bowtie.genome <- c(file.path(chipster.tools.path, "bowtie2", "indexes" , genome))

command.start <- paste("bash -c '", bowtie.binary)
rdg.value <- paste (rdg.open ,rdg.ext , sep=",")
rfg.value <- paste (rfg.open ,rfg.ext , sep=",")

parameters <- paste(strategy, "--mp", mp,"--np", np, "--rdg", rdg.value, "--rfg", rfg.value, quality.format, "--no-unal")

if ( alignment.no>0){
	if ( alignment.no==6){
		parameters <- paste(parameters, "--all")
	}
	if ( alignment.no<6){
		parameters <- paste(parameters, "-k", alignment.no )
	}
}

# Local alignment specific parameters 
if (strategy == "--very-fast-local" || strategy == "--fast-local" || strategy == "--sensitive-local" || strategy == "--very-sensitive-local" ) {
	parameters <- paste(parameters ,"--local --ma", ma)
}

if (unaligned.file== "yes"){
	parameters <- paste(parameters, "--un unaligned")
}


# output parameters
#output.parameters <- paste(unaligned.output, multiread.output)
#stop(paste('CHIPSTER-NOTE: ', parameters))
# command ending
command.end <- paste("-x", bowtie2.genome, "-U reads1.fq 1> alignment.sam 2>> bowtie2.log'")

# run bowtie
bowtie.command <- paste(command.start, parameters, command.end)
#stop(paste('CHIPSTER-NOTE: ', bowtie.command))

echo.command <- paste("echo '", bowtie.command , "' >> bowtie2.log" )
system(echo.command)
system(bowtie.command)
system ("ls -l >>  bowtie2.log")
# samtools binary
samtools.binary <- c(file.path(chipster.tools.path, "samtools", "samtools"))

# convert sam to bam
system(paste(samtools.binary, "view -bS alignment.sam -o alignment.bam"))

# sort bam
system(paste(samtools.binary, "sort alignment.bam alignment.sorted"))

# index bam
system(paste(samtools.binary, "index alignment.sorted.bam"))

# rename result files
system("mv alignment.sorted.bam bowtie2.bam")
system("mv alignment.sorted.bam.bai bowtie2.bam.bai")

if (unaligned.file== "yes"){
	system("mv unaligned.1 unaligned_1.fq")
}

