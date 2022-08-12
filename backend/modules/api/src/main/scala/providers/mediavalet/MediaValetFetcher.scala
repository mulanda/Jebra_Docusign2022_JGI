package providers.mediavalet

import akka.http.scaladsl.model.Uri.Query
import core.date.DatePlus
import core.jsonic.JsLookupResultPlus.StaticJsLookupResultPlus
import core.jsonic.ToJson.JsValueSeqPlusToJson
import core.mime.{MimeHeader, MimeType}
import core.model.{Reply, ThrowableToReply}
import core.util.QueryPlus
import env.ApiContext
import fetchers.{ApiFetcherWithHttp, OAuthFetcher}
import play.api.libs.json.*
import providers.mediavalet.MediaValetFetcher.Upload
import providers.mediavalet.data.{MediaValetAsset, MediaValetAttribute, MediaValetCategory}
import providers.oauth.{OAuthManager, OAuthManagerFactory}
import sttp.model.Method
import sttp.model.Method.{POST, PUT}
import user.User

import java.io.{ByteArrayInputStream, File}
import java.net.URL
import java.nio.file.Files
import java.util.Date
import scala.annotation.nowarn
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/08/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class MediaValetFetcher(val authManager: OAuthManager)
                       (implicit val ec: ApiContext)
  extends OAuthFetcher
    with ApiFetcherWithHttp
    with QueryPlus
    with ThrowableToReply
    with DatePlus {

  override lazy val baseUrl: URL = api.baseUrl

  private def apiConfig: MediaValetConfig = MediaValetConfig(api)

  override protected def headersWithAccessToken(accessToken: String): Seq[(String, String)] = {
    super.headersWithAccessToken(accessToken) ++ Seq(
      "Ocp-Apim-Subscription-Key" -> apiConfig.subscriptionKey
    )
  }

  private def getWith(dataKey: String,
                      since: Option[Date],
                      limit: Option[Long],
                      params: Query,
                      useCache: Boolean)
                     (func: JsObject => Future[Unit]):
  Future[(Long, Option[String])] = {
    withAccessToken { accessToken =>
      var req = makeGet(apiUrl(s"/$dataKey"), headersWithAccessToken(accessToken))
      val query: Query = params
      since.foreach { since =>
        warn(since.bold)
        //val modifiedSince = s"Modified ge datetime'${SINCE_DATE_FMT.format(since)}'"
        //query = query.set("Modified", modifiedSince) // @todo: FIXME!
      }

      req = req.withParams(query)
      getObjects(req, useCache, 0, limit, dataKey)(func)
    }
  }

  /**
   * Cursor-based pagination
   * https://docs.mediavalet.com/#900bddfc-e0fd-4320-8a2b-195219cc300c
   * Default count is 50
   */
  private def MaxPageSize: Int = 50

  private def getObjects(req: JsonRequest,
                         useCache: Boolean,
                         lastFetchedCount: Long,
                         maxCount: Option[Long],
                         dataKey: String)
                        (func: JsObject => Future[Unit]):
  Future[(Long, Option[String])] = {
    var lastId: Option[String] = None
    var totalFetched: Long = lastFetchedCount

    def nextObject(iter: Iterator[JsObject]): Future[Unit] = {
      if (iter.hasNext && (maxCount.isEmpty || (totalFetched < maxCount.get))) {
        val js = setConnectionId(iter.next())
        val id = (js \ "id").asOpt[String].orElse(
          (js \ "id").asLongOpt.map(_.toString)
        )
        func(js).flatMap { _ =>
          lastId = id
          totalFetched += 1
          nextObject(iter)
        }
      } else Future.successful(())
    }

    /** Set page size */
    var nextRequest: JsonRequest = req
    val pageSize = maxCount match {
      case Some(max) => Math.min(max - lastFetchedCount, MaxPageSize)
      case _ => MaxPageSize
    }
    nextRequest = nextRequest.withParams(
      req.query
        .set("Offset", lastFetchedCount)
        .set("Count", pageSize)
    )
    exec(nextRequest, useCache).map(_.toReply).flatMap { reply =>
      handleReply(reply) match {
        case Left(reply) => Future.failed(reply)
        case Right(body) =>
          //warn(body.prettyPrint.red)
          /** @todo: Audit recordCount, e.g GET /attributes? */
          val pagination = (body \ "recordCount").asOpt[JsObject].getOrElse {
            warn(s"${dataKey.bold} has NO recordCount....")
            Json.obj()
          }
          val totalResults = (pagination \ "totalRecordsFound").asLongOpt.getOrElse(0L)
          val payload = (body \ "payload").as[JsValue]
          val js = (payload \ dataKey).asSeqOpt[JsObject].getOrElse {
            warn(s"${dataKey.bold} has SEQ payload....")
            payload.as[Seq[JsObject]]
          }
          nextObject(js.iterator).flatMap { _ =>
            //if (lastFetchedCount == 0) warn(pagination.prettyPrint.green)
            val taken = totalFetched - lastFetchedCount
            val takenText = s"TOOK: ${taken.blue} of ${js.length.red}"
            val fetchedText = s"FETCHED ${totalFetched.blue} of ${totalResults.bold}"
            warn(s"+++>> $takenText; $fetchedText")
            maxCount match {
              case Some(max) if totalFetched >= max =>
                Future.successful((totalFetched, lastId))
              case _ if totalFetched < totalResults =>
                getObjects(
                  nextRequest,
                  useCache,
                  totalFetched,
                  maxCount,
                  dataKey)(func)

              case _ => Future.successful((totalFetched, lastId))
            }
          }
      }
    }
  }


  private def handleReply(reply: Reply): Either[Reply, JsObject] = {
    reply.data.map { body =>
      (body \ "Error" \ "Code").asOpt[String] match {
        case Some(message) =>
          val replyCode = message match {
            case "DeniedAccess" => Reply.Forbidden
            case _ => Reply.BadRequest
          }
          Left(replyCode(message))
        case _ => Right(body.as[JsObject])
      }
    }.getOrElse(Left(reply)) // Must be an error if the body is empty
  }

  /**
   * https://docs.mediavalet.com/#b01d0403-0fae-4c16-9e1b-0f9043262445
   */
  def getCurrentUser(): Future[JsObject] = {
    port("getCurrentUser")
    withCredential { cred =>
      val url = s"$baseUrl/users/current"
      val accessToken = cred.accessToken.get
      get(url, accessToken, useCache = ec.isTest).map(_.toReply).flatMap { reply =>
        warn(reply)
        reply.data match {
          case Some(js) => Future.successful(js.asInstanceOf[JsObject])
          case _ => Future.failed(Reply.InvalidData("Failed to parse response"))
        }
      }
    }
  }

  /**
   * https://docs.mediavalet.com/#a376678c-5a4a-4808-b456-c8de8226448c
   */
  def getAssets(since: Option[Date],
                limit: Option[Long],
                query: Query = Query(),
                useCache: Boolean = ec.isTest)
               (func: MediaValetAsset => Future[Unit]): Future[(Long, Option[String])] = {
    getWith(
      "assets",
      since,
      limit,
      query,
      useCache) { js =>
      func(js.as[MediaValetAsset])
    }
  }

  /**
   * https://docs.mediavalet.com/#068bdb3d-e406-486c-89a3-a9e09ae9e372
   * @todo: It seems `Count` is NOT supported for attributes; default = 25?
   */
  def getAttributes(since: Option[Date],
                    limit: Option[Long],
                    query: Query = Query(),
                    useCache: Boolean = ec.isTest)
                   (func: MediaValetAttribute => Future[Unit]): Future[(Long, Option[String])] = {
    getWith(
      "attributes",
      since,
      limit,
      query,
      useCache) { js =>
      func(js.as[MediaValetAttribute])
    }
  }

  /**
   * https://docs.mediavalet.com/#c1fc3bb0-8bf7-425f-880d-e17367c718cc
   */
  def getCategories(since: Option[Date],
                    limit: Option[Long],
                    query: Query = Query(),
                    useCache: Boolean = ec.isTest)
                   (func: MediaValetCategory => Future[Unit]): Future[(Long, Option[String])] = {
    getWith(
      "categories",
      since,
      limit,
      query,
      useCache) { js =>
      func(js.as[MediaValetCategory])
    }
  }

  /**
   * https://docs.mediavalet.com/#74f4ac80-332c-475b-a4ad-895d1c57cc27
   */
  def getAssetAttributes(assetId: String,
                         verbose: Boolean = ec.isTest,
                         useCache: Boolean = ec.isTest): Future[JsObject] = {
    mock("getAssetAttributes...")
    withGET(apiUrl(s"/assets/$assetId/attributes")) { req =>
      exec(req, useCache = useCache, verbose = verbose)
        .map(r => (r.toReply.dataAs[JsObject] \ "payload" \ "attributes").as[JsObject])
    }
  }

  /**
   * https://docs.mediavalet.com/#09bb0c8b-0486-48ba-85a1-458756e7dd9d
   */
  def patchAsset(assetId: String,
                 add: Seq[(String, String)] = Seq(),
                 remove: Seq[(String, String)] = Seq(),
                 replace: Seq[(String, String)] = Seq(),
                 verbose: Boolean = ec.isTest): Future[Reply] = {
    warn("patchAsset...")
    withPATCH(apiUrl(s"/assets/$assetId")) { req =>
      val body = Seq(
        "add" -> add,
        "remove" -> remove,
        "replace" -> replace,
      ).flatMap { case (op, values) =>
        values.map { case (key, value) =>
          Json.obj("op" -> op,
            "path" -> s"/${key.stripStart("/")}",
            "value" -> value
          )
        }
      }
      if (body.isEmpty) Future.failed(Reply.InvalidData("Empty data"))
      else {
        body.foreach(a => warn(a.prettyPrint))
        val request = req.body(body.toJson)
        exec(request, useCache = false, verbose = verbose).map(_.toReply).map { reply =>
          //warn(reply.prettyPrint.greener)
          reply
        }
      }
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
   * https://docs.mediavalet.com/#0f8205c5-354a-4667-8554-5630657e5af7
   */
  private def formDataWith(method: Method,
                           urlSuffix: String,
                           body: Query,
                           verbose: Boolean): Future[Reply] = {
    if (verbose) {
      port("postWith", method, urlSuffix)
      warn(body.toJson.prettyPrint.green)
    }
    withRequest(method, apiUrl(urlSuffix)) { req =>
      val request = req.body(body.toMap)
        .header(MimeHeader.ContentType, MimeType.FORM.value, true)
      exec(request, useCache = false, verbose = verbose).map(_.toReply).map { reply =>
        if (verbose) {
          val filename = s"POST.${urlSuffix.sanitizedFilename}.${new Date().seconds}.json"
          val dest = new File(logDir, filename)
          reply.data.foreach { js =>
            js.prettyPrint.writeBuffered(dest, showPath = true)
          }
        }
        reply
      }
    }
  }

  private def postForm(urlSuffix: String,
                       body: Query,
                       verbose: Boolean): Future[Reply] = {
    formDataWith(POST, urlSuffix, body, verbose)
  }

  @nowarn("msg=never used")
  private def putForm(urlSuffix: String,
                      body: Query,
                      verbose: Boolean): Future[Reply] = {
    formDataWith(PUT, urlSuffix, body, verbose)
  }


  /**
   * https://docs.mediavalet.com/#bbbc0b64-1707-4d3c-8de3-e244ab0b3cd6
   */
  def upload(file: File,
             categories: Seq[String],
             verbose: Boolean = false): Future[Reply] = {
    mock("upload", file.name)
    warn(file.getPath)
    if (!file.exists()) throw Reply.NotFound("File does NOT exist!")
    getUploadUrl(file, verbose).flatMap { res =>
      putFile(file, res, verbose).flatMap { reply =>
        warn(reply.prettyPrint.purpler)
        setFileAttributes(file, res, verbose).flatMap { reply =>
          warn(reply.prettyPrint)
          setAssetCategories(res, categories, verbose).flatMap { reply =>
            warn(reply.prettyPrint.red)
            finalizeUpload(res, verbose)
          }
        }
      }
    }
  }

  /** Request upload URL */
  private def getUploadUrl(file: File, verbose: Boolean): Future[Upload] = {
    mock("getUploadUrl...")
    postForm(s"/uploads",
      Query("filename" -> file.name),
      verbose).map { reply =>
      warn(reply.prettyPrint)
      Upload((reply.dataAs[JsObject] \ "payload").as[JsObject])
    }
  }

  /** PUT file */
  private def putFile(file: File, res: Upload, verbose: Boolean): Future[Reply] = {
    mock("putFile...")
    warn(file.getPath)
    if (!file.exists()) throw Reply.NotFound("File does NOT exist!")
    /**  */
    val headers = Seq(
      "x-ms-blob-type" -> "BlockBlob",
      MimeHeader.ContentType -> MimeType.PlainText.value,
      MimeHeader.ContentLength -> file.size.toString,
    )
    val request = makeRequest(
      PUT,
      res.uploadUrl,
      headers
    ).body(new ByteArrayInputStream(Files.readAllBytes(file.toPath)))
    exec(request, useCache = false, verbose = verbose).map(_.toReply)
  }

  /** Set title, description, and file size */
  private def setFileAttributes(file: File,
                                res: Upload,
                                verbose: Boolean): Future[Reply] = {
    mock("setFileAttributes...")
    putForm(
      s"/uploads/${res.id}",
      Query(
        "filename" -> file.name,
        "fileSizeInBytes" -> file.size.toString
      ),
      verbose)
  }

  /**
   * Set Asset category
   */
  private def setAssetCategories(res: Upload,
                                 categories: Seq[String],
                                 verbose: Boolean): Future[Reply] = {
    mock("setAssetCategory...")
    /** Assign to asset category */
    withPOST(apiUrl(s"/uploads/${res.id}/categories")) { req =>
      val request = req.body(JsArray(categories.map(JsString)))
      exec(request, useCache = false, verbose = verbose).map(_.toReply)
    }
  }

  /**
   * Finalize Upload
   */
  private def finalizeUpload(res: Upload, verbose: Boolean): Future[Reply] = {
    mock("finalizeUpload...")
    withPATCH(apiUrl(s"/uploads/${res.id}")) { req =>
      val request = req.body(Seq(Json.obj(
        "op" -> "replace",
        "path" -> "/status",
        "value" -> 1
      )).toJson)
      exec(request, useCache = false, verbose = verbose).map(_.toReply).map { reply =>
        warn(reply.prettyPrint.greener)
        reply
      }
    }
  }


  /**
   * https://docs.mediavalet.com/#48d68bf0-ecd5-4cb8-bde4-94c180383b40
   */
  def deleteCategory(categoryId: String): Future[Reply] = {
    withDELETE(apiUrl(s"categories/$categoryId")) { req =>
      exec(req, false).map(_.toReply)
    }
  }


}

object MediaValetFetcher extends OAuthManagerFactory {

  /** @todo: Should probably be either user or connection, not both */
  def apply(user: User,
            conn: MediaValetConnection)
           (implicit ec: ApiContext): MediaValetFetcher =  {
    new MediaValetFetcher(conn.provider.api.oAuthManager(user, Some(conn)))
  }

  case class Upload(raw: JsObject) {
    def id: String = (raw \ "id").as[String]
    def uploadUrl: String = (raw \ "uploadUrl").as[String]
    def uploadFilename: String = (raw \ "uploadFilename").as[String]
    def mediaFileId: String = (raw \ "mediaFileId").as[String]
  }

}
