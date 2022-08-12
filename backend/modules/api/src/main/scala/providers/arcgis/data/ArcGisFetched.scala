package providers.arcgis.data

import core.jsonic.JsonReader
import core.model.HasClazz
import dx.data.FetchedData
import play.api.libs.json.{Format, JsResult, JsValue}

import java.util.Date

/**
 * Created by Daudi Chilongo on 08/09/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait ArcGisFetched[T] extends ArcGisData[T] with FetchedData[T] {

  override def dataIdKey: String = "id"

  override def syncDate: Option[Date] = None

}


object ArcGisFetched extends HasClazz with JsonReader {

  override def clazzez: Seq[String] = Seq(
    ArcGisSurveyPayload.clazzez,
  ).flatten

  implicit val formatter: Format[ArcGisFetched[_]] = new Format[ArcGisFetched[_]] {

    override def writes(o: ArcGisFetched[_]): JsValue = o.write

    override def reads(json: JsValue): JsResult[ArcGisFetched[_]] =  {
      (json \ "clazz").asOpt[String] match {
        case Some(clazz) =>
          if (ArcGisSurveyPayload.clazzez.contains(clazz)) {
            ArcGisSurveyPayload.formatter.reads(json)
          } else skipClass(clazz, json)
        case _ => skipNoClass(json)
      }
    }
  }

}
