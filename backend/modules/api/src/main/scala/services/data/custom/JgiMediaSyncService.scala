package services.data.custom

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import auth.ResourceAuth
import core.jsonic.JsObjectPlus.JsonWithObject
import core.model.{HeaderNames, Reply}
import env.ApiContext
import jobs.JobRoutes
import jobs.custom.jgi.Jgi
import jobs.custom.jgi.mediasync.output.JgiMediaRecord
import jobs.custom.jgi.mediasync.{JgiMediaSyncJob, JgiMediaSyncMonth, JgiMediaSyncProps}
import jobs.requests.JobRequest
import org.mongodb.scala.model.Aggregates.{group, limit, sort}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Sorts.descending
import play.api.libs.json.{JsArray, JsObject, Json}
import services.DocusignService
import services.data.DataZipBuilder
import user.User

import java.io.File
import java.time.format.DateTimeParseException
import java.util.UUID
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/12/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait JgiMediaSyncService extends ResourceAuth
  with DataZipBuilder
  with JobRoutes
  with DocusignService {

  implicit class JgiMediaSyncData(val job: JgiMediaSyncJob)
                                 (implicit ec: ApiContext, by: User)
    extends JgiMediaSyncProps{

    def routes(query: Query): Route = {
      pathPrefix("d" / "months") {
        concat(
          pathEnd {
            get {
              completeWithReply(getMonths)
            }
          },
          pathPrefix(Segment) { docId =>
            warn(docId.cayenne.ul)
            concat(
              pathEnd {
                concat(
                  get {
                    completeWithReply(getMonth(docId))
                  },
                  post {
                    entity(as[JsObject]) { body =>
                      completeWithReply(Future.failed(Reply.MethodNotImplemented("TODO")))
                    }
                  },
                )
              },

              /** We support full-month download only @todo: Audit */
              path("download") {
                get {
                  optionalHeaderValueByName(HeaderNames.AUTHORIZATION) { auth =>
                    downloadZip(docId, auth)
                  }
                }
              },

              path("update") {
                /** User actions */
                post {
                  completeWithReply(userUpdateAction(docId))
                }
              },

              pathPrefix(Segment) { dataType =>
                /** @todo: Add other data types? */
                warn(s"dataType=${dataType.red.ul}")
                concat(
                  pathEnd {
                    concat(
                      get {
                        completeWithReply(getMonth(docId))
                      },
                    )
                  },
                  path("update") {
                    /** User actions */
                    post {
                      completeWithReply(userUpdateAction(docId))
                    }
                  },

                  /** Sign zip */
                  path("sign") {
                    post {
                      entity(as[JsObject]) { body =>
                        completeWithReply(signZip(docId, body))
                      }

                    }
                  },
                )
              },

            )
          }
        )
      }
    }

    /** @todo: Deprecate legacy */
    def getDocuments(category: String): Future[Reply] = {
      category match {
        case "months" => getMonths
        case _ => Future.failed(Reply.InvalidData(s"Unexpected category: $category"))
      }
    }

    /** @todo: Deprecate legacy */
    def getDocument(category: String, docId: String): Future[Reply] = {
      category match {
        case "months" => getMonth(docId)
        case _ => Future.successful(Reply.InvalidData(s"Unexpected category: $category"))
      }
    }

    private def getMonths: Future[Reply] = {
      port("getMonths", job.logText)
      ec.dataDB
        .docs(mediaRecords.name)
        .aggregate(Seq(
          group("$month"),
          sort(descending("_id")),
          limit(12), /** Limit to last year @todo: Audit hardcoded value */
        ))
        .toFuture()
        .map { groups =>
          val months = groups.map(g => Json.parse(g.toJson()).as[JgiMediaSyncMonth])
          val default = months
            .find(_.yearMonth == Jgi.MediaSync.defaultYearMonth)
            .orElse(months.headOption)
          val js = months.map { doc =>
            doc.write(isResponsify = true) :++ Json.obj(
              "isDefault" -> default.exists(_.id == doc.id)
            )
          }
          Reply.Ok(JsArray(js))
        }
    }

    private def getMonth(docId: String): Future[Reply] = {
      port("getMonth", job.logText, docId)
      try {
        val month = JgiMediaSyncMonth(docId)
        mediaRecords.find(equal("month", docId))
          .toFuture()
          .map { docs =>
            val js = month.write(true) :++ Json.obj(
              "docs" -> docs.flatMap(_.csvRows)
            )
            Reply.Ok(js)
          }
      } catch {
        case _: DateTimeParseException =>
          Future.failed(Reply.InvalidData(s"Invalid month $docId"))
      }
    }

    private def downloadZip(docId: String, auth: Option[String]): Route = {
      port("downloadZip", docId)
      val month = JgiMediaSyncMonth(docId)
      val zipFile = month.zipFile.ensureParent
      val f = {
        if (ec.isOutputDataStore) {
          if (zipFile.exists()) {
            Future.successful(())
          } else {
            /** @todo: Disable test workflow on PROD */
            warn("‼️ ZIP FILE NOT FOUND, BUILDING...")
            buildZip(month)
          }
        } else {
          val uri = job.docsUrl(s"revenue/$docId/download/zip")
          fetchDataFile(uri, month.zipFile, auth)
        }
      }
      completeWithRoute(f.flatMap { _ =>
        warn(zipFile.bold)
        if (zipFile.exists()) {
          warn(s"RETURN ${zipFile.absolutePath.ul}")
          sendFile(zipFile, "live-copy")
        } else Future.failed(Reply.NotFound("Revenue data not available"))
      })
    }

    private def buildZip(month: JgiMediaSyncMonth): Future[File] = {
      port(s"buildZip", month.id)
      val zipName = Jgi.MediaSync.MediaZip
      val zipRoot = new File(ec.tmpDir, UUID.randomUUID().toString).ensure
      val zipDir = new File(zipRoot, s"${zipName.baseName}_${month.id}").ensure
      val zipFile = zipRoot.withExtension("zip")
      writeMediaRecords(month, zipDir).map { _ =>
        zipDir.zipTo(zipFile)
        zipRoot.deleteRecursively()
        zipFile.moveTo(month.zipFile, showPath = true)
      }
    }

    private def writeMediaRecords(month: JgiMediaSyncMonth,
                                  toDir: File): Future[Unit] = {
      port(s"writeMediaRecords", month.id)
      mediaRecords.find(equal("month", month.id))
        .toFuture().map { records =>
        warn(s"Found ${records.length} records")
        val t0 = JgiMediaRecord.t0(mediaSyncTableId)
        records
          .toJson
          .prettyPrint
          .writeBuffered(
            new File(toDir, s"${t0.seriesName}.json"),
            showPath = true)
      }
    }

    private def userUpdateAction(monthId: String): Future[Reply] = {
      port("jobAction", monthId)
      val month = JgiMediaSyncMonth(monthId) // Force monthId parsing
      val js = Json.obj(JobRequest.MONTH -> month.id)
      ec.jobService.addJobRun(by, job.id, js).flatMap { _ =>
        getMonth(month.id)
      }
    }

    private def signZip(monthId: String, body: JsObject): Future[Reply] = {
      port("signZip", monthId)
      warn(body.prettyPrint)
      startSigning(by, job, body)
    }

  }

}
