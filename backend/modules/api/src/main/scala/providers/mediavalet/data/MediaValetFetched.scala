package providers.mediavalet.data

import core.jsonic.JsonReader
import core.model.HasClazz
import dx.data.FetchedData
import play.api.libs.json.{Format, JsResult, JsValue}

import java.util.Date

/**
 * Created by Daudi Chilongo on 08/09/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait MediaValetFetched[T] extends MediaValetData[T] with FetchedData[T] {

  override def dataIdKey: String = "id"

  override def syncDate: Option[Date] = None

}


object MediaValetFetched extends HasClazz with JsonReader {

  override def clazzez: Seq[String] = Seq(
    MediaValetAsset.clazzez,
    MediaValetAttribute.clazzez,
    MediaValetCategory.clazzez,
    MediaValetOrgUnit.clazzez,
    MediaValetUser.clazzez,
  ).flatten

  implicit val formatter: Format[MediaValetFetched[_]] = new Format[MediaValetFetched[_]] {

    override def writes(o: MediaValetFetched[_]): JsValue = o.write

    override def reads(json: JsValue): JsResult[MediaValetFetched[_]] =  {
      (json \ "clazz").asOpt[String] match {
        case Some(clazz) =>
          if (MediaValetAsset.clazzez.contains(clazz)) {
            MediaValetAsset.formatter.reads(json)
          } else if (MediaValetAttribute.clazzez.contains(clazz)) {
            MediaValetAttribute.formatter.reads(json)
          } else if (MediaValetCategory.clazzez.contains(clazz)) {
            MediaValetCategory.formatter.reads(json)
          } else if (MediaValetOrgUnit.clazzez.contains(clazz)) {
            MediaValetOrgUnit.formatter.reads(json)
          } else if (MediaValetUser.clazzez.contains(clazz)) {
            MediaValetUser.formatter.reads(json)
          } else skipClass(clazz, json)
        case _ => skipNoClass(json)
      }
    }
  }

}
