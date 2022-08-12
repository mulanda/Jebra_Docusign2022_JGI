package providers.arcgis

import env.ApiContext
import providers.oauth.{OAuthConnector, OAuthCredential, OAuthManager}
import schema.connections.SetCredentials

import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/02/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class ArcGisConnector(val cred: OAuthCredential,
                      params: Option[SetCredentials])
                      (implicit val ec: ApiContext, manager: OAuthManager)
  extends OAuthConnector[ArcGisConnection] {

  override def connect(): Future[ArcGisConnection] = {
    mock("connectArcGis", provider)
    ifNotReady(conn) { _ =>
      val nextConn = conn.getOrElse {
        new ArcGisConnection(
          userId = user.id,
          name = params.flatMap(_.name).get, /** New connection name */
          apiName = manager.api.name
        )
      }
      Future.successful(nextConn)
    }
  }

}
