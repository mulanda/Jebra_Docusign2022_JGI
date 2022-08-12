package providers.arcgis

import _root_.auth.BasicAuth
import api.util.ApiDateFormatting
import core.date.DatePlus
import core.file.FilePlusCSV
import core.jsonic.JsObjectPlus.JsonWithObject
import core.mime.{MimeHeader, MimeType}
import core.model.{Reply, ThrowableReply}
import core.string.{StringEncoding, StringHashing, StringPlusObjectId}
import env.ApiContext
import fetchers.OAuthFetcher
import play.api.libs.json.*
import providers.*
import providers.auth.CredentialStore
import providers.oauth.OAuth.GrantType
import providers.oauth.OAuth.RequestType.AccessToken
import providers.oauth.{OAuth, OAuthCredential, OAuthManager, OAuthManagerFactory}
import schema.connections.SetCredentials
import user.User

import java.net.URL
import scala.annotation.nowarn
import scala.concurrent.Future
import scala.concurrent.duration.*

/**
 * Created by Daudi Chilongo on 08/02/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 * @todo: Audit
 */
class ArcGisFetcher(val authManager: OAuthManager)
                    (implicit val ec: ApiContext)
  extends OAuthFetcher
    with BasicAuth
    with DatePlus
    with ApiDateFormatting
    with FilePlusCSV
    with StringEncoding
    with StringHashing
    with StringPlusObjectId {

  override def logId: String = authManager.conn match {
    case Some(conn) => conn.fsName
    case _ => authManager.user.email
  }

  override lazy val baseUrl: URL = api.baseUrl

  /** @todo: Audit */
  private lazy val tokenUrl: String = api.oAuth.get.tokenUrl

  private lazy val RegionIdHeader: String = "X-ZINGFIT-REGION-ID"
  //protected def apiConfig: ArcGisConfig = ArcGisConfig(api)

  @nowarn("msg=never used")
  private def headers(accessToken: String,
                      regionId: Option[String]): Seq[(String, String)] = {
    super.headersWithAccessToken(accessToken)
      .appendOpt(regionId.map(id => (RegionIdHeader, id)))
  }

  override def conn: Option[ArcGisConnection] = {
    super.conn.map(_.asInstanceOf[ArcGisConnection])
  }

  override def withAccessToken[A](f: String => Future[A]): Future[A] = {
    authManager.credential.flatMap {
      case Some(credential) =>
        credential.accessToken match {
          case Some(_) if credential.isExpired =>
            refreshAccessToken().flatMap { accessToken =>
              f(accessToken)
            }
          case Some(accessToken) => f(accessToken).recoverWith {
            case t: ThrowableReply if t.reply.code.isUnauthorizedError =>
              ec.after(5.seconds) {  /* Wait 5 seconds then refresh credentials and retry */
                refreshAccessToken().flatMap { accessToken =>
                  f(accessToken)
                }
              }
            case t: Throwable => Future.failed(t)
          }
          case _ => Future.failed(Reply.Unauthorized("Missing accessToken"))
        }
      case _ => Future.failed(Reply.Unauthorized(s"Missing credential"))
    }
  }

  /**
   * @see https://developers.arcgis.com/rest/users-groups-and-items/authentication.htm
   * For now we only support App Login
   * NOTE: Adapted from [[ providers.zingfit.ZingfitFetcher.fetchAccessToken ]]
   */
  def fetchAccessToken(creds: SetCredentials,
                       connectionId: Option[String],
                       by: Option[User] = None):
  Future[(ArcGisConnection, OAuthCredential)] = {
    port("fetchAccessToken", tokenUrl)
    val headers = Seq(
      MimeHeader.ContentType -> MimeType.FORM.value,
    )

    val params = Map(
      "client_id" -> creds.clientId,
      "client_secret" -> creds.clientSecret,
      "grant_type" -> GrantType.ClientCredentials.toString,
    )

    warn(tokenUrl.ul)

    makePost(tokenUrl, headers).body(params) /** @todo: Verify form Content-type */
      .send(backend)
      .flatMap { res =>
        OAuth.parseResponse(authManager.provider, AccessToken, res)  match {
          case Left(e) =>
            /** @see CredentialManager.processOAuthResponse */
            Future.failed(e.toReplyWith(Reply.Forbidden))
          case Right(body) =>
            val targetId = connectionId.getOrElse(newObjectId())
            val cred = new OAuthCredential(body :++ Json.obj(
              "connectionId" -> targetId,
              "userId" -> userId,
              "provider" -> authManager.provider,
              "username" -> creds.clientId,
              "secret" -> creds.clientSecret,
              "isOAuth" -> true,
            ))
            authManager
              .connectWith(cred, by.getOrElse(user), Some(creds), None)
              .flatMap { case (conn, cred) =>
                new CredentialStore().save(cred).map { _ =>
                  (conn.asInstanceOf[ArcGisConnection], cred)
                }
              }
        }
      }
  }

  private def refreshAccessToken(): Future[String] = {
    authManager.credentialToRefresh.flatMap {
      case Some(cred) =>
        (cred.username, cred.secret) match {
          case (Some(username), Some(secret)) =>
            fetchAccessToken(
              SetCredentials(api, username, secret),
              Some(cred.connectionId)
            ).map(_._2.accessToken.get)
          case _ => Future.failed(Reply.Forbidden("Missing clientId and/or secret"))
        }
      case _ => Future.failed(Reply.InvalidData("Missing credentials"))
    }
  }

}

object ArcGisFetcher extends OAuthManagerFactory {

  def apply(user: User,
            conn: ArcGisConnection)
           (implicit ec: ApiContext): ArcGisFetcher = apply(user, conn.apiName, Some(conn))

  def apply(user: User, apiName: ApiName)
           (implicit ec: ApiContext): ArcGisFetcher = apply(user, apiName, None)

  private def apply(user: User,
            apiName: ApiName,
            conn: Option[ArcGisConnection])
           (implicit ec: ApiContext): ArcGisFetcher = {
    new ArcGisFetcher(Provider.ArcGis.apiWithName(apiName).oAuthManager(user, conn))
  }

}
