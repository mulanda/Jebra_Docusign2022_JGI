package providers.docusign

import core.jsonic.JsLookupResultPlus.StaticJsLookupResultPlus
import core.jsonic.JsObjectPlus.JsonWithObject
import core.jsonic.ToJson
import core.jsonic.ToJson.formats
import core.model.HasClazz
import core.util.Mongo.newObjectId
import core.util.Util.secondsSinceEpoch
import play.api.libs.json.{Format, JsObject, Json}

/**
 * Created by Daudi Chilongo on 08/12/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
case class DocusignSigner(recipientId: Long,
                          name: String,
                          email: String,
                          clientUserId: Option[String] = None, /** Set for embedded */
                          routingOrder: Option[String] = None,
                          createdAt: Long = secondsSinceEpoch,
                          id: String = newObjectId())
  extends ToJson[DocusignSigner] {

  def this(o: JsObject) = this(
    (o \ "recipientId").asLong,
    (o \ "name").as[String],
    (o \ "email").as[String],
    (o \ "clientUserId").asOpt[String],
    (o \ "routingOrder").asOpt[String],
    o.numberLong("createdAt"),
    o.objectId,
  )

  override def write(isResponsify: Boolean): JsObject = Json.obj(
    "id" -> id,
    "recipientId" -> recipientId,
    "name" -> name,
    "email" -> email,
    "clientUserId" -> clientUserId,
    "routingOrder" -> routingOrder,
    "createdAt" -> createdAt,
    "clazz" -> clazz,
  )

}

object DocusignSigner extends HasClazz {

  implicit val formatDocusignSigner: Format[DocusignSigner] = formats[DocusignSigner]

}
