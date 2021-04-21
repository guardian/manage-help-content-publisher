package managehelpcontentpublisher

object Eithers {

  // extension method because has same semantics as cats.sequence: https://typelevel.org/cats/typeclasses/traverse.html
  implicit class SeqToEither[E, A](eithers: Seq[Either[E, A]]) {
    val sequence: Either[E, Seq[A]] = eithers.partitionMap(identity) match {
      case (firstLeft :: _, _) => Left(firstLeft)
      case (_, as)             => Right(as)
    }
  }

  // extension method because has same semantics as cats.sequence: https://typelevel.org/cats/typeclasses/traverse.html
  implicit class OptionToEither[E, A](optEither: Option[Either[E, A]]) {
    val sequence: Either[E, Option[A]] = optEither match {
      case None         => Right(None)
      case Some(either) => either.map(Some(_))
    }
  }
}
