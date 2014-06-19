package ru.ispras.modis.topicmodels.topicmodels

import breeze.linalg.SparseVector
import org.apache.spark.broadcast.Broadcast
import ru.ispras.modis.topicmodels.documents.Document

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 30.08.13
 * Time: 22:05
 */
/**
 * the class contains document parameter
 * @param document 
 *@param theta distribution of the document by topics (theta)
 * @param noise noisiness of words
 */
class DocumentParameters(val document: Document,
                         val theta: Array[Float],
                         val noise: SparseVector[Float]) extends Serializable {

    protected def getZ(topics: Broadcast[Array[Array[Float]]], background: Array[Float], eps: Float, gamma: Float) = {
        val topicsValue = topics.value
        val numberOfTopics = topicsValue.size

        var sum = 0f
        val Z = document.tokens.mapActivePairs {
            case (word, n) =>
                sum = (0 until numberOfTopics).foldLeft(0f)((sum, topic)=>  sum + topicsValue(topic)(word) * theta(topic))
                (eps * noise(word) + gamma * background(word) + sum) / (1 + eps + gamma)
        }
        Z
    }

  /**
   * calculates new topics and background
   */
    def wordsFromTopicsAndWordsFromBackground(topics: Broadcast[Array[Array[Float]]], background: Array[Float], eps: Float, gamma: Float): (Array[SparseVector[Float]], SparseVector[Float]) = {
        // return wordsFromTopics and wordsFromBackground
        val Z = getZ(topics, background, eps, gamma)

        val newWordsFromTopic: Array[SparseVector[Float]] = {
            val array = Array.ofDim[SparseVector[Float]](theta.size)
            forWithIndex(theta)((topicWeight, topicNum) =>
                array(topicNum) = document.tokens.mapActivePairs {
                    case (word, num) => num * topics.value(topicNum)(word) * topicWeight / Z(word)
                }
            )
            array
        }


        val newWordsFromBackground: SparseVector[Float] = document.tokens.mapActivePairs {
            case (word, num) => num * background(word) * gamma / Z(word)
        }

        (newWordsFromTopic, newWordsFromBackground)
    }


    protected def getTheta(topics: Broadcast[Array[Array[Float]]], background: Array[Float], eps: Float, gamma: Float, Z: SparseVector[Float]) {
        val newWordsFromTopic: Array[Float] = {
            val array = Array.ofDim[Float](theta.size)
            forWithIndex(theta)((weight, topicNum) => array(topicNum) = weight * document.tokens.activeIterator.foldLeft(0f) {
                case (sum, (word, wordNum)) => sum + wordNum * topics.value(topicNum)(word) / Z(word)
            })
            array
        }
        val wordsByTopics = newWordsFromTopic.sum

        forWithIndex(newWordsFromTopic)((wordsNum, topicNum) => theta(topicNum) = wordsNum / wordsByTopics)
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

        getTheta(topicsBC, background, eps, gamma, Z)

        val newNoise: SparseVector[Float] = getNoise(eps, Z)
        new DocumentParameters(document, theta, newNoise)
    }

    private def forWithIndex(array: Array[Float])(operation: (Float, Int) => Unit) {
        var i = 0
        val size = array.size
        while (i < size) {
            operation(array(i), i)
            i += 1
        }
    }

}

/**
 * companion object of DocumentParameters. Create new DocumentParameters and contain some methods
 */
object DocumentParameters {
  /**
   * create new DocumentParameters
   * @param document
   * @param numberOfTopics
   * @param gamma weight of background
   * @param eps weight of noise
   * @param alphabetSize number of unique words in collection
   * @return new DocumentParameters
   */
    def apply(document: Document, numberOfTopics: Int, gamma: Float, eps: Float, alphabetSize: Int) = {
        val theta = getTheta(numberOfTopics)

        val wordsNum = sumSparseVector(document.tokens)
        val noise = document.tokens.mapActiveValues(word => 1f / wordsNum)
        new DocumentParameters(document, theta, noise)
    }

    private def sumSparseVector(vector: SparseVector[Short]) = {
        var sum = 0
        for (element <- vector) sum += element
        sum
    }


    private def getTheta(numberOfTopics: Int) = {
        Array.fill[Float](numberOfTopics)(1f / numberOfTopics)
    }
}
