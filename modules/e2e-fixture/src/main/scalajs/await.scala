import concurrent.Future

def await[T](f: Future[T]) =
  f
