package ru.ispras.modis.topicmodels.topicmodels


import java.util.Random

import com.typesafe.scalalogging.Logging
import com.typesafe.scalalogging.slf4j.Logger
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.slf4j.LoggerFactory
import ru.ispras.modis.topicmodels.documents.Document


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
class RobustPLSA(@transient private val sc: SparkContext,
                 protected val numberOfTopics: Int,
                 protected val numberOfIterations: Int,
                 protected val random: Random,
                 private val gamma: Float = 0.0f, // background
                 private val eps: Float = 0.00f,
                 private val computePpx: Boolean = true) extends TopicModel with PLSACommon[RobustDocumentParameters, RobustGlobalParameters] with Logging with Serializable {

    @transient protected val logger = Logger(LoggerFactory getLogger "Robust PLSA")

    def infer(documents: RDD[Document]): (RDD[TopicDistribution], Broadcast[Array[Array[Float]]]) = {
        val alphabetSize = documents.first().alphabetSize

        val topicBC = sc.broadcast(getInitialTopics(alphabetSize))
        val parameters = documents.map(doc => RobustDocumentParameters(doc, numberOfTopics, gamma, eps))

        val background = (0 until alphabetSize).map(j => 1f / alphabetSize).toArray

        val (result, topics) = newIteration(parameters, topicBC, background, alphabetSize, 0)

        (result.map(p => new TopicDistribution(p.theta.map(_.toDouble).toArray)), topics)
    }


    private def newIteration(parameters: RDD[RobustDocumentParameters],
                             topicsBC: Broadcast[Array[Array[Float]]],
                             background: Array[Float],
                             alphabetSize: Int,
                             numberOfIteration: Int): (RDD[RobustDocumentParameters], Broadcast[Array[Array[Float]]]) = {

        if (computePpx) {
            logger.info("Interation number " + numberOfIteration)
            logger.info("Perplexity=" + perplexity(topicsBC, parameters, background))
        }
        if (numberOfIteration == numberOfIterations) {
            (parameters, topicsBC)
        }
        else {
            val newParameters = parameters.map(u => u.getNewTheta(topicsBC, background, eps, gamma)).cache()

            val globalParameters = getGlobalParameters(parameters, topicsBC, background, alphabetSize)

            val newTopics = getTopics(newParameters, topicsBC, alphabetSize, globalParameters)
            val newBackground = getNewBackgound(globalParameters)

            parameters.unpersist()

            newIteration(newParameters, sc.broadcast(newTopics), newBackground, alphabetSize, numberOfIteration + 1)
        }
    }

    private def getGlobalParameters(parameters: RDD[RobustDocumentParameters], topics: Broadcast[Array[Array[Float]]], background: Array[Float], alphabetSize: Int) = {
        parameters.aggregate[RobustGlobalParameters](RobustGlobalParameters(numberOfTopics, alphabetSize))(
            (thatOne, otherOne) => thatOne.add(otherOne, topics, background, eps, gamma, alphabetSize),
            (thatOne, otherOne) => thatOne + otherOne)
    }

    private def getNewBackgound(globalParameters: RobustGlobalParameters) = {
        val sum = globalParameters.backgroundWords.sum
        if (sum > 0 && gamma != 0) globalParameters.backgroundWords.map(i => i / sum) else globalParameters.backgroundWords.map(i => 0f)
    }


    private def perplexity(topicsBC: Broadcast[Array[Array[Float]]], parameters: RDD[RobustDocumentParameters], background: Array[Float]) =
        generalizedPerplexity(topicsBC, parameters,
            par => (word, num) => num * math.log(probabilityOfWordGivenTopic(word, par, topicsBC) + gamma * background(word) + eps * par.noise(word) / (1 + eps + gamma)).toFloat)

}

