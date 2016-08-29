BIN_JAVA <-
function(data, parameter = NULL) {
  .jinit(classpath="/home/jdyer1/eclipse/other/RecommenderLab/bin:/home/jdyer1/eclipse/other/RecommenderLab/lib/mtj-1.0.4.jar")
	fileLoader <- .jnew("rl4j/FileLoader")

	dataJ <- fileLoader$importData(as.vector(data@data@data), data@data@itemsetInfo$itemsetID, data@data@itemInfo$labels)
	e <- .jgetEx()
	if(.jcheck(silent = TRUE)) {
		print(e)
	}

  UBCF <- .jnew("rl4j/UserBasedCollaborativeFilter", dataJ, 1.0) 
	e <- .jgetEx()
	if(.jcheck(silent = TRUE)) {
		print(e)
	} 
  model <- list(description = "UBCF-Binary Data Java Implementation", data = data, UBCF=UBCF)
  
  predict <- function(model, newdata, n=10, data=NULL, type=c("topNList"),...) {  
    dataJtest <- fileLoader$importData(as.vector(newdata@data@data), newdata@data@itemsetInfo$itemsetID, newdata@data@itemInfo$labels)
    topNJ <- model$UBCF$recommendationsAsTopNList(dataJtest, as.integer(n))
		e <- .jgetEx()
		if(.jcheck(silent = TRUE)) {
			print(e)
		}
		
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
  new("Recommender", method = "JAVA", dataType = class(data),
      ntrain = nrow(data), model = model, predict = predict)
}
