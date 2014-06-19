package ru.ispras.modis.topicmodels.topicmodels

/**
 * Created with IntelliJ IDEA.
 * User: valerij
 * Date: 2/13/13
 * Time: 5:55 PM
 */
/**
 * class contains the distribution of a document over topics -- theta
 * @param probabilities array of topic weight
 */
class TopicDistribution(val probabilities: Array[Double]) extends Serializable {
  /**
   *
   * @param topic number of topic
   * @return weight of given topic
   */
    def apply(topic: Int) = probabilities(topic)

    override def toString: String = {
        probabilities.toSeq.toString()
    }
}