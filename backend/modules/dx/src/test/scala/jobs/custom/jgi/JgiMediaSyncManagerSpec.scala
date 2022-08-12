package jobs.custom.jgi

import core.db.MongoDBPageInsert
import env.DxTestEnv
import jobs.custom.jgi.mediasync.JgiMediaSyncProps
import jobs.custom.jgi.mediasync.output.JgiMediaRecord
import play.api.libs.json.Json

import java.io.File
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/11/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class JgiMediaSyncManagerSpec extends DxTestEnv
  with JgiMediaSyncProps
  with MongoDBPageInsert {

  override def useProductionDB: Boolean = false

  "generateRecords" taggedAs Do ignore {

    ec.withJgiMediaSync(Jgi.Jobs.mediaSync) { (job, deputy, nmbrs, zingfit) =>

      val dest = new File(ec.externalDataDir, s"Jgi/MediaRecords.json").ensureParent

      val generateRecords: Boolean = true
      val allDocs = if (generateRecords) {
        val manager = new JgiMediaSyncManager()
        manager.generateRecords(job, true).await(10.minutes)
        mediaRecords.find().await()
      } else {
        val docs = Json.parse(dest.read()).asSeq[JgiMediaRecord]
        replacePage(mediaRecords, 0, docs).await()
        docs
      }
      allDocs.toJson.prettyPrint.writeBuffered(dest, showPath = true)
      Seq(
        ("TOTAL", allDocs.length),
      ).foreach { case (k, v) => warn(s"${k.bold}: ${v.bluer}") }

    }
  }

}
