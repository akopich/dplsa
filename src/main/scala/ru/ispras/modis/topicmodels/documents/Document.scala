package ru.ispras.modis.topicmodels.documents

import breeze.linalg.SparseVector


/**
 * Created with IntelliJ IDEA.
 * User: valerij
 * Date: 2/13/13
 * Time: 3:06 PM
 */

/**
 * @param tokens Non-zero components correspond to words included in the document.
 *               Non-zero value equals to the number of time the words is included in the document.
 * @param alphabetSize number of different words in the collections
 */
class Document(val tokens: SparseVector[Short], val alphabetSize: Int) extends Serializable