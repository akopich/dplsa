package ru.ispras.modis.topicmodels.topicmodels

import breeze.linalg.SparseVector
import org.apache.spark.broadcast.Broadcast
import ru.ispras.modis.topicmodels.documents.Document
import ru.ispras.modis.topicmodels.topicmodels.regulaizers.DocumentOverTopicDistributionRegularizer
import ru.ispras.modis.topicmodels.utils.serialization.SparseVectorFasterSum

/**
 * Created by valerij on 6/25/14.
 */
class DocumentParameters(val document: Document, val theta: Array[Float], private val regularizer: DocumentOverTopicDistributionRegularizer) extends Serializable {
    protected def getZ(topics: Broadcast[Array[Array[Float]]]) = {
        val topicsValue = topics.value
        val numberOfTopics = topicsValue.size

        document.tokens.mapActivePairs {
            case (word, n) =>
                (0 until numberOfTopics).foldLeft(0f)((sum, topic) => sum + topicsValue(topic)(word) * theta(topic))
        }
    }

    def wordsFromTopics(topics: Broadcast[Array[Array[Float]]]): Array[SparseVector[Float]] = {
        val Z = getZ(topics)

        wordsToTopicCnt(topics, Z)
    }

    private[topicmodels] def wordsToTopicCnt(topics: Broadcast[Array[Array[Float]]], Z: SparseVector[Float]): Array[SparseVector[Float]] = {
        val array = Array.ofDim[SparseVector[Float]](theta.size)
        forWithIndex(theta)((topicWeight, topicNum) =>
            array(topicNum) = document.tokens.mapActivePairs {
                case (word, num) => num * topics.value(topicNum)(word) * topicWeight / Z(word)
            }
        )
        array
    }

    protected def forWithIndex(array: Array[Float])(operation: (Float, Int) => Unit) {
        var i = 0
        val size = array.size
        while (i < size) {
            operation(array(i), i)
            i += 1
        }
    }

    private[topicmodels] def assignNewTheta(topics: Broadcast[Array[Array[Float]]], Z: SparseVector[Float]) {
        val newTheta: Array[Float] = {
            val array = Array.ofDim[Float](theta.size)
            forWithIndex(theta)((weight, topicNum) => array(topicNum) = weight * document.tokens.activeIterator.foldLeft(0f) {
                case (sum, (word, wordNum)) => sum + wordNum * topics.value(topicNum)(word) / Z(word)
            })
            array
        }
        regularizer.regularize(newTheta, theta)

        val newThetaSum = newTheta.sum

        forWithIndex(newTheta)((wordsNum, topicNum) => theta(topicNum) = wordsNum / newThetaSum)

    }

    def getNewTheta(topicsBC: Broadcast[Array[Array[Float]]]) = {
        val Z = getZ(topicsBC)
        assignNewTheta(topicsBC, Z)

        this
    }

    def priorThetaLogProbability = regularizer(theta)

}


object DocumentParameters extends SparseVectorFasterSum {

    def apply(document: Document, numberOfTopics: Int, regularizer: DocumentOverTopicDistributionRegularizer) = {
        val theta = getTheta(numberOfTopics)
        new DocumentParameters(document, theta, regularizer)
    }

    private def getTheta(numberOfTopics: Int) = {
        Array.fill[Float](numberOfTopics)(1f / numberOfTopics)
    }
}