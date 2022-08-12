package jobs.custom.jgi

import core.jsonic.JsonReader
import core.model.HasClazz
import env.ApiContext
import jobs.PrivateJob
import jobs.custom.jgi.mediasync.JgiMediaSyncJob
import play.api.libs.json.{Format, JsResult, JsValue}

import java.util.TimeZone

/**
 * Created by Daudi Chilongo on 12/08/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
abstract class JgiJob[T] extends PrivateJob[T] {

  override def groupId: String = Jgi.groupId

  override def clientName: Option[String] = Some(Jgi.clientName)

  override def tz: TimeZone = Jgi.tz

  /** Output */

  override def outputDB(implicit ec: ApiContext): JgiDB = {
    super.outputDB.asInstanceOf[JgiDB]
  }

}



object JgiJob extends HasClazz with JsonReader {

  override def clazzez: Seq[String] = Seq(
    JgiMediaSyncJob.clazzez,
  ).flatten

  implicit val formatter: Format[JgiJob[_]] = new Format[JgiJob[_]] {

    override def writes(o: JgiJob[_]): JsValue = o.write

    override def reads(json: JsValue): JsResult[JgiJob[_]] = {
      json.withClazz { clazz =>
        if (JgiMediaSyncJob.clazzez.contains(clazz)) {
          JgiMediaSyncJob.formatter.reads(json)
        } else skipClass(clazz, json)
      }
    }
  }

}
