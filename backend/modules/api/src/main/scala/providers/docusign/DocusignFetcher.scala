package providers.docusign

import akka.http.scaladsl.model.Uri.Query
import api.util.ApiDateFormatting
import core.date.{DateFormats, IntervalPlus, MSNDate, SimpleDateFormatPlus}
import core.jsonic.JsLookupResultPlus.StaticJsLookupResultPlus
import core.jsonic.ToJson.JsonPlusJValue
import core.model.ThrowableToReply
import core.util.QueryPlus
import env.ApiContext
import fetchers.{ApiFetcherWithHttp, OAuthFetcher}
import play.api.libs.json.JsObject
import providers.docusign.DocusignFetcher.SINCE_DATE_FMT
import providers.oauth.{OAuthManager, OAuthManagerFactory}
import sttp.model.Method
import user.User

import java.net.URL
import java.util.Date
import scala.annotation.nowarn
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/08/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class DocusignFetcher(val authManager: OAuthManager,
                      authAccountId: Option[String] = None) // Set during OAuth
                     (implicit val ec: ApiContext)
  extends OAuthFetcher
    with ApiFetcherWithHttp
    with MSNDate
    with ApiDateFormatting
    with IntervalPlus
    with QueryPlus
    with ThrowableToReply {

  override lazy val baseUrl: URL = api.baseUrl

  override def apiUrl(path: String): String = {
    super.apiUrl(s"/$accountId$path")
  }

  override def conn: Option[DocusignConnection] = {
    super.conn.map(_.asInstanceOf[DocusignConnection])
  }

  private def accountId: String = authAccountId.getOrElse(conn.get.accountId)

  /**
   * @see https://www.deputy.com/api-doc/API/Resource_Calls
   */
  @nowarn("msg=never used")
  private def getWith(url: String,
                      //since: Option[Date],
                      limit: Option[Long],
                      params: Option[Query],
                      useCache: Boolean)
                     (func: JsObject => Future[Unit]):
  Future[(Long, Option[String])] = {
    withAccessToken { accessToken =>
      var req = makeGet(url, headersWithAccessToken(accessToken))
      params.foreach { params =>
        req =  req.withParams(params)
      }
      getObjects(
        accessToken,
        req,
        useCache = useCache,
        0,
        limit)(func)
    }
  }

  private def MaxPageSize: Int = 500 /** Default Docusign Max = 500 */

  private def getObjects(accessToken: String,
                         req: JsonRequest,
                         useCache: Boolean,
                         prevTotal: Long,
                         maxCount: Option[Long])
                        (func: JsObject => Future[Unit]):
  Future[(Long, Option[String])] = {

    val idKey: String = "Id"
    var lastId: Option[String] = None
    var total: Long = prevTotal

    def nextObject(iter: Iterator[JsObject]): Future[Unit] = {
      if (iter.hasNext && (maxCount.isEmpty || (total < maxCount.get))) {
        val js = setConnectionId(iter.next())
        val id = (js \ idKey).asOpt[String].getOrElse(
          (js \ idKey).asLong.toString
        ) // @todo: Audit ID
        func(js).flatMap { _ =>
          lastId = Some(id)
          total += 1
          nextObject(iter)
        }
      } else Future.successful(())
    }

    var limit: Long = MaxPageSize
    maxCount.foreach { max =>
      limit = Math.min(max - prevTotal, MaxPageSize)
    }

    exec(req, useCache).map(_.toReply).flatMap { reply =>
      val js = reply.data.map { data =>
        warn(data.prettyPrint.blue)
        /** @todo: FIX RESPONSE PARSING */
        /** *
         * {
         * "resultSetSize" : "0",
         * "totalSetSize" : "0",
         * "nextUri" : "",
         * "previousUri" : ""
         * }
         */
        data.asSeq[JsObject]
      }.getOrElse(Seq())
      val lastPageSize = js.length

      nextObject(js.iterator).flatMap { _ =>
        val consumed = total - prevTotal
        warn(s"+++>> 🦀 TAKE ${consumed.blue} of ${lastPageSize.red} (${total.blue})")
        if (maxCount.contains(total) || lastPageSize < limit) {
          Future.successful((total, lastId))
        } else getObjects(
          accessToken,
          req,
          useCache,
          total,
          maxCount)(func)
      }
    }
  }

  /**
   * https://developers.docusign.com/docs/esign-rest-api/reference/envelopes/envelopes/liststatuschanges/
   */
  def getEnvelopes(since: Date,
                   limit: Option[Long],
                   useCache: Boolean = ec.isTest)
                  (func: JsObject => Future[Unit]): Future[(Long, Option[String])] = {
    val query = Query().set("from_date", SINCE_DATE_FMT.format(since))
    getWith(
      apiUrl(s"/envelopes"),
      //since,
      limit,
      Some(query),
      useCache) { js =>
      func(js)
    }
  }

  /** ApiFetcherWithHttp */

  protected def withRequest[A](method: Method,
                               url: String)
                              (func: JsonRequest => Future[A]): Future[A] = {
    withAccessToken { accessToken =>
      func(makeRequest(method, url, headersWithAccessToken(accessToken)))
    }
  }

  /**
   * Create Envelope
   * https://developers.docusign.com/docs/esign-rest-api/reference/envelopes/envelopes/create/
   * https://developers.docusign.com/docs/esign-rest-api/how-to/request-signature-in-app-embedded/
   */
  def createEnvelope(body: JsObject,
                     verbose: Boolean = ec.isTest): Future[JsObject] = {
    mock("createEnvelope", accountId)
    warn(body.prettyPrint)
    /** Assign to asset category */
    withPOST(apiUrl("/envelopes")) { req =>
      val request = req.body(body)
      exec(request, useCache = false, verbose = verbose).map { r =>
        r.toReply.dataAs[JsObject]
      }
    }
  }

  /**
   * Create Envelope
   * https://developers.docusign.com/docs/esign-rest-api/reference/envelopes/envelopes/create/
   * https://developers.docusign.com/docs/esign-rest-api/how-to/request-signature-in-app-embedded/
   */
  def createSigningView(envelopeId: String,
                        body: JsObject,
                        verbose: Boolean = ec.isTest): Future[JsObject] = {
    mock("createSigningView", accountId)
    warn(body.prettyPrint)

    /** Assign to asset category */
    withPOST(apiUrl(s"/envelopes/$envelopeId/views/recipient")) { req =>
      val request = req.body(body)
      exec(request, useCache = false, verbose = verbose).map { r =>
        r.toReply.dataAs[JsObject]
      }
    }
  }
}

object DocusignFetcher extends OAuthManagerFactory
  with DateFormats
  with SimpleDateFormatPlus {

  /** @todo: Should probably be either user or connection, not both */
  def apply(user: User,
            conn: DocusignConnection)
           (implicit ec: ApiContext): DocusignFetcher =  {
    new DocusignFetcher(conn.provider.api.oAuthManager(user, Some(conn)))
  }

  /** @todo: Audit TZ */
  private val SINCE_DATE_FMT = {
    sdf("yyyy-MM-dd'T'HH:mm:ss").withTimeZone("UTC")
  }

}
