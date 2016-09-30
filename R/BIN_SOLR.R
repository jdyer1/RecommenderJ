
.BIN_SOLR_param <- list(	
		deletePriorData = TRUE,
		solrHosts = "zkHost1:9983,zkHost2:9983",
		solrCloud = TRUE,
		solrCollectionName = "users",
		idFieldName = "id",
		dataFieldName = "items",
		nn = 25
)

BIN_SOLR <- function(data, parameter = NULL) {
	p <- getParameters(.BIN_SOLR_param, parameter)
	fileLoader <- .jnew("rl4j/FileLoader")
	SCF <- .jnew("rl4j/solr/SolrCollaborativeFilter", p$solrHosts, p$solrCloud, p$solrCollectionName, p$idFieldName, p$dataFieldName, 1.0)		
	dataJ <- SCF$indexTrainingData(as.vector(data@data@data), data@data@itemsetInfo$itemsetID, data@data@itemInfo$labels, p$deletePriorData)	
	model <- list(description = "UBCF-Binary Data Solr Implementation", data = data, SCF=SCF, nn=p$nn)
	
	predict <- function(model, newdata, n=10, data=NULL, type=c("topNList"),...) {  
		dataJtest <- fileLoader$importData(as.vector(newdata@data@data), newdata@data@itemsetInfo$itemsetID, newdata@data@itemInfo$labels)
		topNJ <- model$SCF$recommendationsAsTopNList(dataJtest, as.integer(model$nn), as.integer(n))
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
	new("Recommender", method = "SOLR", dataType = class(data),
			ntrain = nrow(data), model = model, predict = predict)
}
