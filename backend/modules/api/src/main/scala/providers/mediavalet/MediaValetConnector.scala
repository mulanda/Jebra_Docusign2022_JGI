package providers.mediavalet

import env.ApiContext
import play.api.libs.json.JsObject
import providers.oauth.{OAuthConnectorWithAuthCode, OAuthCredential, OAuthManager}

import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/08/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class MediaValetConnector(val cred: OAuthCredential,
                          val authResponse: Option[JsObject])
                         (implicit val ec: ApiContext, manager: OAuthManager)
  extends OAuthConnectorWithAuthCode[MediaValetConnection] {

  override def connect(): Future[MediaValetConnection] = {
    mock("connectMediaValet", provider)
    ifNotReady(conn) { _ =>
      conn match {
        case Some(conn) => Future.successful(conn)
        case _ => getName.map { name =>
          new MediaValetConnection(
            userId = user.id,
            name = name,
          )
        }
      }
    }
  }

}
