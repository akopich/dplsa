package ru.ispras.modis.topicmodels.examples

import java.util.Random

import com.esotericsoftware.kryo.Kryo
import gnu.trove.map.hash.TObjectIntHashMap
import org.apache.spark.SparkContext
import org.apache.spark.serializer.KryoRegistrator
import ru.ispras.modis.topicmodels.documents.Numerator
import ru.ispras.modis.topicmodels.topicmodels.PLSA
import ru.ispras.modis.topicmodels.topicmodels.regulaizers.{SymmetricDirichletDocumentOverTopicDistributionRegularizer, SymmetricDirichletTopicRegularizer}
import ru.ispras.modis.topicmodels.utils.serialization.TObjectIntHashMapSerializer

/**
 * Created by valerij on 6/19/14.
 */

class Registrator extends KryoRegistrator {
    override def registerClasses(kryo: Kryo) {
        kryo.register(classOf[TObjectIntHashMap[Object]], new TObjectIntHashMapSerializer)
    }
}

object SimplePLSAJob {
    def main(args: Array[String]) {
        val sc = new SparkContext("local[4]", "Simple Job")
        System.setProperty("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        System.setProperty("spark.kryo.registrator", "ru.ispras.modis.infmeas.prototype.twitterrank.kryo.Registrator")
        System.setProperty("spark.closure.serializer", "org.apache.spark.serializer.KryoSerializer")


        val rawDocuments = sc.parallelize(Seq("a b a", "x y y z", "a b z x ").map(_.split(" ").toSeq))

        val numberOfTopics = 2
        val numberOfIterations = 10

        val plsa = new PLSA(sc,
            numberOfTopics,
            numberOfIterations,
            new Random(),
            new SymmetricDirichletDocumentOverTopicDistributionRegularizer(0.2f),
            new SymmetricDirichletTopicRegularizer(0.2f))

        val docs = Numerator.numerate(rawDocuments, 0)

        val (theta, phi) = plsa.infer(docs)

        theta.collect().foreach(x => println(x.probabilities.toList))
    }
}
