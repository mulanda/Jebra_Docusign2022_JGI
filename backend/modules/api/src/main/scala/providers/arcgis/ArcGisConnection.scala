package providers.arcgis

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
import providers.ApiName.ArcGisOnline
import providers.Provider.ArcGis
import providers.{ApiName, Provider}
import schema.connections.{SetCredentials, UpdateConnection}
import user.User

import java.util.Date
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/02/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
case class ArcGisConnection(userId: String,
                            name: String,
                            apiName: ApiName,
                            status: Status = Idle,
                            createdAt: Date = new Date(),
                            id: String = newObjectId())
  extends Connection[ArcGisConnection] {

  override def provider: Provider = ArcGis


  def this(o: JsObject) = this(
    (o \ "userId").as[String],
    (o \ "name").as[String],
    (o \ "api").as[ApiName],
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
                         (implicit ec: ApiContext): Future[ArcGisConnection] = {
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

object ArcGisConnection extends HasClazz {

  /** @todo: Remove post-migration */

  override def clazzez: Seq[String] = super.clazzez ++ Seq(
    "providers.arcgis.ArcGISConnection",
  )

  implicit val formatter: Format[ArcGisConnection] = formats[ArcGisConnection]

  def apply(ID: ConnectionID,
            apiName: ApiName)
           (implicit ec: ApiContext): ArcGisConnection = new ArcGisConnection(
    id = ID.id,
    userId = ID.userId,
    name = ID.name,
    apiName = apiName
  )

  def apply(by: User, name: String)
           (implicit ec: ApiContext): ArcGisConnection = new ArcGisConnection(
    userId = by.userId,
    name = name,
    apiName = ArcGisOnline, /** @todo: Parametrize */
  )

}
