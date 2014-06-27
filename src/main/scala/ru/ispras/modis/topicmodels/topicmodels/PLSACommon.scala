package ru.ispras.modis.topicmodels.topicmodels

import java.util.Random

import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import ru.ispras.modis.topicmodels.documents.Document
import ru.ispras.modis.topicmodels.topicmodels.regulaizers.{MatrixInPlaceModification, TopicsRegularizer}

/**
 * Created by valerij on 6/25/14.
 */
trait PLSACommon[DocumentParameterType <: DocumentParameters, GlobalParameterType <: GlobalParameters] extends MatrixInPlaceModification {
    protected val numberOfTopics: Int
    protected val random: Random
    protected val topicRegularizer: TopicsRegularizer
    protected val sc: SparkContext

    protected def generalizedPerplexity(topicsBC: Broadcast[Array[Array[Float]]], parameters: RDD[DocumentParameterType], collectionLength: Int, wordGivenModel: DocumentParameterType => (Int, Short) => Float) = {
        math.exp(-(parameters.aggregate(0f)(
            (thatOne, otherOne) => thatOne + singleDocumentLikelihood(otherOne, topicsBC, wordGivenModel(otherOne)),
            (thatOne, otherOne) => thatOne + otherOne) + topicRegularizer(topicsBC.value)) / collectionLength
        )
    }

    protected def getAlphabetSize(documents: RDD[Document]) = documents.first().alphabetSize

    protected def getCollectionLength(documents: RDD[Document]) = documents.map(_.tokens.activeSize).reduce(_ + _)

    protected def singleDocumentLikelihood(parameter: DocumentParameters, topicsBC: Broadcast[Array[Array[Float]]], wordGivenModel: ((Int, Short) => Float)) = {
        parameter.document.tokens.mapActivePairs(wordGivenModel).sum + parameter.priorThetaLogProbability
    }

    protected def probabilityOfWordGivenTopic(word: Int, parameter: DocumentParameters, topicsBC: Broadcast[Array[Array[Float]]]) = {
        var underLog = 0f
        for (topic <- 0 until numberOfTopics) underLog += parameter.theta(topic) * topicsBC.value(topic)(word)
        underLog
    }

    protected def getInitialTopics(alphabetSize: Int) = {
        val topics = Array.fill[Array[Float]](numberOfTopics)(Array.fill[Float](alphabetSize)(random.nextFloat))
        normalize(topics)
        sc.broadcast(topics)
    }

    protected def getTopics(parameters: RDD[DocumentParameterType],
                            alphabetSize: Int,
                            oldTopics: Array[Array[Float]],
                            globalParameters: GlobalParameterType) = {

        val newTopicCnt: Array[Array[Float]] = globalParameters.topicWords

        topicRegularizer.regularize(newTopicCnt, oldTopics)
        normalize(newTopicCnt)

        newTopicCnt
    }

    private def normalize(matrix: Array[Array[Float]]) = {
        matrix.foreach(array => {
            val sum = array.sum
            shift(array, (arr, i) => arr(i) /= sum)
        })
    }
}
