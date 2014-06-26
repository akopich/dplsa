package ru.ispras.modis.topicmodels.topicmodels.regulaizers

/**
 * Created by valerij on 6/26/14.
 */
class SymmetricDirichletDocumentOverTopicDistributionRegularizer(protected val alpha: Float)
    extends DocumentOverTopicDistributionRegularizer
    with SymmetricDirichletHelper
    with ShiftMatrix {
    override def apply(theta: Array[Float]): Float = dirichletLogLikelihood(theta)

    override def regularize(theta: Array[Float], oldTheta: Array[Float]) = {
        shift(theta, (theta, i) => theta(i) += alpha - 1)
        super.regularize(theta, oldTheta)
    }
}
