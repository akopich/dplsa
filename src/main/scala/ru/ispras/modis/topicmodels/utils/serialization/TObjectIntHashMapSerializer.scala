package ru.ispras.modis.topicmodels.utils.serialization

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}
import gnu.trove.map.hash.TObjectIntHashMap

/**
 * Created with IntelliJ IDEA.
 * User: valerij
 * Date: 09.09.13
 * Time: 20:13
 * To change this template use File | Settings | File Templates.
 */
class TObjectIntHashMapSerializer extends Serializer[TObjectIntHashMap[Object]] {


    def write(kryo: Kryo, out: Output, map: TObjectIntHashMap[Object]) {
        val values = map.values
        val keys = map.keys()
        kryo.writeObject(out, values)
        kryo.writeObject(out, keys)
    }

    def read(kryo: Kryo, in: Input, clazz: Class[TObjectIntHashMap[Object]]): TObjectIntHashMap[Object] = {
        val values = kryo.readObject(in, classOf[Array[Int]])
        val keys = kryo.readObject(in, classOf[Array[Object]])

        val map = new TObjectIntHashMap[Object]()

        keys.zip(values).map {
            case (key, value) => map.put(key, value)
        }
        map
    }

}
