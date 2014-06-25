package ru.ispras.modis.topicmodels.topicmodels

import java.util.Random

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

/**
 * Created by valerij on 6/25/14.
 */
trait PLSACommon[DocumentParameterType <: DocumentParameters, GlobalParameterType <: GlobalParameters] {
    protected val numberOfTopics: Int
    protected val random: Random

    protected def generalizedPerplexity(topicsBC: Broadcast[Array[Array[Float]]], parameters: RDD[DocumentParameterType], wordGivenModel: DocumentParameterType => (Int, Short) => Float) = {
        math.exp(-parameters.aggregate(0f)(
            (thatOne, otherOne) => thatOne + singleDocumentLikelihood(otherOne, topicsBC, wordGivenModel(otherOne)),
            (thatOne, otherOne) => thatOne + otherOne) / parameters.count
        )
    }

    protected def singleDocumentLikelihood(parameter: DocumentParameters, topicsBC: Broadcast[Array[Array[Float]]], wordGivenModel: ((Int, Short) => Float)) = {
        parameter.document.tokens.mapActivePairs {
            wordGivenModel
        }.sum
    }

    def probabilityOfWordGivenTopic(word: Int, parameter: DocumentParameters, topicsBC: Broadcast[Array[Array[Float]]]) = {
        var underLog = 0f
        for (topic <- 0 until numberOfTopics) underLog += parameter.theta(topic) * topicsBC.value(topic)(word)
        underLog
    }

    protected def getInitialTopics(alphabetSize: Int) = {
        val topics = Array.fill[Array[Float]](numberOfTopics)(Array.fill[Float](alphabetSize)(random.nextFloat))
        topics.map {
            topic => val sum = topic.sum; topic.map(_ / sum)
        }
    }

    protected def getTopics(parameters: RDD[DocumentParameterType],
                            topics: Broadcast[Array[Array[Float]]],
                            alphabetSize: Int,
                            globalParameters: GlobalParameterType) = {

        val newTopics = globalParameters.topicWords.map {
            array => val sum = array.sum; array.map(i => i / sum)
        }

        newTopics
    }
}
