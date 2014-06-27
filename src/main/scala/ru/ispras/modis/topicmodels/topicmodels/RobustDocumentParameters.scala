package ru.ispras.modis.topicmodels.topicmodels

import breeze.linalg.SparseVector
import org.apache.spark.broadcast.Broadcast
import ru.ispras.modis.topicmodels.documents.Document
import ru.ispras.modis.topicmodels.topicmodels.regulaizers.DocumentOverTopicDistributionRegularizer
import ru.ispras.modis.topicmodels.utils.serialization.SparseVectorFasterSum

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 30.08.13
 * Time: 22:05
 */
/**
 * the class contains document parameter in Robust PLSA model
 *
 * @param document
 * @param theta the distribution over topics
 * @param noise noisiness of words
 * @param regularizer
 */
class RobustDocumentParameters(document: Document, theta: Array[Float],
                               val noise: SparseVector[Float],
                               regularizer: DocumentOverTopicDistributionRegularizer) extends DocumentParameters(document, theta, regularizer) {

    protected def getZ(topics: Broadcast[Array[Array[Float]]], background: Array[Float], eps: Float, gamma: Float) = {
        val topicsValue = topics.value
        val numberOfTopics = topicsValue.size

        var sum = 0f
        val Z = document.tokens.mapActivePairs {
            case (word, n) =>
                sum = (0 until numberOfTopics).foldLeft(0f)((sum, topic) => sum + topicsValue(topic)(word) * theta(topic))
                (eps * noise(word) + gamma * background(word) + sum) / (1 + eps + gamma)
        }
        Z
    }

    def wordsFromTopicsAndWordsFromBackground(topics: Broadcast[Array[Array[Float]]], background: Array[Float], eps: Float, gamma: Float): (Array[SparseVector[Float]], SparseVector[Float]) = {
        val Z = getZ(topics, background, eps, gamma)

        (super.wordsToTopicCnt(topics, Z), wordToBackgroundCnt(background, eps, gamma, Z))
    }


    def wordToBackgroundCnt(background: Array[Float], eps: Float, gamma: Float, Z: SparseVector[Float]): SparseVector[Float] = {
        document.tokens.mapActivePairs {
            case (word, num) => num * background(word) * gamma / Z(word)
        }
    }


    protected def getNoise(eps: Float, Z: SparseVector[Float]) = {
        val newWordsFromNoise = document.tokens.mapActivePairs {
            case (word, num) => eps * noise(word) * num / Z(word)
        }

        val noiseWordsNum = newWordsFromNoise.sum

        if (noiseWordsNum > 0) newWordsFromNoise.mapActiveValues(_ / noiseWordsNum) else newWordsFromNoise.mapActiveValues(i => 0f)
    }

    /**
     * calculates a new distribution of this document by topic, corresponding to the new topics
     */
    def getNewTheta(topicsBC: Broadcast[Array[Array[Float]]], background: Array[Float], eps: Float, gamma: Float) = {
        val Z = getZ(topicsBC, background, eps, gamma)

        super.assignNewTheta(topicsBC, Z)

        val newNoise: SparseVector[Float] = getNoise(eps, Z)
        new RobustDocumentParameters(document, theta, newNoise, regularizer)
    }


}

/**
 * companion object of DocumentParameters. Create new DocumentParameters and contain some methods
 */
object RobustDocumentParameters extends SparseVectorFasterSum {
    /**
     * create new DocumentParameters
     * @param document
     * @param numberOfTopics
     * @param gamma weight of background
     * @param eps weight of noise
     * @return new DocumentParameters
     */
    def apply(document: Document, numberOfTopics: Int, gamma: Float, eps: Float, regularizer: DocumentOverTopicDistributionRegularizer) = {
        val wordsNum = sumSparseVector(document.tokens)
        val noise = document.tokens.mapActiveValues(word => 1f / wordsNum)

        val documentParameters: DocumentParameters = DocumentParameters(document, numberOfTopics, regularizer)
        new RobustDocumentParameters(document, documentParameters.theta, noise, regularizer)
    }

}
