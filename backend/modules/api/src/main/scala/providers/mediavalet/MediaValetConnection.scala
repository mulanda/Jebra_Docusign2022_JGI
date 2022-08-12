package providers.mediavalet

import connections.Connection.Status
import connections.Connection.Status.Idle
import connections.{Connection, ConnectionEvent, ConnectionEventData, ConnectionID}
import core.jsonic.JsLookupResultPlus.StaticJsLookupResultPlus
import core.jsonic.JsObjectPlus.JsonWithObject
import core.jsonic.ToJson.formats
import core.model.{HasClazz, UserAddressLike}
import core.util.Mongo.newObjectId
import env.ApiContext
import play.api.libs.json.{Format, JsObject}
import providers.ApiName.MediaValetAPI
import providers.Provider.MediaValet
import providers.{ApiName, Provider}
import schema.connections.{SetCredentials, UpdateConnection}
import user.User

import java.util.Date
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/08/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
case class MediaValetConnection(userId: String,
                            name: String,
                            status: Status = Idle,
                            createdAt: Date = new Date(),
                            id: String = newObjectId())
  extends Connection[MediaValetConnection] {

  override def provider: Provider = MediaValet

  override def apiName: ApiName = MediaValetAPI

  def this(o: JsObject) = this(
    (o \ "userId").as[String],
    (o \ "name").as[String],
    (o \ "status").as[Status],
    (o \ "createdAt").asDate,
    o.objectId,
  )

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
                         (implicit ec: ApiContext): Future[MediaValetConnection] = {
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

object MediaValetConnection extends HasClazz {

  implicit val formatter: Format[MediaValetConnection] = formats[MediaValetConnection]

  def apply(ID: ConnectionID)
           (implicit ec: ApiContext): MediaValetConnection = new MediaValetConnection(
    id = ID.id,
    userId = ID.userId,
    name = ID.name,
  )

  def apply(by: User, name: String)
           (implicit ec: ApiContext): MediaValetConnection = new MediaValetConnection(
    userId = by.userId,
    name = name,
  )

}
