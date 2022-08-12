package providers.mediavalet

import connections.ConnectionID
import env.KnownConnections
import user.User

/**
 * Created by Daudi Chilongo on 08/09/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait MediaValetFactory extends KnownConnections {

  def withMediaValet[A](id: ConnectionID)
                  (f: (MediaValetConnection, User) => A): A = {
    withKnown[MediaValetConnection, A](id) { case (conn, user) =>
      f(conn, user)
    }
  }

  def withMediaValetFetcher[A](id: ConnectionID)
                         (f: (MediaValetConnection, User, MediaValetFetcher) => A): Unit = {
    withMediaValet(id) { case (conn, user) =>
      f(conn, user, MediaValetFetcher(user, conn))
    }
  }
}
