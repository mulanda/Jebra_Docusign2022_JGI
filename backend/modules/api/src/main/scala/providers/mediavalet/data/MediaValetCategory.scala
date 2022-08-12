package providers.mediavalet.data

import core.date.DatePlus
import core.jsonic.JsLookupResultPlus.StaticJsLookupResultPlus
import core.jsonic.ToJson.formats
import core.model.HasClazz
import env.ApiContext
import jobs.custom.jgi.Jgi.MediaValetCategories
import play.api.libs.json.{Format, JsObject, Json}

import java.time.LocalDate
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
case class MediaValetCategory(raw: JsObject) extends MediaValetFetched[MediaValetCategory] {

  /** JobData */

  override def seriesName: String = "Categories"

  override def resetIndex()(implicit ec: ApiContext): Future[Unit] = {
    ec.dataDB.mediaValet.resetCategoriesIndex(connectionId)
  }

  override def logId: String = s"${id.blue}/${treeName.red}"

  def name: Option[String] = (raw \ "name").asOpt[String]

  def parentId: String = (raw \ "parentId").as[String]

  def treePath: String = (raw \ "tree" \ "path").as[String]

  def treeName: String = (raw \ "tree" \ "name").as[String]

  def cameraName: Option[String] = {
    treeName match {
      case MediaValetCategories.sourceFolderRegex(cName, cNumber, d1, d2, d3) =>
        //warn(s"(${cName.ul}/${cNumber}) =>> ${d1.greener} -- ${d2.bluer} -- ${d3.purpler}")
        Some(cName)
      case _ => None
    }
  }

  def cameraDate: Option[LocalDate] = {
    treeName match {
      case MediaValetCategories.sourceFolderRegex(cName, cNumber, d1, d2, year) =>
        warn(s"(${cName.ul}/${cNumber}) =>> ${d1.greener} -- ${d2.bluer} -- ${year.purpler}")
        val month = d1.padLeft(2, "0")
        val day = d2.padLeft(2, "0")
        Some(LocalDate.parse(s"$year-$month-$day"))
      case _ => None
    }
  }

  def assetCount: Long = (raw \ "assetCount").asLong

}

object MediaValetCategory extends HasClazz with DatePlus {

  implicit val formatter: Format[MediaValetCategory] = formats[MediaValetCategory]

  def t0(connectionId: String): MediaValetCategory = {
    MediaValetCategory(Json.obj(
      "connectionId" -> connectionId,
    ))
  }

}
