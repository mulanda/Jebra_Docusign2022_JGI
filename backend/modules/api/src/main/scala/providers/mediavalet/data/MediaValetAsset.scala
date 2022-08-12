package providers.mediavalet.data

import core.jsonic.JsLookupResultPlus.StaticJsLookupResultPlus
import core.jsonic.ToJson.formats
import core.model.HasClazz
import env.ApiContext
import play.api.libs.json.{Format, JsObject, Json}

import java.time.OffsetDateTime
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 12/20/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
case class MediaValetAsset(raw: JsObject) extends MediaValetUpdated[MediaValetAsset] {

  /** JobData */

  override def seriesName: String = "Assets"

  override def resetIndex()(implicit ec: ApiContext): Future[Unit] = {
    ec.dataDB.mediaValet.resetAssetsIndex(connectionId)
  }

  override def updatedAt: OffsetDateTime = offsetDate(raw \ "record" \ "modifiedAt").get

  def categories: Seq[String] = (raw \ "categories").asSeq[String]

  def filename: String = (raw \ "file" \ "fileName").as[String]

}

object MediaValetAsset extends HasClazz {

  implicit val formatter: Format[MediaValetAsset] = formats[MediaValetAsset]

  def t0(connectionId: String): MediaValetAsset = {
    MediaValetAsset(Json.obj(
      "connectionId" -> connectionId,
    ))
  }

}
