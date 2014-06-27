package ru.ispras.modis.topicmodels.topicmodels.regulaizers


/**
 * Created by valerij on 6/26/14.
 */

/**
 *
 * @param alpha - parmeter of Dirichlet distribution
 */
class SymmetricDirichletTopicRegularizer(protected val alpha: Float) extends TopicsRegularizer with MatrixInPlaceModification with SymmetricDirichletHelper {
    override def apply(topics: Array[Array[Float]]): Float = topics.foldLeft(0f)((sum, x) => sum + dirichletLogLikelihood(x))

    override def regularize(topics: Array[Array[Float]], oldTopics: Array[Array[Float]]): Unit = {
        shift(topics, (matrix, i, j) => matrix(i)(j) += (alpha - 1))
        super.regularize(topics, oldTopics: Array[Array[Float]])
    }
}
