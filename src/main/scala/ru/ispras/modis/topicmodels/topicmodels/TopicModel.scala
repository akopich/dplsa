package ru.ispras.modis.topicmodels.topicmodels

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import ru.ispras.modis.topicmodels.documents.Document


/**
 * Created with IntelliJ IDEA.
 * User: valerij
 * Date: 2/13/13
 * Time: 5:56 PM
 */

/**
 * topic modeling interface
 */
trait TopicModel {
    /**
     *
     * @param documents  -- document collection
     * @return a pair of theta (documents to topic) and phi (words to topics)
     */
    def infer(documents: RDD[Document]): (RDD[TopicDistribution], Broadcast[Array[Array[Float]]])
}
