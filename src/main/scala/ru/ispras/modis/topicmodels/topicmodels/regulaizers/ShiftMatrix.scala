package ru.ispras.modis.topicmodels.topicmodels.regulaizers

/**
 * Created by valerij on 6/26/14.
 */
trait ShiftMatrix {
    def shift(matrix: Array[Array[Float]], op: (Array[Array[Float]], Int, Int) => Unit): Unit =
        for (i <- 0 until matrix.size; j <- 0 until matrix.head.size) op(matrix, i, j)

    def shift(matrix: Array[Float], op: (Array[Float], Int) => Unit): Unit =
        for (i <- 0 until matrix.size) op(matrix, i)
}
