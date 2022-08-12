package providers.arcgis

import core.model.{Reply, ThrowableReply}
import env.DxTestEnv
import jobs.custom.jgi.Jgi
import providers.ApiName.ArcGisOnline
import providers.oauth.OAuth.CustomError
import providers.oauth.OAuthError
import schema.connections.SetCredentials

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/02/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class ArcGisAuthSpec extends DxTestEnv {

  override def useProductionDB: Boolean = false

  override def canUpdateKnown: Boolean = false

  override def canUpdateKnownCredentials: Boolean = false

  "authorize" taggedAs Do in {

    val ID = Jgi.Connections.arcGIS
    withSavedOrKnownConnection(ID, _ => ArcGisConnection(ID, ArcGisOnline)) { c0 =>
      val conn = c0.asInstanceOf[ArcGisConnection]
      conn.withUser { user =>

        val defaultParams = SetCredentials(conn, "clientId", "clientSecret")
        val overrideParams: Option[SetCredentials] = {
          /** @todo: Change when authorizing new connections */
          val creds = SetCredentials(conn, "change-clientId", "change-clientSecret")
          if (creds.clientId.startsWith("change")) {
            None
          } else {
            Some(creds)
          }
        }

        val client = ArcGisFetcher(user, conn)
        val params: SetCredentials = overrideParams.getOrElse {
          client.authManager.credential.await().map { cred =>
            SetCredentials(conn, cred.username.get, cred.secret.get)
          }.getOrElse(defaultParams)
        }

        if (params == defaultParams) {
          warn(s"Connect ${conn.logText} w/invalid credentials...")
          client.fetchAccessToken(params, Some(conn.id)).failsWith {
            case t: ThrowableReply =>
              warn(t.reply.prettyPrint)
              t.reply.code mustBe Reply.Forbidden
              val err = t.reply.data.get.as[OAuthError]
              warn(err.prettyPrint.blue)
              err.errorCode mustEqual CustomError("invalid_client_id")
              err.description mustBe Some("Invalid client_id")
              Future.successful(())
          }.await()
        } else {
          warn(s"Connect ${conn.logText} w/valid credentials...")
          client.fetchAccessToken(params, Some(conn.id)).flatMap { case (conn, cred) =>
            //val c2 = ec.connectionAs[ArcGisConnection](conn.id).await().get
            warn(cred.prettyPrint)
            conn.updateKnown(true)
            warn(conn.write(false).prettyPrint.blue)
            //if (canUpdateKnown) updateSites(conn)
            //conn.addWebhook().await()
            Future.successful(())
          }.await()
        }

        // Cleanup
        if (!useProductionDB) {
          deleteUsers(user).await()
          checkDBs().await()
        }
        Future.successful(())
      }
    }.await(5.minutes)

  }

}
