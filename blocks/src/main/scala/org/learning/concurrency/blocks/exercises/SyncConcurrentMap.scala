package org.learning.concurrency.blocks.exercises

import scala.collection.{concurrent, mutable}

class SyncConcurrentMap[K, V] extends concurrent.Map[K, V] {

  private val _map = mutable.Map.empty[K, V]

  def get(key: K): Option[V] = _map.synchronized {
    _map get key
  }

  def addOne(elem: (K, V)): SyncConcurrentMap.this.type = _map synchronized {
    _map addOne elem
    this
  }

  def subtractOne(key: K): SyncConcurrentMap.this.type = _map synchronized {
    _map subtractOne key
    this
  }

  def iterator: Iterator[(K, V)] = _map.iterator

  def remove(k: K, v: V): Boolean = _map synchronized {
    _map.get(k) match {
      case Some(value) if (v == null && value == null) || v == value =>
        _map remove k
        true
      case _ =>
        false
    }
  }

  def replace(k: K, oldValue: V, newValue: V): Boolean = _map.synchronized {
    _map.get(k) match {
      case Some(value) if (value == null && oldValue == null) || value == oldValue =>
        _map put(k, newValue)
        true
      case _ =>
        false
    }
  }

  def replace(k: K, v: V): Option[V] = _map.synchronized {
    _map.get(k) match {
      case x@Some(_) =>
        _map put(k, v)
        x
      case None =>
        None
    }
  }

  def putIfAbsent(k: K, v: V): Option[V] = _map.synchronized {
    _map.get(k) match {
      case x@Some(_) =>
        x
      case None =>
        _map put(k, v)
    }
  }

}
