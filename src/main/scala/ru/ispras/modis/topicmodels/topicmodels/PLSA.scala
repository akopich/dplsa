package ru.ispras.modis.topicmodels.topicmodels


import java.util.Random

import com.typesafe.scalalogging.Logging
import com.typesafe.scalalogging.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.slf4j.LoggerFactory
import ru.ispras.modis.topicmodels.documents.Document

import scala.math.log


/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 27.08.13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
/**
 * distributed topic modeling
 * @param sc  spark context
 * @param numberOfTopics number of topics
 * @param numberOfIterations number of iterations
 * @param random java.util.Random need for initialisation
 * @param gamma weight of background
 * @param eps   weight of noise
 * @param computePpx boolean. If true, model computes perplexity and prints it puts in the log at INFO level. it takes some time and memory
 */
class PLSA(@transient private val sc: SparkContext,
           private val numberOfTopics: Int,
           private val numberOfIterations: Int,
           private val random: Random,
           private val gamma: Float = 0.0f, // background
           private val eps: Float = 0.00f,
           private val computePpx: Boolean = true) extends TopicModel with Logging with Serializable {

    @transient protected val logger = Logger(LoggerFactory getLogger "PLSA")

    def infer(documents: RDD[Document]): (RDD[TopicDistribution], Broadcast[Array[Array[Float]]]) = {
        val alphabetSize = documents.first().alphabetSize

        val topicBC = sc.broadcast(getInitialTopics(alphabetSize))
        val background = (0 until alphabetSize).map(j => 1f / alphabetSize).toArray

        val parameters = documents.map(doc => DocumentParameters(doc, numberOfTopics, gamma, eps, alphabetSize))

        val (result, topics) = newIteration(parameters, topicBC, background, alphabetSize, 0)

        (result.map(p => new TopicDistribution(p.theta.map(_.toDouble).toArray)), topics)
    }


    private def newIteration(parameters: RDD[DocumentParameters],
                             topicsBC: Broadcast[Array[Array[Float]]],
                             background: Array[Float],
                             alphabetSize: Int,
                             numberOfIteration: Int): (RDD[DocumentParameters], Broadcast[Array[Array[Float]]]) = {

        if (computePpx) {
            logger.info("Interation number " + numberOfIteration)
            logger.info("Perplexity=" + perplexity(topicsBC, parameters, background))
        }
        if (numberOfIteration == numberOfIterations) {
            (parameters, topicsBC)
        }
        else {
            val newParameters = parameters.map(u => u.getNewTheta(topicsBC, background, eps, gamma)).cache()
            val (newTopics, newBackground) = getTopics(newParameters, topicsBC, background, alphabetSize)

            parameters.unpersist()

            newIteration(newParameters, sc.broadcast(newTopics), newBackground, alphabetSize, numberOfIteration + 1)
        }
    }

    private def getTopics(parameters: RDD[DocumentParameters],
                          topics: Broadcast[Array[Array[Float]]],
                          background: Array[Float],
                          alphabetSize: Int) = {
        def getGlobal(parameters: RDD[DocumentParameters]) = {
            parameters.aggregate[GlobalParameters](GlobalParameters(numberOfTopics, alphabetSize))(
                (thatOne, otherOne) => thatOne.add(otherOne, topics, background, eps, gamma, alphabetSize),
                (thatOne, otherOne) => thatOne + otherOne)
        }

        val globalParameters = getGlobal(parameters)

        val newTopics = globalParameters.topicWords.map {
            array => val sum = array.sum; array.map(i => i / sum)
        }

        val sum = globalParameters.backgroundWords.sum
        val newBackground = if (sum > 0 && gamma != 0) globalParameters.backgroundWords.map(i => i / sum) else globalParameters.backgroundWords.map(i => 0f)
        (newTopics, newBackground)
    }

    private def singleDocumentLikelihood(parameter: DocumentParameters, topicsBC: Broadcast[Array[Array[Float]]], background: Array[Float]) = {
        parameter.document.tokens.mapActivePairs {
            case (word, num) => {
                var underLog = 0f

                for (topic <- 0 until numberOfTopics) underLog += parameter.theta(topic) * topicsBC.value(topic)(word)

                underLog += background(word) * gamma + eps * parameter.noise(word)
                underLog /= (1 + eps + gamma)

                num * log(underLog)
            }
        }.sum.toFloat
    }

    private def perplexity(topicsBC: Broadcast[Array[Array[Float]]], parameters: RDD[DocumentParameters], background: Array[Float]) = {
        math.exp(-parameters.aggregate(0f)((thatOne, otherOne) => thatOne + singleDocumentLikelihood(otherOne, topicsBC, background),
            (thatOne, otherOne) => thatOne + otherOne) / parameters.count)
    }


    private def getInitialTopics(alphabetSize: Int) = {
        val topics = Array.fill[Array[Float]](numberOfTopics)(Array.fill[Float](alphabetSize)(random.nextFloat))
        topics.map {
            topic => val sum = topic.sum; topic.map(_ / sum)
        }
    }
}

