package providers.docusign

import env.LocalAuthDx
import jobs.custom.jgi.Jgi
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/08/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class DocusignAuthSpec extends LocalAuthDx  {

  private val resetCredentials: Boolean = false

  override def useProductionDB: Boolean = false

  override def canUpdateKnown: Boolean = resetCredentials

  override def canUpdateKnownCredentials: Boolean = resetCredentials

  "authorize" taggedAs Do in {

    val ID = Jgi.Connections.docusign
    val accountId: String = Jgi.DocusignAccountId
    withSavedOrKnownConnection(ID, _ => DocusignConnection(ID, accountId)) { c0 =>
      val conn = c0.asInstanceOf[DocusignConnection]
      conn.withUser { user =>
        conn.api.oAuthManager(user, Some(conn))
          .browserRefresh(resetCredentials, Json.obj("accountId" -> conn.accountId))
          .await(10.minutes)
          .foreach { cred =>
            warn(cred.prettyPrint)
            val c2 = ec.connectionWithId(conn.id).await().get
            c2.updateKnown(true)
            warn(c2.write(false).prettyPrint.blue)
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
