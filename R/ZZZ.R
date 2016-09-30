.onLoad <- function(libname, pkgname) {
	.jpackage(pkgname, jars='*')
	recommenderRegistry$set_entry(
			method="JAVA", dataType = "binaryRatingMatrix", fun=BIN_JAVA,
			description="UBCF-Binary Data Java Implementation."
	)	
	print("method=JAVA added to recommenderlab for User-based collaborative filtering on binary matrices using Jaccard similarity.")	
	
	recommenderRegistry$set_entry(
			method="LUCENE", dataType = "binaryRatingMatrix", fun=BIN_LUCENE,
			description="Lucene-Based Implementation."
	)	
	print("method=LUCENE added to recommenderlab for User-based collaborative filtering on binary matrices.")	
	
	recommenderRegistry$set_entry(
			method="SOLR", dataType = "binaryRatingMatrix", fun=BIN_SOLR,
			description="Solr-Based Implementation."
	)	
	print("method=SOLR added to recommenderlab for User-based collaborative filtering on binary matrices.")	
	
	recommenderRegistry$set_entry(
			method="CLIMF", dataType = "binaryRatingMatrix", fun=BIN_CLIMF,
			description="CLIMF Bindar Data Java Implementation."
	)	
	print("method=CLIMF added to recommenderlab for User-based collaborative filtering on binary matrices.")	
}

