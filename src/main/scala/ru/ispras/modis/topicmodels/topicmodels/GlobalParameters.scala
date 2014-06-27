package ru.ispras.modis.topicmodels.topicmodels

import org.apache.spark.broadcast.Broadcast


/**
 * holds \Phi matrix
 * @param topicWords
 * @param alphabetSize
 */
class GlobalParameters(val topicWords: Array[Array[Float]], alphabetSize: Int) extends Serializable {

    /**
     * merges two GlobalParameters into a single one
     * @param that other GlobalParameters
     * @return GlobalParameters
     */
    def +(that: GlobalParameters) = {
        topicWords.zip(that.topicWords).foreach {
            case (thisOne, otherOne) =>
                (0 until alphabetSize).foreach(i => thisOne(i) += otherOne(i))
        }

        new GlobalParameters(topicWords, alphabetSize)
    }

    /**
     * calculates and add local parameters to global parameters
     * @param that DocumentParameters.
     * @param topics broadcasted words by topics distribution
     * @param alphabetSize number of unique words
     * @return GlobalParameters
     */
    def add(that: DocumentParameters, topics: Broadcast[Array[Array[Float]]], alphabetSize: Int) = {

        val wordsFromTopic = that.wordsFromTopics(topics)

        wordsFromTopic.zip(topicWords).foreach {
            case (topic, words) => topic.activeIterator.foreach {
                case (word, num) => words(word) += num
            }
        }

        this
    }
}

object GlobalParameters {
    def apply(topicNum: Int, alphabetSize: Int) = {
        val topicWords = (0 until topicNum).map(i => new Array[Float](alphabetSize)).toArray
        new GlobalParameters(topicWords, alphabetSize)
    }
}