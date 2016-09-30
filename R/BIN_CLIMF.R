
.BIN_CLIMF_param <- list(
		dimensionality = 10,
		lambda=.001,
		gamma=.0001,
		maxIterations=25
)

BIN_CLIMF <- function(data, parameter = NULL) {
	p <- getParameters(.BIN_CLIMF_param, parameter)
	fileLoader <- .jnew("rl4j/FileLoader")	
	dataJ <- fileLoader$importData(as.vector(data@data@data), data@data@itemsetInfo$itemsetID, data@data@itemInfo$labels)	
	CCF <- .jnew("rl4j/ClimfCollaborativeFilter", dataJ, 1.0, as.integer(p$dimensionality), p$lambda, p$gamma, as.integer(p$maxIterations))
	model <- list(description = "CLIMF-Binary Data Java Implementation", data = data, CCF=CCF)
	
	predict <- function(model, newdata, n=10, data=NULL, type=c("topNList"),...) {  
		dataJtest <- fileLoader$importData(as.vector(newdata@data@data), newdata@data@itemsetInfo$itemsetID, newdata@data@itemInfo$labels)
		topNJ <- model$CCF$recommendationsAsTopNList(dataJtest, as.integer(0), as.integer(n))
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
	new("Recommender", method = "CLIMF", dataType = class(data),
			ntrain = nrow(data), model = model, predict = predict)
}
