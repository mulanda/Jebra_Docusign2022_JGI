package services

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri.Query
import connections.Connection
import core.file.FilePlus
import core.jsonic.JsObjectPlus.JsonWithObject
import core.model.{Reply, ThrowableToReply}
import core.string.StringEncoding
import env.JsonResources
import jobs.Job
import jobs.custom.jgi.Jgi
import org.mongodb.scala.model.Filters.equal
import play.api.libs.json.{JsArray, JsObject, Json}
import providers.ApiLookup
import providers.docusign.*
import schema.StartDocusign
//import providers.oauth.{AuthCodeResult, OAuth, OAuthManagerFactory}
import user.{User, UserLookup}

import java.io.File
import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/12/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait DocusignService extends ApiService
  with ApiLookup
  with StringEncoding
  with ThrowableToReply
  with DocusignUser
  with UserLookup
  with FilePlus
  with JsonResources {

  def makeEnvelope(documentId: Long,
                   signer: DocusignSigner,
                   file: File): JsObject = {
    makeEnvelope(documentId, signer, file.name, file.extension, file.read())
  }

  def makeEnvelope(documentId: Long,
                   signer: DocusignSigner,
                   filename: String,
                   fileExtension: String,
                   text: String,
                   anchorString: String = "sign",
                   anchorX: Int = 40,
                   anchorY: Int = 20): JsObject = Json.obj(
    "emailSubject" -> "Please sign this document set",
    "documents" -> JsArray(
      Seq(
        Json.obj(
          "documentBase64" -> s"${text.base64}",
          "name" -> filename,
          "fileExtension" -> fileExtension,
          "documentId" -> documentId,
        ),
      )
    ),
    "recipients" -> Json.obj(
      "signers" -> JsArray(
        Seq(
          Json.obj(
            "email" -> signer.email, //"daudi@jebra.io",
            "name" -> signer.name, //"Daudi Chilongo",
            "recipientId" -> signer.recipientId, // 1
            "routingOrder" -> signer.routingOrder,
            "clientUserId" -> signer.clientUserId,
            "tabs" -> Json.obj(
              "signHereTabs" -> JsArray(
                Seq(
                  Json.obj(
                    "anchorString" -> s"/$anchorString/",
                    "anchorUnits" -> "pixels",
                    "anchorXOffset" -> anchorX,
                    "anchorYOffset" -> anchorY,
                  ),
                )
              ),
            ),
          ).compact,
        )
      ),
    ),
    "status" -> "sent"
  )

  def signingCallback(sessionId: String): String = {
    s"${ec.appConfig.apiRoot}${ec.docusignCallback}?sessionId=$sessionId"
  }

  def makeRecipientView(sessionId: String,
                        signer: DocusignSigner): JsObject = {
    Json.obj(
      "returnUrl"-> signingCallback(sessionId),
      "authenticationMethod"-> "none",
      "email" -> signer.email,
      "userName" -> signer.name,
      "clientUserId" -> signer.clientUserId
    ).compact
  }

  def startSigning(user: User,
                   job: Job[_],
                   body: JsObject): Future[Reply] = {
    port("startSigning")
    StartDocusign(body) { query =>
      warn(query)
      val session = DocusignSession(
        userId = user.id,
        jobId = job.id,
        callback = query.callback,
        body
      )
      session.upsert().flatMap { session =>
        val signer = user.docusignSigner(1)
        val envBody = ec.dataService
          .makeEnvelope(
            documentId = 1,
            signer = signer,
            filename = "TestDocument.txt",
            fileExtension = "txt",
            Jgi.MediaSync.SignedDocumentTest
          )
        ec.withConnection(Jgi.Connections.docusign.id) { aConn =>
          val conn = aConn.asInstanceOf[DocusignConnection]
          conn.withUser { connUser =>
            val fetcher = DocusignFetcher(connUser, conn)
            fetcher.createEnvelope(envBody).flatMap { envelope =>
              warn(envelope.prettyPrint.blue)
              val envelopeId = (envelope \ "envelopeId").as[String]
              val viewBody = ec.dataService.makeRecipientView(session.id, signer)
              fetcher.createSigningView(envelopeId, viewBody).map { view =>
                warn(view.prettyPrint.greener)
                val url = (view \ "url").as[String]
                Reply.Ok(Json.obj("url" -> url))
              }
            }

          }
        }
      }
    }
  }

  /**
   * Will redirect as long as the target DocusignSession is found
   */
  def signingCallback(query: Query): Future[HttpResponse] = {
    query.get("sessionId") match {
      case Some(sessionId) =>
        ec.userDB.docusignSessions.find(equal("_id", sessionId))
          .headOption().flatMap {
          case Some(session) if session.id == sessionId =>
            userWithId(session.userId).flatMap {
              case Some(user) => finishSigning(user, session, query)
              case _ => Future.failed(Reply.NotFound("User not found"))
            }
          case _ => Future.failed(Reply.Forbidden("Invalid sessionId"))
        }
      case _ => Future.failed(Reply.InvalidData("Missing: sessionId"))
    }
  }

  private def finishSigning(user: User,
                            session: DocusignSession,
                            query: Query): Future[HttpResponse] = {
    mock("finishSigning", user.logText)
    warn(query.toJson.prettyPrint.blue)
    val f = query.get("sessionId") match {
      case Some(sessionId) =>
        ec.userDB.docusignSessions.find(equal("_id", sessionId))
          .headOption().flatMap {
          case Some(session) =>
            val target = session.callback.replaceAll("_JID_", session.jobId)
            Future.successful(Reply.redirect(target))
          case _ => Future.failed(Reply.NotFound("Session not found: $sessio"))
        }
      case _ => Future.failed(Reply.InvalidData("Missing: sessionId"))
    }
    f.recover {
      case t: Throwable =>
        /** Redirect w/ Base64-encoded error param @todo: Test */
        val target = Query("error" -> t.toReply.toJson.stringify.urlSafeBase64)
          .addTo(session.callback)
        Reply.redirect(target)
    }

  }

  /** Subclasses can override */
  protected def didConnect(conn: Connection[_], user: User): Future[Unit] = {
    ec.didConnect(conn, user)
  }

}

