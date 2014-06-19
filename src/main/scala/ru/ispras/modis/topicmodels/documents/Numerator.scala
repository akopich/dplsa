package ru.ispras.modis.topicmodels.documents

import breeze.linalg.SparseVector
import gnu.trove.map.hash.TObjectIntHashMap
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.rdd.RDD


/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 15.11.13
 * Time: 20:41
 * To change this template use File | Settings | File Templates.
 */

/**
 * This object numerates tokens. E.g. it replaces a word with its order number. It also calculates the number of unique words
 */
object Numerator {

    /**
     *
     * @param rawDocuments just RDD of tokenized documents
     * @param rareTokenThreshold words that are encountered in the collection less that rareTokenThreshold times are ommited
     * @return
     */
    def numerate(rawDocuments: RDD[Seq[String]], rareTokenThreshold: Int) = {
        val alphabet = rawDocuments.context.broadcast(getAlphabet(rawDocuments, rareTokenThreshold))

        rawDocuments.map(document => mkDocument(document, alphabet.value))
    }


    private def mkDocument(rawDocument: Seq[String], alphabet: TObjectIntHashMap[String]) = {
        val wordsMap = rawDocument.map(alphabet.get).foldLeft(Map[Int,Int]().withDefaultValue(0))((map, word) => map + (word -> (1 + map(word)) ))

        val words = wordsMap.keys.toArray.sorted

        val tokens = new SparseVector[Short](words, words.map(word => wordsMap(word).toShort), alphabet.size())
        new Document(tokens, alphabet.size())
    }

    private def getAlphabet(rawDocuments: RDD[Seq[String]], rareTokenThreshold: Int) = {
        val alphabet = new TObjectIntHashMap[String]()

        rawDocuments.flatMap(x => x).map(x => (x,1)).reduceByKey(_ + _).filter(_._2 > rareTokenThreshold).collect.map(_._1).zipWithIndex.foreach {
            case (key, value) => alphabet.put(key, value)
        }
        alphabet
    }
}
