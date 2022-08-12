package providers.mediavalet.data

import core.date.DatePlus
import core.jsonic.ToJson.formats
import core.model.HasClazz
import core.util.SeqPlusOpt
import dx.data.JebraPrefix
import env.ApiContext
import play.api.libs.json.{Format, JsObject, Json}

import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 10/28/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 * @todo: Audit option/required properties
 */
case class MediaValetUser(raw: JsObject) extends MediaValetFetched[MediaValetUser]
  with SeqPlusOpt {

  override def seriesName: String = "Users"

  override def resetIndex()(implicit ec: ApiContext): Future[Unit] = {
    ec.dataDB.mediaValet.resetUsersIndex(connectionId)
  }

  override def logId: String = s"${id.blue.ul}/${name.greener}"

  /**
   * @todo: Generalize orgUnitId in superclass?
   */
  def orgUnitId: String = (raw \ "orgUnitId").as[String]

  def name: String = Seq[String]().appendOpt(firstName).appendOpt(lastName).mkString(" ")

  def firstName: Option[String] = (raw \ "firstName").asOpt[String]

  def lastName: Option[String] = (raw \ "lastName").asOpt[String]

}

object MediaValetUser extends HasClazz
  with DatePlus
  with JebraPrefix {

  implicit val formatter: Format[MediaValetUser] = formats[MediaValetUser]

  def t0(connectionId: String): MediaValetUser = {
    MediaValetUser(Json.obj("connectionId" -> connectionId))
  }

}
