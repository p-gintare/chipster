# TOOL classification-knn.R: "KNN classification" (K-nearest neighbor classification. If you have a separate test data set, you can validate the prediction with it by setting the validation type to predict. This function does not perform any gene selection, and the analysis is run for the selected data set.)
# INPUT normalized.tsv: normalized.tsv TYPE GENE_EXPRS 
# INPUT META phenodata.tsv: phenodata.tsv TYPE GENERIC 
# OUTPUT knn-cross-validation.tsv: knn-cross-validation.tsv 
# PARAMETER number.of.nearest.neighbors: number.of.nearest.neighbors TYPE INTEGER FROM 1 TO 1000 DEFAULT 2 (Number of nearest neighbors)
# PARAMETER number.of.votes: number.of.votes TYPE INTEGER FROM 0 TO 1000 DEFAULT 2 (Number of votes needed to get a definite answer)
# PARAMETER validation.type: validation.type TYPE [crossvalidate: crossvalidate, predict: predict] DEFAULT crossvalidate (Type of analysis)


# KNN classification
# JTT 26.6.2006

# Parameter settings (default) for testing purposes
#number.of.nearest.neighbors<-2
#number.of.votes<-2
#validation.type<-"crossvalidate"

# Renaming the variables
knn.type<-validation.type
k.no<-number.of.nearest.neighbors
k.vote<-number.of.votes

# Load the libraries
library(class)

# Loads the data
file<-c("normalized.tsv")
dat<-read.table(file, sep="\t", header=T, row.names=1)

# Reads the phenodata table
phenodata<-read.table("phenodata.tsv", header=T, sep="\t")

# Checks whether the training variable is present if phenodata
# If training part of phenodata has not been filled, but the column (header) is present, 
# all the chips belong to the training set
if(length(grep("training", names(phenodata)))>0) {
   tr<-phenodata$training
   if(tr[1]==" " | tr[1]=="" | any(is.na(tr))==T) {
	tr<-rep(1, nrow(phenodata))
   }
}

# Separates expression values and flags
calls<-dat[,grep("flag", names(dat))]
dat2<-dat[,grep("chip", names(dat))]

# Are the parameter values sensical?
if(k.no>length(dat2)) {
	stop("The number of neighbors is larger than the number of chips!")
}
if(k.vote>k.no) {
	stop("The number of votes needed to give a definitive answer is larger than the number of neighbors!")
}

if(validation.type=="predict") {
   # Which parts of the data are training and test sets?
   dat3<-split(as.data.frame(t(dat2)), tr) 
   train<-dat3$'1'
   test<-dat3$'2'
   # Which part of the phenodata are training and test set
   phenodata_training <- phenodata[phenodata$training==1,]
   if (knn.type=="predict") {
	phenodata_test <- phenodata[phenodata$training==2,]
   }
} else {
   train<-dat3
}

# Defines the true classification of the training set
if(validation.type=="predict") {
   cl<-phenodata_training$group
} else {
   cl<-phenodata$group
}

# Runs the KNN analysis and reports the results
if(knn.type=="crossvalidate") {
	knn.cross<-knn.cv(train=train, cl=cl, k=k.no, l=k.vote)
	# Writes a table of known versus predicted classes
	write.table(data.frame(sample=rownames(train), known.classes=cl, prediction=knn.cross), file="knn-cross-validation.tsv", sep="\t", row.names=F, col.names=T, quote=F)
}

if(knn.type=="predict") {
	cl_test<-phenodata_test$group
	knn.predict<-knn(train=train, test=test, cl=cl, k=k.no, l=k.vote)
	# Writes a table of known versus predicted classes 
	write.table(data.frame(sample=rownames(test), known.classes=cl_test, prediction=knn.predict), file="knn-cross-validation.tsv", sep="\t", row.names=F, col.names=T, quote=F)
}
