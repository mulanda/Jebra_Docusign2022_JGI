package jobs.custom.jgi

import core.date.LocalDatePlus
import core.db.MongoDBPageInsert
import core.jsonic.JsObjectPlus.JsonWithObject
import core.string.StringPlus
import core.util.SeqPlus.WithSeq
import env.ApiContext
import jobs.custom.jgi.Jgi.MediaValetCategories
import jobs.custom.jgi.mediasync.output.JgiMediaRecord
import jobs.custom.jgi.mediasync.{JgiMediaSyncJob, JgiMediaSyncProps}
import play.api.libs.json.Json
import providers.arcgis.data.ArcGisSurveyPayload
import providers.mediavalet.data.{MediaValetAsset, MediaValetCategory}

import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/11/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */

class JgiMediaSyncManager(implicit ec: ApiContext)
  extends JgiMediaSyncProps
    with LocalDatePlus
    with StringPlus
    with MongoDBPageInsert {

  def generateRecords(job: JgiMediaSyncJob,
                      reset: Boolean = false): Future[Unit] = {
    mock("generateRecords", s"Reset=${reset.bold}")
    (if (reset) {
      mediaRecords
        .deleteAll()
        .map { result =>
          warn(s"Deleted ${result.getDeletedCount.bold} records..")
        }
    } else Future.successful(())).flatMap { _ =>
      selectSources(job).flatMap { sources =>
        warn(s"Found ${sources.length} sources...")
        sources.foreach { t =>
          warn(t.logText)
          warn(t.treePath.ul)
        }
        buildCameraMap(job).flatMap { cameras =>
          generateMediaRecords(job,
            cameras.map(kv => (kv._1, kv._2.head)),
            sources).map { records =>
            records.foreach(r => warn(r.prettyPrint.blue))
          }
        }
      }
    }
  }

  private def selectSources(job: JgiMediaSyncJob): Future[Seq[MediaValetCategory]] = {
    port("selectSources")
    val t0 = MediaValetCategory.t0(job.mediaValetConnectionId)
    val sourceIds = MediaValetCategories.sources
    ec.dataDB.coll(t0).find().toFuture().map { categories =>
      val roots = categories.filter(c => sourceIds.contains(c.id))
      if (roots.isEmpty) {
        warn("ZERO categories found...")
        Seq()
      } else {
        categories.filter { category =>
          category.cameraName match {
            case Some(_) => roots.exists(r => category.treePath.startsWith(r.treePath))
            case _ => false
          }
        }
      }
    }
  }

  private def buildCameraMap(job: JgiMediaSyncJob,
                             isStrict: Boolean = false): Future[Map[String, Seq[ArcGisSurveyPayload]]] = {
    val t0 = ArcGisSurveyPayload.t0(job.arcGisConnectionId)
    ec.dataDB.coll(t0).find().toFuture().map { surveys =>
      val groups = surveys.groupBy(_.cameraId)
      if (isStrict) {
        /** @todo: Check for conflicting camera info */
        warn("TODO: check for conflicts...")
      }
      groups
    }
  }

  private def generateMediaRecords(job: JgiMediaSyncJob,
                                   cameras: Map[String, ArcGisSurveyPayload],
                                   sources: Seq[MediaValetCategory]): Future[Seq[JgiMediaRecord]] = {
    mock("generateMediaRecords")
    cameras.foreach(kv => warn(s"${kv._1}=${kv._2.cameraId}"))
    val t0 = MediaValetAsset.t0(job.mediaValetConnectionId)
    ec.dataDB.coll(t0).find().toFuture().flatMap { found =>
      val sourceToAssetMap = found.map { asset =>
        // Parent folder
        (sources.find(s => asset.categories.contains(s.id))
          .flatMap { parent =>
            warn(s"TreeNAME=${parent.treeName}/${parent.cameraName.yellow}")
            //parent
            cameras.get(parent.cameraName.get).map(_ => parent)
          },
          asset)
      }.filter(_._1.nonEmpty).map(kv => (kv._1.get, kv._2))

      warn(s"Selected ${sourceToAssetMap.length.greener} of ${found.length.bold} assets...")
      sourceToAssetMap.serialMap { case (source, asset) =>
        val survey = cameras(source.cameraName.get)
        //val cameraMonth = source.cameraDate.map(_.yearMonth.toString)
        JgiMediaRecord.t0(job.tableID).prepareRaw(
          Json.obj(
            "assetId" -> asset.id,
            "assetFilename" -> asset.filename,
            "cameraDay" -> source.cameraDate,
            "cameraMonth" -> source.cameraDate.map(_.yearMonth.toString),
            "cameraId" -> survey.cameraId,
            "cameraX" -> survey.cameraX,
            "cameraY" -> survey.cameraY,
          ).compact
        ).as[JgiMediaRecord].upsert()
      }
    }
  }
}