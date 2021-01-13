package org.learning.concurrency.actors.exercises

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import org.learning.concurrency.actors.log

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

object DistributedMapApp extends App {

  val system = ActorSystem("DistributedMapSystem")

  val shards = (0 until 4).map(i => system.actorOf(ShardActor.props[String], name = s"shardActor-$i"))

  val distributedMap = new DistributedMap[Int, String](shards: _*)

  val values = List(
    0 -> "A",
    1 -> "B",
    2 -> "C",
    3 -> "D",
    4 -> "E",
    5 -> "F",
    6 -> "G"
  )

  values.foreach {
    case (key, value) =>
      distributedMap.update(key, value)
  }

  values.map(t => distributedMap get t._1).foreach { result =>
    result.foreach(r => log(msg = s"r = $r"))
  }

  Thread sleep 3000

  system.terminate()
  Thread sleep 3000

}

class DistributedMap[K, V](shards: ActorRef*)(implicit ct: ClassTag[V]) {

  private val _shardsArray = shards.toArray

  private val _numberOfShards = (Math.log10(_shardsArray.length) / Math.log10(2)).toInt

  private implicit val _timeout: Timeout = 5.seconds

  def get(key: K): Future[Option[V]] = {
    val distributedKey = DistributedKey(key.hashCode(), _numberOfShards)
    val result = _shardsArray(distributedKey.shard) ? ShardActor.Get(distributedKey.key)
    result.mapTo[Option[V]]
  }

  def update(key: K, value: V): Future[Unit] = {
    val distributedKey = DistributedKey(key.hashCode(), _numberOfShards)
    val result = _shardsArray(distributedKey.shard) ? ShardActor.Update(distributedKey.key, value)
    result.mapTo[Unit]
  }

}

class ShardActor[V](implicit ct: ClassTag[V]) extends Actor {

  private val _log = Logging(context.system, this)

  private val _map = mutable.Map.empty[Int, V]

  override def receive: Receive = {
    case ShardActor.Get(key: Int) =>
      sender() ! _map.get(key)
    case ShardActor.Update(key: Int, value: V) =>
      _log info s"update $key -> $value, actor = ${this.self.path}"
      sender() ! _map.update(key, value)
  }

}

object ShardActor {

  def props[V](implicit ct: ClassTag[V]): Props =
    Props(new ShardActor[V])

  case class Get(key: Int)

  case class Update[V](key: Int, value: V)(implicit ct: ClassTag[V])

}

case class DistributedKey(shard: Int, key: Int)

object DistributedKey {

  def apply(keyHashCode: Int, numOfShards: Int): DistributedKey = {
    val keyHashCodeBits = keyHashCode.toBinaryString

    val shardBits = keyHashCodeBits.takeRight(numOfShards)
    val shard = Integer.parseInt(shardBits, 2)

    val keyBits = keyHashCodeBits.take(keyHashCodeBits.length - numOfShards)
    val key = if (keyBits.nonEmpty) Integer.parseInt(keyBits, 2) else 0

    new DistributedKey(shard, key)
  }

}
