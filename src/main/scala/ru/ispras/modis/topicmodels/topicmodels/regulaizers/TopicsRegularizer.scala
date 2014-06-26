package ru.ispras.modis.topicmodels.topicmodels.regulaizers

/**
 * Created by valerij on 6/26/14.
 */
trait TopicsRegularizer extends ShiftMatrix {
    def apply(topics: Array[Array[Float]]): Float

    def regularize(topics: Array[Array[Float]], oldTopics: Array[Array[Float]]): Unit = shift(topics, (x, i, j) => x(i)(j) = math.max(0, x(i)(j)))
}
