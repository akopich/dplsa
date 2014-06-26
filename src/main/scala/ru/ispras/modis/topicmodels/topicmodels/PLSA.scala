package ru.ispras.modis.topicmodels.topicmodels

import java.util.Random

import com.typesafe.scalalogging.Logging
import com.typesafe.scalalogging.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.slf4j.LoggerFactory
import ru.ispras.modis.topicmodels.documents.Document
import ru.ispras.modis.topicmodels.topicmodels.regulaizers.{DocumentOverTopicDistributionRegularizer, TopicsRegularizer, UniformDocumentOverTopicRegularizer, UniformTopicRegularizer}


/**
 * Created by valerij on 6/25/14.
 */
class PLSA(@transient private val sc: SparkContext,
           protected val numberOfTopics: Int,
           private val numberOfIterations: Int,
           protected val random: Random,
           private val documentOverTopicRegularizer: DocumentOverTopicDistributionRegularizer = new UniformDocumentOverTopicRegularizer,
           @transient protected val topicRegularizer: TopicsRegularizer = new UniformTopicRegularizer,
           private val computePpx: Boolean = true) extends TopicModel with PLSACommon[DocumentParameters, GlobalParameters] with Logging with Serializable {

    @transient protected val logger = Logger(LoggerFactory getLogger "PLSA")

    def infer(documents: RDD[Document]): (RDD[TopicDistribution], Broadcast[Array[Array[Float]]]) = {
        val alphabetSize = documents.first().alphabetSize

        val collectionLength = documents.map(_.tokens.activeSize).reduce(_ + _)

        val topicBC = sc.broadcast(getInitialTopics(alphabetSize))

        val parameters = documents.map(doc => DocumentParameters(doc, numberOfTopics, documentOverTopicRegularizer))

        val (result, topics) = newIteration(parameters, topicBC, alphabetSize, collectionLength, 0)

        (result.map(p => new TopicDistribution(p.theta.map(_.toDouble).toArray)), topics)
    }


    private def newIteration(parameters: RDD[DocumentParameters],
                             topicsBC: Broadcast[Array[Array[Float]]],
                             alphabetSize: Int,
                             collectionLength: Int,
                             numberOfIteration: Int): (RDD[DocumentParameters], Broadcast[Array[Array[Float]]]) = {

        if (computePpx) {
            logger.info("Interation number " + numberOfIteration)
            logger.info("Perplexity=" + perplexity(topicsBC, parameters, collectionLength))
        }
        if (numberOfIteration == numberOfIterations) {
            (parameters, topicsBC)
        }
        else {
            val newParameters = parameters.map(u => u.getNewTheta(topicsBC)).cache()
            val globalParameters = getGlobalParameters(parameters, topicsBC, alphabetSize)
            val newTopics = getTopics(newParameters, alphabetSize, topicsBC.value, globalParameters)

            parameters.unpersist()

            newIteration(newParameters, sc.broadcast(newTopics), alphabetSize, collectionLength, numberOfIteration + 1)
        }
    }

    private def getGlobalParameters(parameters: RDD[DocumentParameters], topics: Broadcast[Array[Array[Float]]], alphabetSize: Int) = {
        parameters.aggregate[GlobalParameters](GlobalParameters(numberOfTopics, alphabetSize))(
            (thatOne, otherOne) => thatOne.add(otherOne, topics, alphabetSize),
            (thatOne, otherOne) => thatOne + otherOne)
    }

    private def perplexity(topicsBC: Broadcast[Array[Array[Float]]], parameters: RDD[DocumentParameters], collectionLength: Int) = {
        generalizedPerplexity(topicsBC, parameters, collectionLength, par => (word, num) => num * math.log(probabilityOfWordGivenTopic(word, par, topicsBC)).toFloat)
    }
}
