package ru.ispras.modis.topicmodels.topicmodels.regulaizers

/**
 * Created by valerij on 6/26/14.
 */

/**
 * Defines a prior distribution (possibly, improper) on \Phi matrix  (words over topics)
 */
trait TopicsRegularizer extends MatrixInPlaceModification {
    /**
     *
     * @param topics \Phi matrix
     * @return log prior probability of \Phi. Is used for perplexity calculation only
     */
    def apply(topics: Array[Array[Float]]): Float

    /**
     * This implementation performs a positive cut on every element 
     * @param topicsCnt number of times the word was generated by a topic (n_{wt} in Vorontsov's notation)
     * @param oldTopics
     */
    def regularize(topicsCnt: Array[Array[Float]], oldTopics: Array[Array[Float]]): Unit = shift(topicsCnt, (x, i, j) => x(i)(j) = math.max(0, x(i)(j)))
}
