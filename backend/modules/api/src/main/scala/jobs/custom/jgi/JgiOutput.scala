package jobs.custom.jgi

import core.jsonic.JsLookupResultPlus.StaticJsLookupResultPlus
import core.jsonic.JsonReader
import core.model.HasClazz
import dx.data.OutputData
import env.ApiContext
import jobs.custom.jgi.mediasync.output.JgiMediaRecord
import play.api.libs.json.{Format, JsResult, JsValue, Reads}

/**
 * Created by Daudi Chilongo on 11/13/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
trait JgiOutput[T] extends OutputData[T] {

  override def groupId: String = Jgi.groupId

  override def db(implicit ec: ApiContext): JgiDB = {
    super.db.asInstanceOf[JgiDB]
  }

  /** Processed */
  def isProcessed: Boolean = (raw \ "isProcessed").asBoolean

  def withIsProcessed(processed: Boolean)
                     (implicit reads: Reads[T]): T = {
    setProperty("isProcessed", Some(processed))
  }

}

object JgiOutput extends HasClazz with JsonReader {

  override def clazzez: Seq[String] = Seq(
    JgiMediaRecord.clazzez,
  ).flatten

  implicit val formatter: Format[JgiOutput[_]] = new Format[JgiOutput[_]] {

    override def writes(o: JgiOutput[_]): JsValue = o.write

    override def reads(json: JsValue): JsResult[JgiOutput[_]] = {
      json.withClazz { clazz =>
        if (JgiMediaRecord.clazzez.contains(clazz)) {
          JgiMediaRecord.formatter.reads(json)
        } else skipClass(clazz, json)
      }
    }
  }

  /** Public Attributes */

  val DATA_TYPE: String = "dataType"
  val DATE_KEY = "Date"  // Example: "2020-11-04"

}
