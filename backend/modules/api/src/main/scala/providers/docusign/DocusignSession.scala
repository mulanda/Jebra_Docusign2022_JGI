package providers.docusign

import core.date.DatePlus
import core.jsonic.JsLookupResultPlus.StaticJsLookupResultPlus
import core.jsonic.JsObjectPlus.JsonWithObject
import core.jsonic.ToJson.{formats, formatsFiniteDuration}
import core.model.HasClazz
import core.util.Mongo.newObjectId
import env.{ApiContext, UserDBDocument}
import play.api.libs.json.{Format, JsObject, Json}

import java.util.Date
import scala.concurrent.duration.FiniteDuration

/**
 * Created by Daudi Chilongo on 08/12/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
case class DocusignSession(userId: String,
                           jobId: String,
                           callback: String,
                           params: JsObject,
                           id: String = newObjectId(),
                           createdAt: Date = new Date(),
                           ttl: Option[FiniteDuration] = None)
  extends UserDBDocument[DocusignSession] with DatePlus {

  def this(o: JsObject) = this(
    (o \ "userId").as[String],
    (o \ "jobId").as[String],
    (o \ "callback").as[String],
    (o \ "params").as[JsObject],
    o.objectId,
    (o \ "createdAt").asDate,
    (o \ "ttl").asOpt[FiniteDuration],
  )

  override def write(isResponsify: Boolean): JsObject = Json.obj(
    "id" -> id,
    "userId" -> userId,
    "jobId" -> jobId,
    "callback" -> callback,
    "params" -> params,
    "createdAt" -> createdAt,
    "ttl" -> ttl,
    "clazz" -> clazz,
  ).compact

  def isExpired(implicit ec: ApiContext): Boolean = {
    (createdAt + ttl.getOrElse(ec.authSessionTTL)).before(new Date())
  }

}

object DocusignSession extends HasClazz {

  implicit val formatter: Format[DocusignSession] = formats[DocusignSession]

}
