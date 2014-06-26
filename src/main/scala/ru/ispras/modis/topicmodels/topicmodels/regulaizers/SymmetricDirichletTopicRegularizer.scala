package ru.ispras.modis.topicmodels.topicmodels.regulaizers


/**
 * Created by valerij on 6/26/14.
 */
class SymmetricDirichletTopicRegularizer(protected val alpha: Float) extends TopicsRegularizer with ShiftMatrix with SymmetricDirichletHelper {
    override def apply(topics: Array[Array[Float]]): Float = topics.foldLeft(0f)((sum, x) => sum + dirichletLogLikelihood(x))

    override def regilarize(topics: Array[Array[Float]], oldTopics: Array[Array[Float]]): Unit = {
        shift(topics, (matrix, i, j) => matrix(i)(j) += (alpha - 1))
        super.regilarize(topics, oldTopics: Array[Array[Float]])
    }
}
