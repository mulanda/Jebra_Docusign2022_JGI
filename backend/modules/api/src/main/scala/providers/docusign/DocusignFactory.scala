package providers.docusign

import connections.ConnectionID
import env.KnownConnections
import user.User

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait DocusignFactory extends KnownConnections {

  def withDocusign[A](id: ConnectionID)
                   (f: (DocusignConnection, User) => A): A = {
    withKnown[DocusignConnection, A](id) { case (conn, user) =>
      f(conn, user)
    }
  }

  def withDocusignFetcher[A](id: ConnectionID)
                          (f: (DocusignConnection, User, DocusignFetcher) => A): Unit = {
    withDocusign(id) { case (conn, user) =>
      f(conn, user, DocusignFetcher(user, conn))
    }
  }

}
