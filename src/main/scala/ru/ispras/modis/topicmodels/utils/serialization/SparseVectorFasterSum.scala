package ru.ispras.modis.topicmodels.utils.serialization

import breeze.linalg.SparseVector

/**
 * Created by valerij on 6/25/14.
 */
trait SparseVectorFasterSum {
    protected def sumSparseVector(vector: SparseVector[Short]) = {
        var sum = 0
        for (element <- vector) sum += element
        sum
    }
}
