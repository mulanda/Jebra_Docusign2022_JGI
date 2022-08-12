package providers.mediavalet.data

import core.date.DatePlus
import core.jsonic.ToJson.formats
import core.model.HasClazz
import env.ApiContext
import play.api.libs.json.{Format, JsObject, Json}

import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 01/18/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
case class MediaValetAttribute(raw: JsObject) extends MediaValetFetched[MediaValetAttribute] {

  /** JobData */

  override def seriesName: String = "Attributes"

  override def resetIndex()(implicit ec: ApiContext): Future[Unit] = {
    ec.dataDB.mediaValet.resetAttributesIndex(connectionId)
  }

  def attributeType: String = (raw \ "attributeType").as[String]

  def name: String = (raw \ "name").as[String]

  def tagName: Option[String] = (raw \ "tagName").asOpt[String]

}

object MediaValetAttribute extends HasClazz with DatePlus {

  implicit val formatter: Format[MediaValetAttribute] = formats[MediaValetAttribute]

  def t0(connectionId: String): MediaValetAttribute = {
    MediaValetAttribute(Json.obj(
      "connectionId" -> connectionId,
    ))
  }

}
