package org.learing.concurrency.parallel.collections

import scala.collection.immutable.WrappedString
import scala.collection.mutable.ArrayBuffer
import scala.collection.parallel.immutable.ParSeq
import scala.collection.parallel.{Combiner, SeqSplitter, immutable}

class ParString(val str: String) extends immutable.ParSeq[Char] {

  override def apply(i: Int): Char =
    str charAt i

  override def length: Int =
    str.length

  override def splitter: SeqSplitter[Char] =
    new ParStringSplitter(str, 0, str.length)

  override def newCombiner: Combiner[Char, ParSeq[Char]] =
    new ParStringCombiner

  override def seq: Seq[Char] =
    new WrappedString(str)

}

class ParStringSplitter(val str: String, var curr: Int, val end: Int) extends SeqSplitter[Char] {

  override def dup: SeqSplitter[Char] =
    new ParStringSplitter(str, curr, end)

  override def remaining: Int =
    end - curr

  override def split: Seq[ParStringSplitter] = {
    val r = remaining
    val half: Int = r / 2

    if (r >= 2) psplit(half, r - half) else Seq(this)
  }

  override def psplit(sizes: Int*): Seq[ParStringSplitter] = {
    val splitters = for (size <- sizes) yield {
      val nEnd = (curr + size) min end
      val splitter = new ParStringSplitter(str, curr, nEnd)

      curr = nEnd
      splitter
    }

    if (curr == end) splitters else splitters :+ new ParStringSplitter(str, curr, end)
  }

  override def next(): Char = {
    val c = str charAt curr
    curr += 1

    c
  }

  override def hasNext: Boolean =
    curr < end

}

class ParStringCombiner extends Combiner[Char, ParString] {

  import scala.collection.mutable

  private val _chunks = ArrayBuffer.empty[mutable.StringBuilder] += new mutable.StringBuilder

  private var _lastChunk = _chunks.last

  private var _size = 0

  override def addOne(elem: Char): this.type = {
    _lastChunk += elem
    _size += 1

    this
  }

  override def result(): ParString = {
    val sb = new mutable.StringBuilder
    for (chunk <- _chunks) sb append chunk

    new ParString(sb.toString)
  }

  override def clear(): Unit = {
    _chunks.clear()

    _chunks += new mutable.StringBuilder
    _lastChunk = _chunks.last
    _size = 0
  }

  override def size: Int = _size

  override def combine[N <: Char, NewTo >: ParString](that: Combiner[N, NewTo]): Combiner[Char, NewTo] =
    if (this eq that) this else {
      that match {
        case that: ParStringCombiner =>
          _chunks ++= that._chunks
          _lastChunk = _chunks.last
          _size += that._size

          this
      }
    }

}

object CustomCharCount extends App {

  val txt = "A custom text " * 250000
  val parTxt = new ParString(txt)

  val seqTime = warmedTimed() {
    txt.foldLeft(0) {
      case (acc, c) =>
        if (Character.isUpperCase(c)) acc + 1 else acc
    }
  }

  val parTime = warmedTimed() {
    parTxt.aggregate(0)((acc, c) => if (Character.isUpperCase(c)) acc + 1 else acc, _ + _)
  }

  log(s"Sequential time - $seqTime ms")
  log(s"Parallel time - $parTime ms")

}

object CustomCharFilter extends App {

  val txt = "A custom text" * 250000
  val parTxt = new ParString(txt)

  val seqTime = warmedTimed(250) {
    txt.filter(_ != ' ')
  }

  val parTime = warmedTimed(250) {
    parTxt.filter(_ != ' ')
  }

  log(s"Sequential time - $seqTime")
  log(s"Parallel time - $parTime")

}
