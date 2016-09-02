# RecommenderJ
A demonstration integrating the R "recommenderlab" package with Java

## Installation
* `git clone` the RecommenderJ repository
* Build: `mvn clean install package`
* R installation: `install.packages("/path/to/RecommenderJ", repos=NULL)`

## Use
* See the [recommenderlab paper](https://cran.r-project.org/web/packages/recommenderlab/vignettes/recommenderlab.pdf) for details on how to evaluate recommender engines in R.
* This package is a Java implementation loosely mimicing the included UBCF recommender.
 * supports Binary Ratings Matrices
 * implements Jaccard Similarity

## Example
```R
# load required libraries
> library(recommenderJ)
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
+ "Java UBCF" = list(name="JAVA", param=NULL))
> eval_sets <- evaluationScheme(data=Jester_binary, method="cross-validation", k=4, given=5)
n_recommendations <- c(1, 5, seq(10, 100, 10))
list_results <- evaluate(x=eval_sets, method=algorithms, n=n_recommendations)

# We can see the ROC curve and Precision/Recall plots, showing the Java version performs close to the built-in version.  The "popular" method is nearly as good while recommending random items performs poorly.
> plot(list_results, annotate = c(1,2,3), legend = "topleft")
> plot(list_results, "prec/rec", annotate = c(1,2,3), legend = "bottomright")
```
