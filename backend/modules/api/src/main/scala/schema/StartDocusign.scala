package schema

import core.jsonic.JsObjectPlus.JsonWithObject
import core.jsonic.ToJson
import core.jsonic.ToJson.formats
import play.api.libs.json.{Format, JsObject, Json}
import schema.Param.URLParam

/**
 * Created by Daudi Chilongo on 08/12/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 * @todo: Remove?
 */
case class StartDocusign(callback: String)
  extends ToJson[StartDocusign] {

  def this(o: JsObject) = this (
    (o \ "callback").as[String],
  )

  def write(isResponsify: Boolean): JsObject = Json.obj(
    "callback" -> callback,
  ).compact

}

object StartDocusign extends Schema[StartDocusign] {

  implicit val formatsStartDocusign: Format[StartDocusign] = formats[StartDocusign]

  val params: Seq[Param] = Seq(
    URLParam("callback").required,
  )


}
