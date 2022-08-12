package routes

import akka.http.scaladsl.server.Directives.{concat, get, path, pathPrefix}
import akka.http.scaladsl.server.Route
import env.ApiContext
import model.PlayJsonSupport
import my.akka.RouteCompletion
import routes.MyDirectives.queryParams

/**
 * Created by Daudi Chilongo on 08/12/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class DocusignRoutes(val ec: ApiContext) extends JWTSession
  with PlayJsonSupport
  with RouteCompletion {

  val routes: Route =
    pathPrefix("docusign") {
      concat(
        /** Secured */
        /*path("start") {
          jwtAuth { by =>
            post {
              entity(as[JsObject]) { body =>
                completeWithReply(ec.docusignService.startSigning(by, body))
              }
            }
          }
        },*/

        path("callback") {
          get {
            queryParams { query =>
              completeWithResponse(ec.docusignService.signingCallback(query))
            }
          }
        },
      )
    }
}