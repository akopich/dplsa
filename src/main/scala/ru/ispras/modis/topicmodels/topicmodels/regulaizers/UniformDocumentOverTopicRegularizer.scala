package ru.ispras.modis.topicmodels.topicmodels.regulaizers

/**
 * Created by valerij on 6/26/14.
 */

/**
 * usage of this prior is equivalent to use of no prior
 */
class UniformDocumentOverTopicRegularizer extends DocumentOverTopicDistributionRegularizer {
    override def apply(theta: Array[Float]): Float = 0

    override def regularize(theta: Array[Float], oldTheta: Array[Float]): Unit = {}
}
