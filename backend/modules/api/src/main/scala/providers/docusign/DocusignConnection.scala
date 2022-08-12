package providers.docusign

import connections.Connection.Status
import connections.Connection.Status.Idle
import connections.{Connection, ConnectionEvent, ConnectionEventData, ConnectionID}
import core.jsonic.JsLookupResultPlus.StaticJsLookupResultPlus
import core.jsonic.JsObjectPlus.JsonWithObject
import core.jsonic.ToJson.formats
import core.model.{HasClazz, UserAddressLike}
import core.util.Mongo.newObjectId
import env.ApiContext
import jobs.custom.jgi.Jgi
import play.api.libs.json.{Format, JsObject, Json}
import providers.ApiName.DocusignAPI
import providers.Provider.Docusign
import providers.{ApiName, Provider}
import schema.connections.{SetCredentials, UpdateConnection}
import user.User

import java.util.Date
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/08/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
case class DocusignConnection(userId: String,
                              name: String,
                              accountId: String,
                              status: Status = Idle,
                              createdAt: Date = new Date(),
                              id: String = newObjectId())
  extends Connection[DocusignConnection] {

  override def provider: Provider = Docusign

  override def apiName: ApiName = DocusignAPI

  def this(o: JsObject) = this(
    (o \ "userId").as[String],
    (o \ "name").as[String],
    (o \ "accountId").asOpt[String].getOrElse(
      Jgi.DocusignAccountId /** @todo: Remove! */
    ),
    (o \ "status").as[Status],
    (o \ "createdAt").asDate,
    o.objectId,
  )

  override def write(isResponsify: Boolean): JsObject = {
    super.write(isResponsify) :++ Json.obj(
      "accountId" -> accountId,
    )
  }

  /**  HasStatusEvents */

  override def eventOfType(eventType: ConnectionEvent.Type,
                           by: UserAddressLike,
                           data: Option[ConnectionEventData[_]]): ConnectionEvent = {
    val event = super.eventOfType(eventType, by, data)
    data match {
      case Some(params: SetCredentials) => event.copy(date = params.date)
      case _ => event
    }
  }

  override def updateWith(event: ConnectionEvent)
                         (implicit ec: ApiContext): Future[DocusignConnection] = {
    event.data match {
      case Some(u: UpdateConnection) =>
        copy(
          status = nextStatus(event.eventType),
          name = u.name,
        ).upsert()
      case _ => copy(status = nextStatus(event.eventType)).upsert()
    }
  }


}

object DocusignConnection extends HasClazz {

  /** @todo: Remove post-migration */

  override def clazzez: Seq[String] = super.clazzez ++ Seq(
    "providers.mediavalet.DocuSignConnection",
  )

  implicit val formatter: Format[DocusignConnection] = formats[DocusignConnection]

  def apply(ID: ConnectionID,
            accountId: String)
           (implicit ec: ApiContext): DocusignConnection = new DocusignConnection(
    id = ID.id,
    userId = ID.userId,
    name = ID.name,
    accountId = accountId
  )

  def apply(by: User, name: String, accountId: String)
           (implicit ec: ApiContext): DocusignConnection = new DocusignConnection(
    userId = by.userId,
    name = name,
    accountId = accountId
  )

}
