package providers.arcgis.data

import core.jsonic.ToJson.formats
import core.model.HasClazz
import env.ApiContext
import play.api.libs.json.{Format, JsObject, Json}

import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 12/20/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
case class ArcGisSurveyPayload(raw: JsObject) extends ArcGisFetched[ArcGisSurveyPayload] {

  /** JobData */

  override def seriesName: String = "SurveyPayload"

  /** Feature */

  override def id: String = globalResultId.strip("{}")

  def globalResultId: String = (raw \ "feature" \ "result" \ "globalId").as[String]

  /** Event Info */

  def eventType: String = (raw \ "eventType").as[String]

  def isAddData: Boolean = eventType == "addData"

  /** UserInfo */

  def username: String = (raw \ "userInfo" \ "formItemId").as[String]

  def firstName: String = (raw \ "userInfo" \ "firstName").as[String]

  def lastName: String = (raw \ "userInfo" \ "lastName").as[String]

  def fullName: String = (raw \ "userInfo" \ "fullName").as[String]

  def email: String = (raw \ "userInfo" \ "email").as[String]

  /** SurveyInfo */

  def formItemId: String = (raw \ "surveyInfo" \ "formItemId").as[String]

  def formTitle: String = (raw \ "surveyInfo" \ "formTitle").as[String]

  def serviceItemId: String = (raw \ "surveyInfo" \ "serviceItemId").as[String]

  def serviceUrl: String = (raw \ "surveyInfo" \ "serviceUrl").as[String]

  /** Camera Info */

  def cameraId: String = (raw \ "feature" \ "attributes" \ "cameraId").as[String]

  def cameraX: BigDecimal = {
    (raw \ "feature" \ "attributes" \ "cameraCoordinates" \ "x").as[BigDecimal]
  }

  def cameraY: BigDecimal = {
    (raw \ "feature" \ "attributes" \ "cameraCoordinates" \ "y").as[BigDecimal]
  }

  override def resetIndex()(implicit ec: ApiContext): Future[Unit] = {
    ec.dataDB.arcGis.resetSurveyPayloadIndex(connectionId)
  }

}

object ArcGisSurveyPayload extends HasClazz {

  implicit val formatter: Format[ArcGisSurveyPayload] = formats[ArcGisSurveyPayload]

  def t0(connectionId: String): ArcGisSurveyPayload = {
    ArcGisSurveyPayload(Json.obj(
      "connectionId" -> connectionId,
    ))
  }

}
