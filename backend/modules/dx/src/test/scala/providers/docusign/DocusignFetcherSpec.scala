package providers.docusign

import connections.ConnectionID
import core.date.{DatePlus, TimeZonePlus}
import core.jsonic.ToJson.JsValueSeqPlusToJson
import core.model.Reply
import env.DxTestEnv
import jobs.custom.jgi.Jgi
import play.api.libs.json.{JsObject, Json}
import providers.ProviderFixtures

import java.io.File
import java.util.Date
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/12/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class DocusignFetcherSpec extends DxTestEnv
  with DocusignFactory
  with DocusignUser
  with ProviderFixtures
  with TimeZonePlus
  with DatePlus {

  val targetId: ConnectionID = Jgi.Connections.docusign

  "getEnvelopes" taggedAs Dot ignore {

    withDocusignFetcher(targetId) { case (conn, user, client) =>
      val out: mutable.ListBuffer[JsObject] = ListBuffer()
      client.getEnvelopes(new Date() - 30.days, Some(3)) { doc =>
        warn(doc.prettyPrint)
        out.append(doc)
        Future.successful(())
      }.await()
      val dest = new File(client.logDir, s"${conn.id}_Envelopes.json")
      out.toSeq.toJson.prettyPrint.writeBuffered(dest, showPath = true)

      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }

  }

  "createEnvelope" taggedAs Do ignore {

    withDocusignFetcher(targetId) { case (conn, user, client) =>
      val signer = user.docusignSigner(1)
      val js = ec.dataService
        .makeEnvelope(
          documentId = 1,
          signer = signer,
          filename = "TestDocument.txt",
          fileExtension = "txt",
          textResource("providers/Docusign/TestDocument.txt").get
        )
      warn(js.prettyPrint)
      if (true) throw Reply.InvalidData()
      val out: mutable.ListBuffer[JsObject] = ListBuffer()
      val r = client.createEnvelope(js).await()
      warn(r.prettyPrint)
      /**
       * -------------------
       * Sample Responses
       * -------------------
       * {
       *  "code" : "Created(201)",
       *  "data" : {
       *  "envelopeId" : "13807d77-4e0d-4ea0-bd8d-9bde37e30c19",
       *  "uri" : "/envelopes/13807d77-4e0d-4ea0-bd8d-9bde37e30c19",
       *  "statusDateTime" : "2022-08-12T16:44:15.2200000Z",
       *  "status" : "created"
       *  },
       *  "clazz" : "core.model.Reply"
       *  }
       *  ----------------------
       * {
       *  "code" : "Created(201)",
       *  "data" : {
       *  "envelopeId" : "13ab5c70-daee-4e1b-be6a-894644279b63",
       *  "uri" : "/envelopes/13ab5c70-daee-4e1b-be6a-894644279b63",
       *  "statusDateTime" : "2022-08-12T17:35:46.6200000Z",
       *  "status" : "sent"
       *  },
       *  "clazz" : "core.model.Reply"
       *  }
       */
      val dest = new File(client.logDir, s"${conn.id}_Envelopes.json")
      out.toSeq.toJson.prettyPrint.writeBuffered(dest, showPath = true)

      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }

  }

  "createSigningView" taggedAs Dot ignore {

    withDocusignFetcher(targetId) { case (conn, user, client) =>
      val signer = DocusignSigner(
        recipientId = 1,
        name = user.name.get,
        email = user.email)
      val session = DocusignSession(
        userId = user.id,
        jobId = Jgi.Jobs.mediaSync.id,
        callback = "EMPTY",
        params = Json.obj()
      )
      val js = ec.dataService.makeRecipientView(session.id, signer)
      //warn(js.prettyPrint)
      val envelopeIds = Seq(
        "13807d77-4e0d-4ea0-bd8d-9bde37e30c19", // Status: created (DRAFT)
        "13ab5c70-daee-4e1b-be6a-894644279b63", // Status: sent
      )
      val envelopeId: String = envelopeIds(1)
      val out: mutable.ListBuffer[JsObject] = ListBuffer()
      val r = client.createSigningView(envelopeId, js).await()
      warn(r.prettyPrint)
      /**
       * -------------------
       * Sample Response
       * -------------------
       *  {
       *  "code" : "Created(201)",
       *  "data" : {
       *  "url" : "https://demo.docusign.net/Signing/MTRedeem/v1/ebf5278c-1763-46f5-aad8-352fa6c5c6a6?slt=eyJ0eXAiOiJNVCIsImFsZyI6IlJTMjU2Iiwia2lkIjoiNjgxODVmZjEtNGU1MS00Y2U5LWFmMWMtNjg5ODEyMjAzMzE3In0.AQYAAAABAAMABwCAS9aciXzaSAgAgOvnI6t82kgYAAEAAAAAAAAAIQCEAgAAeyJUb2tlbklkIjoiYzMyZjkwZDAtOGYyMy00MzZhLWFlNTctN2Q1MDgxZGRkZjAxIiwiRXhwaXJhdGlvbiI6IjIwMjItMDgtMTJUMTc6NDQ6MzArMDA6MDAiLCJJc3N1ZWRBdCI6IjIwMjItMDgtMTJUMTc6Mzk6MzEuMjAwMTI3NiswMDowMCIsIlJlc291cmNlSWQiOiIxM2FiNWM3MC1kYWVlLTRlMWItYmU2YS04OTQ2NDQyNzliNjMiLCJSZXNvdXJjZXMiOiJ7XCJFbnZlbG9wZUlkXCI6XCIxM2FiNWM3MC1kYWVlLTRlMWItYmU2YS04OTQ2NDQyNzliNjNcIixcIkFjdG9yVXNlcklkXCI6XCI5NTEzNjdjNC05N2ZlLTRmM2ItOWJjZS03NWJkYzlmZjE4NjNcIixcIkZha2VRdWVyeVN0cmluZ1wiOlwidD03MDkzYjM5OC1jZWE4LTQwMGUtOWE4ZS1kOWFjZjliZGJhZDZcIixcIkludGVncmF0b3JLZXlcIjpcImYyMTliZDQ5LTM5ZGMtNDQzYS1iN2I4LTJmZTkwMjAyZTAzNVwifSIsIlRva2VuVHlwZSI6MSwiQXVkaWVuY2UiOiIyNWUwOTM5OC0wMzQ0LTQ5MGMtOGU1My0zYWIyY2E1NjI3YmYiLCJSZWRpcmVjdFVyaSI6Imh0dHBzOi8vZGVtby5kb2N1c2lnbi5uZXQvU2lnbmluZy9TdGFydEluU2Vzc2lvbi5hc3B4IiwiSGFzaEFsZ29yaXRobSI6MCwiSGFzaFJvdW5kcyI6MCwiVG9rZW5TdGF0dXMiOjAsIklzU2luZ2xlVXNlIjpmYWxzZX0_AIB8dU6KfNpI.P-VlvLAgUJkOG60fP2sr3mcA-74O04HisyiPVM7S-fWf71JP1C2aaM4uq7RgzPK4UiKPjywS32lzlO_GCiVhWNnqqRFdF4aQPJQJK9rIRrmaGt-SeZ6sX_RmaLqgb-Axm0seJ0ziWbReTztFY4gRjSM0uxuC-JK45KIdBff23Si0ncG_jPv6dYxr6Rbg8Xv56UF8HsoitRpuhjVOTnBxzkeUUcHzZjlvDNCQwol_ttUcGNMt5BI9-UXUgsIjm3eX8FgwFnrpR3BWKjSkYjCsdoqXeJYxoWl3J7PPoJiCPjdNynt7nufJoX0XwutKu65fXsASLV0OQlOUs1-lBfoiZw"
       *  },
       *  "clazz" : "core.model.Reply"
       *  }
       */
      val dest = new File(client.logDir, s"${conn.id}_RecipientView.json")
      out.toSeq.toJson.prettyPrint.writeBuffered(dest, showPath = true)

      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }

  }
}