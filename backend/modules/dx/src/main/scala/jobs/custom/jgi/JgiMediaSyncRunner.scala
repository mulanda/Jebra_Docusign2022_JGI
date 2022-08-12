package jobs.custom.jgi

import core.date.DatePlus
import core.file.FileZipping
import core.util.SeqPlus.WithSeq
import dx.DxContext
import env.ApiDefaults
import jobs.custom.jgi.mediasync.output.JgiMediaRecord
import jobs.custom.jgi.mediasync.{JgiMediaSyncJob, JgiMediaSyncJobRequest}
import jobs.requests.JobRequest
import jobs.{JobRun, JobRunner, SimpleJobRunner}
import play.api.libs.json.Json
import providers.arcgis.data.ArcGisSurveyPayload
import providers.mediavalet.data.{MediaValetAsset, MediaValetAttribute, MediaValetCategory}
import providers.mediavalet.{MediaValetConnection, MediaValetConnectionData, MediaValetFetcher}

import java.util.Date
import scala.annotation.nowarn
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class JgiMediaSyncRunner(val job: JgiMediaSyncJob,
                         val jobRunAtStart: JobRun)
                        (implicit val ec: DxContext)
  extends SimpleJobRunner
    with JobRunner
    with MediaValetConnectionData
    with DatePlus
    with FileZipping
    with JgiMediaSyncWriter {

  /** Helper for casting JobRequest */

  private def jobRequest(r: JobRequest[_]): JgiMediaSyncJobRequest = {
    r.asInstanceOf[JgiMediaSyncJobRequest]
  }

  override def lastRequest: JgiMediaSyncJobRequest = jobRequest(super.lastRequest)

  @nowarn("msg=never used")
  private def arcGisConnectionId: String = job.arcGisConnectionId

  @nowarn("msg=never used")
  private def docusignConnectionId: String = job.docusignConnectionId

  private def mediaValetConnectionId: String = job.mediaValetConnectionId

  /** Fetch data */

  override def fetchData(): Future[Unit] = {
    for {
      _ <- jobRun.resetStats().save() /** Reset stats */
      _ <- inputTree
      _ <- updateMediaValet()
    } yield {}
  }

  private def inputTree: Future[Unit] = {
    jobSettings.map { s =>
      warn(s.prettyPrint)
    }
  }

  private def updateMediaValet(): Future[Unit] = {
    warn("updateMediaValet...")
    for {
      conn <- ec.connectionAs[MediaValetConnection](mediaValetConnectionId).map(_.get)
      _ <- conn.updateCategories()
      _ <- conn.updateAttributes()
      _ <- conn.updateAssets()
    } yield {}
  }

  /** Merged zip */

  override def buildMergedZip(): Future[Unit] = {
    for {
      _ <- resetMergedZip()
      _ <- writeArcGisJSONs()
      _ <- writeMediaValetJSONs()
      _ <- writeMediaRecords()
      _ <- writeJobToZip()
      _ <- patchAssets() /** @todo: Move? */
    } yield {}
  }

  private def writeArcGisJSONs(): Future[Unit] = {
    val connectionId: String = arcGisConnectionId
    Seq(
      ArcGisSurveyPayload.t0(connectionId)).serially { t0 =>
      t0.dbToJson(t0.mergedJSON).map(_ => ())
    }
  }

  private def writeMediaValetJSONs(): Future[Unit] = {
    val connectionId: String = mediaValetConnectionId
    Seq(
      MediaValetAsset.t0(connectionId),
      MediaValetAttribute.t0(connectionId),
      MediaValetCategory.t0(connectionId),
    ).serially { t0 =>
      t0.dbToJson(t0.mergedJSON).map(_ => ())
    }
  }

  private def writeMediaRecords(): Future[Unit] = {
    warn("writeMediaRecords...")
    resetMediaRecords().flatMap { _ =>
      val t0 = JgiMediaRecord.t0(tableID)
      t0.dbToJson(t0.mergedJSON).map(_ => ())
    }.flatMap { _ =>
      /** Add generated stats */
      mediaRecords.find().toFuture().flatMap { docs =>
        val t0 = JgiMediaRecord.t0(tableID)
        jobRun.addGeneratedCounts(Json.obj(
          t0.seriesName -> docs.length
        )).save()
      }
    }
  }

  private def patchAssets(): Future[Unit] = {
    for {
      conn <- ec.connectionAs[MediaValetConnection](mediaValetConnectionId).map(_.get)
      user <- conn.withUser(Future.successful)
      (xAttr, yAttr) <- {
        ec.dataDB.mediaValet
          .attributes(mediaValetConnectionId)
          .find ().toFuture ().map { attributes =>
          val xAttr = attributes.find(_.name == "X").get
          val yAttr = attributes.find(_.name == "Y").get
          (xAttr, yAttr)
        }
      }
      docs <- mediaRecords.find().toFuture()
      _ <- {
        warn(s"Patching ${docs.length} docs...")
        val fetcher = MediaValetFetcher(user, conn)
        docs.serially { doc =>
          val values = Seq(
            "altText" -> s"JR-$runNumber @ ${ApiDefaults.tz.iso8601(new Date())}",
            s"attributes/${xAttr.id}" -> doc.cameraX.toString,
            s"attributes/${yAttr.id}" -> doc.cameraY.toString,
          )
          fetcher.patchAsset(doc.assetId, replace = values).flatMap { _ =>
            /** Throttle down to reduce chances of rate-limiting error @todo: Audit */
            ec.after(1.second)(Future.successful(()))
          }
        }
      }
    } yield {}

  }

  /** Notification */

  override protected def skipNotifyAnalytics: Boolean = true

  /** Posting */

  override def canPost: Boolean = true

}
