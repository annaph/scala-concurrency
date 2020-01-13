package org.learning.concurrency.blocks

object LazyValsCreate extends App {

  lazy val obj = new AnyRef
  lazy val nonDeterministic = s"made by ${Thread.currentThread.getName}"

  execute {
    log(s"Execution context thread sees object = $obj")
    log(s"Execution context thread sees nonDeterministic = $nonDeterministic")
  }

  log(s"Main thread sees object = $obj")
  log(s"Main thread see nondeterministic = $nonDeterministic")

  Thread sleep 3000

}

object LazyValsObject extends App {

  object Lazy {
    log("Running lazy constructor.")
  }

  log("Main thread is about to reference Lazy.")
  Lazy

  log("Main thread completed.")

}

object LazyValsUnderTheHood extends App {

  @volatile private var _bitmap = false

  private var _obj: AnyRef = _

  def obj: AnyRef = if (_bitmap) _obj else this.synchronized {
    if (!_bitmap) {
      _obj = new AnyRef
      _bitmap = true
    }
    _obj
  }

  execute(log(s"$obj"))
  log(s"$obj")

  Thread sleep 3000

}

object LazyValsDeadlock extends App {

  object A {
    lazy val x: Int = B.y
  }

  object B {
    lazy val y: Int = A.x
  }

  execute(B.y)
  A.x

  Thread sleep 3000

}

object LazyValsAndBlocking extends App {

  lazy val x: Int = {
    val t = thread {
      log(s"Initializing $x.")
    }

    t.join()

    1
  }

  x

}

object LazyValsAndMonitors extends App {

  lazy val x = 1

  this.synchronized {
    val t = thread(x)
    t.join()
  }

}
