package managehelpcontentpublisher

object Eithers {

  def seqToEither[E, A](eithers: Seq[Either[E, A]]): Either[E, Seq[A]] =
    eithers
      .collectFirst { case Left(e) => Left(e) }
      .getOrElse { Right(eithers.collect { case Right(a) => a }) }

  def optionToEither[E, A](optEither: Option[Either[E, A]]): Either[E, Option[A]] =
    optEither match {
      case None         => Right(None)
      case Some(either) => either.map(Some(_))
    }
}
