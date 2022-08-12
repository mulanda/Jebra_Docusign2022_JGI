package providers.mediavalet

import env.DxTestEnv
import jobs.custom.jgi.Jgi
import providers.mediavalet.data.MediaValetAsset

import java.io.File
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class MediaValetDataSpec extends DxTestEnv
  with MediaValetFactory
  with MediaValetConnectionData {

  private lazy val target = Jgi.Connections.mediaValet

  "updateAssets" taggedAs Do in {
    withMediaValetFetcher(target) { (conn, user, client) =>
      conn.updateAssets().await(1.hour)
      val dest = new File(client.logDir, s"Assets_${conn.fsName}.json")
      MediaValetAsset.t0(conn.id).dbToJson(dest, showPath = true).await(30.minutes)
    }
  }

}
