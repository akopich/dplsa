package ru.ispras.modis.topicmodels.topicmodels.regulaizers

import cern.jet.stat.Gamma

/**
 * Created by valerij on 6/26/14.
 */
trait SymmetricDirichletHelper {
    protected val alpha: Float

    private def logBeta(x: Array[Float]) = {
        val n = x.size
        n * Gamma.logGamma(alpha) - Gamma.logGamma(n * alpha)
    }

    /**
     * We need this just because in case of alpha < 1 dirichlet log likelihood is infinite for x st x_i = 0
     */
    private val SMALL_VALUE: Double = 0.00001

    protected def dirichletLogLikelihood(x: Array[Float]) = (-logBeta(x) + (alpha - 1) * x.map(xx => math.log(xx + SMALL_VALUE)).sum).toFloat
}
