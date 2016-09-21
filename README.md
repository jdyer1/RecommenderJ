# recommenderlab.java
A demonstration integrating the R "recommenderlab" package with Java

## Installation
* `git clone` the recommenderlab.java repository
* Build: `mvn clean install package`
* R installation: `install.packages("/path/to/recommenderlab.java", repos=NULL)`

## Use
* See the [recommenderlab paper](https://cran.r-project.org/web/packages/recommenderlab/vignettes/recommenderlab.pdf) for details on how to evaluate recommender engines in R.
* Use `name="JAVA"` with a Binary Ratings Matrix for a Java-based recommender similar to the built-in UBCF recommender, using Jaccard or Cosine Similarity
 * `method="COSINE"` for cosine similarity (Ochiai coefficient)
 * `method="JACCARD"` for Jaccard similarity
* Use `name="LUCENE"` with a Binary Ratings Matrix for a recommender built on Apache Lucene.  This will behave similar to the built-in UBCF recommender, using Cosine Similarity
 * `method="QUERY"` to find documents by building a query enumerated with stored field values (faster).
   * `index.field.type="POINT"` to index documents as int points (1 or 0).
   * `index.field.type="STRING"` to index documents as Strings ("1" or "0"), with tf-idf disabled.
 * `method="MLT"` to find documents using Lucene's More-Like-This feature (Term Vectors) (slower).

## Example
```R
# load required libraries
> library(recommenderlab.java)
Loading required package: rJava
Loading required package: recommenderlab
Loading required package: Matrix
Loading required package: arules

# create a binary matrix from the Jester5k dataset.  See section 5 of the recommenderlab paper for more information.
> data("Jester5k")
> Jester_binary <- binarize(Jester5k, minRating=5)
> Jester_binary <- Jester_binary[rowCounts(Jester_binary)>20]

# Set up the recommender comparison, comparing the built-in UBCF with this Java version, and also the "random" and "popular" algorithms.  We're using k-fold cross-validation, trying stepwise between 1 and 100 recommendations.
> algorithms <- list(
+ "random items" = list(name = "RANDOM", param=NULL),
+ "popular items" = list(name="POPULAR", param=NULL),
+ "built-in UBCF" = list(name="UBCF", param=list(nn=50)),
+ "Java UBCF Jaccard" = list(name="JAVA", param = list(nn=50, method="JACCARD")),
+ "Java UBCF Cosine" = list(name="JAVA", param = list(nn=50, method="COSINE")),
+ "Lucene UBCF MLT" = list(name="LUCENE", param = list(nn=25, path="/path/to/save/lucene/index/on/disk", method="MLT", index.field.type="POINT")),
+ "Lucene UBCF QUERY" = list(name="LUCENE", param = list(nn=25, path="/path/to/save/lucene/index/on/disk", method="QUERY")))
> eval_sets <- evaluationScheme(data=Jester_binary, method="cross-validation", k=4, given=5)
> n_recommendations <- c(1, 5, seq(10, 100, 10))
> list_results <- evaluate(x=eval_sets, method=algorithms, n=n_recommendations)

# We can see the ROC curve and Precision/Recall plots.  These show both the in-memory Java version and the Lucene version performing close to the built-in UBCF version.  The "popular" method is nearly as good while recommending random items performs poorly.
> plot(list_results, annotate = c(1,2,3,4,5,6,7), legend = "topleft")
> plot(list_results, "prec/rec", annotate = c(1,2,3,4,5,6,7), legend = "bottomright")
```
