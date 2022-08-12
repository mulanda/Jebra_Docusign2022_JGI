package jobs.custom.jgi

import env.DxTestEnv
import jobs.GroupFixtures
import jobs.custom.jgi.mediasync.JgiMediaSyncProps
import jobs.custom.jgi.mediasync.output.JgiMediaRecord
import play.api.libs.json.Json

import java.io.File

/**
 * Created by Daudi Chilongo on 08/11/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class JgiMediaSyncDataSpec extends DxTestEnv
  with JgiMediaSyncProps {

  "postRecords" taggedAs Do in {

    val tableID = mediaSyncJobId.tableID
    val fixtures = GroupFixtures(Jgi.groupId)
    val src: File = fixtures.groupFixture(s"MediaRecords.json")
    Json.parse(src.read()).asSeq[JgiMediaRecord]
      .slice(0, 2)
      .foreach { doc =>
        warn(doc.prettyPrint)
      }

    // Cleanup
    if (!useProductionDB) {
      dataDB.output.jgi.mediaRecords(tableID).drop().await() // Manual reset
      checkDBs().await()
    }
  }

}
