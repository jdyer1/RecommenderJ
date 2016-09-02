.onLoad <- function(libname, pkgname) {
	.jpackage(pkgname, jars='*')
	recommenderRegistry$set_entry(
			method="JAVA", dataType = "binaryRatingMatrix", fun=BIN_JAVA,
			description="UBCF-Binary Data Java Implementation."
	)	
	print("method=JAVA added to recommenderlab for User-based collaborative filtering on binary matrices using Jaccard similarity.")	
}

