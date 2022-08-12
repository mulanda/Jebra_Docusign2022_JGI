package jobs.custom.jgi.mediasync

import core.jsonic.JsObjectPlus.JsonWithObject
import core.jsonic.ToJson
import core.jsonic.ToJson.formats
import dx.data.TableID
import env.ApiContext
import jobs.custom.jgi.Jgi.MediaSync.MediaZip
import play.api.libs.json.{Format, JsObject, Json}

import java.io.File
import java.time.YearMonth

/**
 * Created by Daudi Chilongo on 08/11/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
case class JgiMediaSyncMonth(id: String)
  extends ToJson[JgiMediaSyncMonth]
    with JgiMediaSyncProps{

  /** Parse the ID (or throw exception) on construction */
  val yearMonth: YearMonth = YearMonth.parse(id)

  def this(o: JsObject) = this(
    o.objectId,
  )

  override def write(isResponsify: Boolean): JsObject = {
    Json.obj(
      "id" -> id,
      "name" -> yearMonth.getMonth.toString.toLowerCase.ucwords,
    ).compact
  }

  private def tableID: TableID = mediaSyncTableId

  def outputDir(implicit ec: ApiContext): File = new File(tableID.jobOutputDir, id)

  def zipFile(implicit ec: ApiContext): File = outputWithName(MediaZip)

  def outputWithName(name: String)(implicit ec: ApiContext): File = {
    new File(outputDir, name)
  }

}

object JgiMediaSyncMonth {

  implicit val formatter: Format[JgiMediaSyncMonth] = {
    formats[JgiMediaSyncMonth]
  }

  def apply(ym: YearMonth): JgiMediaSyncMonth = {
    JgiMediaSyncMonth(ym.toString)
  }

}
