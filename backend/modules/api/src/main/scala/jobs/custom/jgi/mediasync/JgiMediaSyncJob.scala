package jobs.custom.jgi.mediasync

import core.jsonic.JsObjectPlus.JsonWithObject
import core.jsonic.ToJson.formats
import dx.data.OutputData
import env.ApiContext
import jobs.custom.jgi.{Jgi, JgiJob}
import jobs.{JobID, JobLifecycle, JobType}
import org.mongodb.scala.MongoCollection
import play.api.libs.json.{Format, JsObject, Json}

/**
 * Created by Daudi Chilongo on 12/06/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 * In production, `arcGisConnectionId` == `mediaValetConnectionId`, but we allow them to
 * potentially be different so that we can send output to demo accounts
 */
case class JgiMediaSyncJob(id: String,
                           userId: String,
                           name: String,
                           lifecycle: JobLifecycle,
                           arcGisConnectionId: String,
                           docusignConnectionId: String,
                           mediaValetConnectionId: String)
  extends JgiJob[JgiMediaSyncJob] {

  override def connectionIds: Seq[String] = Seq(
    arcGisConnectionId,
    mediaValetConnectionId,
  )

  def this(o: JsObject) = this(
    o.objectId,
    (o \ "userId").as[String],
    (o \ "name").as[String],
    (o \ "lifecycle").as[JobLifecycle],
    (o \ "arcGisConnectionId").as[String],
    (o \ "docusignConnectionId").as[String],
    (o \ "mediaValetConnectionId").as[String],
  )

  override def write(isResponsify: Boolean): JsObject = {
    super.write(isResponsify) ++ Json.obj(
      "arcGisConnectionId" -> arcGisConnectionId,
      "docusignConnectionId" -> docusignConnectionId,
      "mediaValetConnectionId" -> mediaValetConnectionId,
    ).compact
  }

  /** Output */

  override def generatedCollections(implicit ec: ApiContext):
  Seq[MongoCollection[_ <: OutputData[_]]] = Seq(
    outputDB.dataEvents(tableID),
    outputDB.mediaRecords(tableID),
  )

}

object JgiMediaSyncJob extends JobType {

  def apply(ID: JobID)(implicit ec: ApiContext): JgiMediaSyncJob = {
    validateJobType(ID)
    JgiMediaSyncJob(
      id = ID.id,
      userId = ID.userId,
      name = ID.name,
      lifecycle = ID.lifecycle,
      arcGisConnectionId = Jgi.Connections.arcGIS.id,
      docusignConnectionId = Jgi.Connections.docusign.id,
      mediaValetConnectionId = Jgi.Connections.mediaValet.id,
    )
  }

  implicit val formatter: Format[JgiMediaSyncJob] = formats[JgiMediaSyncJob]

}
