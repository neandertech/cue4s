import concurrent.*, duration.*

def await[T](f: Future[T]): Unit =
  concurrent.Await.ready(f, 5.seconds)
