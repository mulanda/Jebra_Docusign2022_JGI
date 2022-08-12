package providers.docusign

import env.ApiContext
import play.api.libs.json.JsObject
import providers.oauth.{OAuthConnectorWithAuthCode, OAuthCredential, OAuthManager}

import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/08/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class DocusignConnector(val cred: OAuthCredential,
                        val authResponse: Option[JsObject])
                       (implicit val ec: ApiContext, manager: OAuthManager)
  extends OAuthConnectorWithAuthCode[DocusignConnection] {

  override def connect(): Future[DocusignConnection] = {
    mock("connectDocusign", provider)
    ifNotReady(conn) { _ =>
      conn match {
        case Some(conn) => Future.successful(conn)
        case _ => getName.flatMap { name =>
            /** @todo: Audit accountId */
            requiredAuthSessionParam("accountId").map { accountId =>
              new DocusignConnection(
                userId = user.id,
                name = name,
                accountId = accountId
              )
            }
          }
      }
    }
  }

}
