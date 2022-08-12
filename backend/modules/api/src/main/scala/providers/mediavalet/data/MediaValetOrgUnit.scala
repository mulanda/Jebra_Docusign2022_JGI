package providers.mediavalet.data

import core.date.DatePlus
import core.jsonic.ToJson.formats
import core.model.HasClazz
import env.ApiContext
import play.api.libs.json.{Format, JsObject, Json}

import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 12/20/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
case class MediaValetOrgUnit(raw: JsObject) extends MediaValetFetched[MediaValetOrgUnit] {

  /** JobData */

  override def seriesName: String = "OrgUnits"

  override def resetIndex()(implicit ec: ApiContext): Future[Unit] = {
    ec.dataDB.mediaValet.resetOrgUnitsIndex(connectionId)
  }

  def name: String = (raw \ "names" \ "name").as[String]

  def canonicalName: String = (raw \ "names" \ "canonicalName").as[String]

}

object MediaValetOrgUnit extends HasClazz with DatePlus {

  implicit val formatter: Format[MediaValetOrgUnit] = formats[MediaValetOrgUnit]

  def t0(connectionId: String): MediaValetOrgUnit = {
    MediaValetOrgUnit(Json.obj(
      "connectionId" -> connectionId,
    ))
  }

}
