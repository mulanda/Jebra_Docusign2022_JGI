package providers.arcgis

import connections.ConnectionID
import env.KnownConnections
import user.User

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait ArcGisFactory extends KnownConnections {

  def withArcGis[A](id: ConnectionID)
                   (f: (ArcGisConnection, User) => A): A = {
    withKnown[ArcGisConnection, A](id) { case (conn, user) =>
      f(conn, user)
    }
  }

  def withArcGisFetcher[A](id: ConnectionID)
                          (f: (ArcGisConnection, User, ArcGisFetcher) => A): Unit = {
    withArcGis(id) { case (conn, user) =>
      f(conn, user, ArcGisFetcher(user, conn))
    }
  }

}
