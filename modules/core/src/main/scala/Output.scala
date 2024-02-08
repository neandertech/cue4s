package com.indoorvivants.proompts

trait Output:
  def out[A: AsString](a: A): Unit
  def logLn[A: AsString](a: A): Unit

trait AsString[A]:
  extension (a: A) def render: String

given AsString[String] with
  extension (a: String) def render = a

extension (o: Output)
  def outLn[A: AsString](a: A) =
    o.out(a.render + "\n")
  def loggerOnly: Output =
    new Output:
      override def logLn[A: AsString](a: A): Unit = o.logLn(a)
      override def out[A: AsString](a: A): Unit   = ()

object Output:
  object Std extends Output:
    override def logLn[A: AsString](a: A): Unit = System.err.println(a.render)
    override def out[A: AsString](a: A): Unit   = System.out.print(a.render)

  object StdOut extends Output:
    override def logLn[A: AsString](a: A): Unit = System.out.println(a.render)
    override def out[A: AsString](a: A): Unit   = System.out.print(a.render)

  object DarkVoid extends Output:
    override def logLn[A: AsString](a: A): Unit = ()
    override def out[A: AsString](a: A): Unit   = ()


  class Delegate(writeOut: String => Unit, writeLog: String => Unit)
      extends Output:
    override def logLn[A: AsString](a: A): Unit = writeLog(a.render + "\n")
    override def out[A: AsString](a: A): Unit   = writeOut(a.render)
end Output
