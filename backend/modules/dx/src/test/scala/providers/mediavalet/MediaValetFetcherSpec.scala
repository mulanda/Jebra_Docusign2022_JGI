package providers.mediavalet

import connections.ConnectionID
import core.date.TimeZonePlus
import env.{ApiDefaults, DxTestEnv}
import jobs.custom.jgi.Jgi
import play.api.libs.json.Json
import providers.Provider.MediaValet
import providers.ProviderFixtures
import providers.mediavalet.data.{MediaValetAsset, MediaValetAttribute, MediaValetCategory}

import java.io.File
import java.util.Date
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/09/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class MediaValetFetcherSpec extends DxTestEnv
  with MediaValetFactory
  with ProviderFixtures
  with TimeZonePlus {

  val targetId: ConnectionID = Jgi.Connections.mediaValet

  "getMe" taggedAs Dot in {
    withMediaValetFetcher(targetId) { case (conn, user, client) =>
      val p = client.getCurrentUser().await()
      warn(p.prettyPrint)
      val dest = new File(client.logDir, s"${conn.id}_CurrentUser.json")
      p.prettyPrint.writeBuffered(dest, showPath = true)

      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }
  }

  "getAssets" taggedAs Dot in {

    withMediaValetFetcher(targetId) { case (conn, user, client) =>
      val out: mutable.ListBuffer[MediaValetAsset] = ListBuffer()
      client.getAssets(None, Some(3)) { doc =>
        warn(doc.prettyPrint)
        out.append(doc)
        Future.successful(())
      }.await()
      val dest = new File(client.logDir, s"${conn.id}_Assets.json")
      out.toSeq.toJson.prettyPrint.writeBuffered(dest, showPath = true)

      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }

  }

  "getAttributes" taggedAs Dot in {

    withMediaValetFetcher(targetId) { case (conn, user, client) =>
      val out: mutable.ListBuffer[MediaValetAttribute] = ListBuffer()
      client.getAttributes(None, Some(11)) { doc =>
        warn(doc.prettyPrint)
        out.append(doc)
        Future.successful(())
      }.await()
      val dest = new File(client.logDir, s"${conn.id}_Attributes.json")
      out.toSeq.toJson.prettyPrint.writeBuffered(dest, showPath = true)

      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }

  }

  "getCategories" taggedAs Dot in {

    withMediaValetFetcher(targetId) { case (conn, user, client) =>
      val out: mutable.ListBuffer[MediaValetCategory] = ListBuffer()
      client.getCategories(None, Some(10)) { doc =>
        warn(doc.prettyPrint)
        out.append(doc)
        Future.successful(())
      }.await()
      val dest = new File(client.logDir, s"${conn.id}_Categories.json")
      out.toSeq.toJson.prettyPrint.writeBuffered(dest, showPath = true)

      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }

  }

  "uploadAsset" taggedAs Dot ignore {
    withMediaValetFetcher(targetId) { case (conn, user, client) =>
      Seq(
        "BambooCampsite.jpeg",
        "Kazimzumbi.png",
        "PuguHills.png"
      ).foreach { filename =>
          val src = new File(ec.externalDataDir, s"Jgi/$filename")
          val categories = Seq(Jgi.MediaValetCategories.JebraUploads)
          val r = client.upload(src, categories).await(2.minutes)
          warn(r.prettyPrint)
        }
      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }
  }

  "deleteCategory" taggedAs Dot ignore {
    withMediaValetFetcher(targetId) { case (conn, user, client) =>
      val r = client.deleteCategory("989c3057-ccbe-4b78-a035-24cb9c3c37b1").await()
      warn(r.prettyPrint)
      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }
  }

  private val assetIds = Seq(
    "98ffd25f-bf28-45bb-8140-96ea2181d97f", // CAMERA1, IMG_0001
    "9fc85573-2001-427e-903c-b276176c40ab", // CAMERA2, IMG_0051
  )


  "getAssetAttributes" taggedAs Do in {
    withMediaValetFetcher(targetId) { case (conn, user, client) =>
      val attrs = client.getAssetAttributes(assetIds(1)).await()
      warn(attrs.prettyPrint)
      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }
  }

  "patchAsset" taggedAs Dot ignore {
    withMediaValetFetcher(targetId) { case (conn, user, client) =>

      val attributes = Json.parse(MediaValet.fixture("Attributes.json").read())
        .asSeq[MediaValetAttribute]
      val xAttr = attributes.find(_.name == "X").get
      val yAttr = attributes.find(_.name == "Y").get
      val assetId = assetIds(1)
      val values = Seq(
        "altText" -> s"JR @ ${ApiDefaults.tz.iso8601(new Date())}",
        s"attributes/${xAttr.id}" -> "0.9899",
        s"attributes/${yAttr.id}" -> "-11.444"
      )
      values.foreach(a => warn(a.ul))
      val r = client.patchAsset(assetId, replace = values).await()
      warn(r.prettyPrint)
      // Cleanup
      deleteUsers(user).await()
      checkDBs().await()
    }
  }


}
