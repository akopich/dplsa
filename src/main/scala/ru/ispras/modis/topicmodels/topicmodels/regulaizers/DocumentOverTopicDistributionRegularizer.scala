package ru.ispras.modis.topicmodels.topicmodels.regulaizers

/**
 * Created by valerij on 6/26/14.
 */
trait DocumentOverTopicDistributionRegularizer extends Serializable with ShiftMatrix {
    def apply(theta: Array[Float]): Float

    def regularize(theta: Array[Float], oldTheta: Array[Float]): Unit = shift(theta, (x, i) => x(i) = math.max(0, x(i)))
}
