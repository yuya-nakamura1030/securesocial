package securesocial.core.providers

import play.api.libs.json.JsObject
import securesocial.core._
import securesocial.core.services.{ CacheService, RoutesService }

import scala.concurrent.Future
import scala.util.{ Success, Failure }

/**
 * A SoundcloudProvider OAuth2 Provider
 */
class SoundcloudProvider(routesService: RoutesService,
  cacheService: CacheService,
  client: OAuth2Client)
    extends OAuth2Provider(routesService, client, cacheService) {
  val UserInfoApi = "https://api.soundcloud.com/me.json?oauth_token="
  val Error = "error"
  val Message = "message"
  val Code = "code"
  val Id = "id"
  val Username = "username"
  val FullName = "full_name"
  val AvatarUrl = "avatar_url"
  val Account = "account"

  override val id = SoundcloudProvider.Soundcloud

  def fillProfile(info: OAuth2Info): Future[BasicProfile] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val accessToken = info.accessToken
    client.retrieveProfile(UserInfoApi + accessToken).map { me =>
      (me \ Error).asOpt[JsObject] match {
        case Some(error) =>
          val message = (error \ Message).as[String]
          val errorCode = (error \ Code).as[String]
          logger.error(s"[securesocial] error retrieving profile information from Soundcloud. Error type = $errorCode, message = $message")
          throw new AuthenticationException()
        case _ =>
          val userId = (me \ Id).as[Int]
          val username = (me \ Username).asOpt[String]
          val fullName = (me \ FullName).asOpt[String]
          val avatarUrl = (me \ AvatarUrl).asOpt[String]
          BasicProfile(id, userId.toString, None, None, fullName.filterNot { p => p.isEmpty }.orElse(username), None, avatarUrl, authMethod, oAuth2Info = Some(info))
      }
    } recover {
      case e: AuthenticationException => throw e
      case e =>
        logger.error("[securesocial] error retrieving profile information from Soundcloud", e)
        throw new AuthenticationException()
    }
  }
}

object SoundcloudProvider {
  val Soundcloud = "soundcloud"
}
