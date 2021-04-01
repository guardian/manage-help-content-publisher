package managehelpcontentpublisher

object Eithers {

  def seqToEither[E, A](eithers: Seq[Either[E, A]]): Either[E, Seq[A]] =
    eithers.partitionMap(identity) match {
      case (firstLeft :: _, _) => Left(firstLeft)
      case (_, as)             => Right(as)
    }

  def optionToEither[E, A](optEither: Option[Either[E, A]]): Either[E, Option[A]] =
    optEither match {
      case None         => Right(None)
      case Some(either) => either.map(Some(_))
    }
}
