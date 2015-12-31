package coursier

sealed trait CachePolicy extends Product with Serializable

object CachePolicy {
  case object LocalOnly extends CachePolicy
  case object UpdateChanging extends CachePolicy
  case object Update extends CachePolicy
  case object FetchMissing extends CachePolicy
  case object ForceDownload extends CachePolicy
}