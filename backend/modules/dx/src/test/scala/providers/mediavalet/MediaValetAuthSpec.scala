package providers.mediavalet

import env.LocalAuthDx
import jobs.custom.jgi.Jgi

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/08/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class MediaValetAuthSpec extends LocalAuthDx  {

  private val resetCredentials: Boolean = false

  override def useProductionDB: Boolean = false

  override def canUpdateKnown: Boolean = resetCredentials

  override def canUpdateKnownCredentials: Boolean = resetCredentials

  "authorize" taggedAs Do in {

    val ID = Jgi.Connections.mediaValet
    withSavedOrKnownConnection(ID, _ => MediaValetConnection(ID)) { c0 =>
      val conn = c0.asInstanceOf[MediaValetConnection]
      conn.withUser { user =>
        conn.api.oAuthManager(user, Some(conn))
          .browserRefresh(resetCredentials)
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
