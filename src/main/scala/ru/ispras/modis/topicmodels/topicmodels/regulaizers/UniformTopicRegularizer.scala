package ru.ispras.modis.topicmodels.topicmodels.regulaizers

/**
 * Created by valerij on 6/26/14.
 */
class UniformTopicRegularizer extends TopicsRegularizer {
    override def apply(topics: Array[Array[Float]]): Float = 0

    override def regilarize(topics: Array[Array[Float]], oldTopics: Array[Array[Float]]): Unit = {}
}
