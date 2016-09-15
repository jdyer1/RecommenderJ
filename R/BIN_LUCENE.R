
.BIN_LUCENE_param <- list(
  path = "/tmp/BIN_LUCENE",
  nn = 25
)

BIN_LUCENE <-
function(data, parameter = NULL) {
	p <- getParameters(.BIN_LUCENE_param, parameter)
	
	fileLoader <- .jnew("rl4j/FileLoader")
	indexer <- .jnew("rl4j/lucene/Indexer", p$path)
	
	dataJ <- indexer$importData(as.vector(data@data@data), data@data@itemsetInfo$itemsetID, data@data@itemInfo$labels, TRUE)
	LCF <- .jnew("rl4j/lucene/LuceneCollaborativeFilter", p$path, 1.0)	
 	model <- list(description = "UBCF-Binary Data Lucene Implementation", data = data, LCF=LCF, nn=p$nn)
  
  predict <- function(model, newdata, n=10, data=NULL, type=c("topNList"),...) {  
    dataJtest <- fileLoader$importData(as.vector(newdata@data@data), newdata@data@itemsetInfo$itemsetID, newdata@data@itemInfo$labels)
    topNJ <- model$LCF$recommendationsAsTopNList(dataJtest, as.integer(model$nn), as.integer(n))
	itemMatrix <- matrix(topNJ$items, ncol=n)
	ratingMatrix <- matrix(topNJ$ratings, ncol=n)
	itemList <- lapply((split(itemMatrix, rep(1:nrow(itemMatrix), each = ncol(itemMatrix)))), function(xx) { xx[xx!=0] })
	ratingList <- lapply((split(ratingMatrix, rep(1:nrow(ratingMatrix), each=ncol(ratingMatrix)))), function(xx) { xx[xx!=0]})
	names(itemList) <- newdata@data@itemsetInfo$itemsetID
	names(ratingList) <- newdata@data@itemsetInfo$itemsetID
	topN <- new("topNList", items=itemList, ratings=ratingList, itemLabels = colnames(newdata), n = as.integer(n))
	return(topN)
  }
  
  ## construct recommender object
  new("Recommender", method = "LUCENE", dataType = class(data),
      ntrain = nrow(data), model = model, predict = predict)
}
