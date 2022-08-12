package jobs.custom.jgi.mediasync.output

import core.date.DatePlus
import core.jsonic.JsObjectPlus.JsonWithObject
import core.jsonic.ToJson.{formats, formatsYearMonth}
import core.model.HasClazz
import dx.data.TableID
import env.ApiContext
import jobs.custom.jgi.JgiOutput
import play.api.libs.json.{Format, JsObject, Json}

import java.time.YearMonth
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/23/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
case class JgiMediaRecord(raw: JsObject)
  extends JgiOutput[JgiMediaRecord] {

  override def seriesName: String = s"Jgi_MediaRecords"

  override def resetIndex()(implicit ec: ApiContext): Future[Unit] = {
    db.resetMediaRecordsIndex(tableID)
  }

  override def id: String = assetId

  def assetId: String = (raw \ "assetId").as[String]

  def assetFilename: String = (raw \ "assetFilename").as[String]

  def cameraId: String = (raw \ "cameraId").as[String]

  def cameraDay: String = (raw \ "cameraDay").as[String]

  def cameraMonth: YearMonth = (raw \ "cameraMonth").as[YearMonth]

  def cameraX: BigDecimal = (raw \ "cameraX").as[BigDecimal]

  def cameraY: BigDecimal = (raw \ "cameraY").as[BigDecimal]

  def warning: Option[String] = (raw \ "warning").asOpt[String]

  def isWarning: Boolean = warning.isDefined

  override def write(isResponsify: Boolean): JsObject = {
    super.write(isResponsify) :++ Json.obj(
      "month" -> cameraMonth.toString,
    )
  }


}

object JgiMediaRecord extends HasClazz with DatePlus {

  implicit val formatter: Format[JgiMediaRecord] = formats[JgiMediaRecord]

  def t0(tableID: TableID): JgiMediaRecord = JgiMediaRecord(Json.obj(
    "jobId" -> tableID.jobId,
    "jobType" -> tableID.jobType,
  ))

  /** Public events */
  val MEDIA_UPDATED = "MEDIA_UPDATED"
  val MEDIA_WARNED = "MEDIA_WARNED"

}
