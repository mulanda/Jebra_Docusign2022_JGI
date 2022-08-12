package providers.arcgis

import core.jsonic.Json2File
import dx.data.JobData
import env.DxTestEnv
import jobs.custom.jgi.Jgi
import play.api.libs.json.{JsObject, Json}
import providers.Provider.ArcGis
import providers.ProviderFixtures
import providers.arcgis.data.ArcGisSurveyPayload

import java.io.File
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class ArcGisDataSpec extends DxTestEnv
  with ProviderFixtures
  with Json2File
  with ArcGisFactory {

  override def useProductionDB: Boolean = false

  private lazy val target = Jgi.Connections.arcGIS

  "combineSurveyPayloads" taggedAs Dot ignore {

    withArcGis(target) { (conn, user) =>
      val docs = Seq(
        "SurveyPayload_C01_001.json",
        "SurveyPayload_C02_001.json",
        "SurveyPayload_C03_001.json",
        "SurveyPayload_C03_002.json",
      ).map { filename =>
        val src: File = ArcGis.fixture(filename)
        Json.parse(src.read()).as[JsObject]
      }

      docs.toJson(ArcGis.fixture("SurveyPayload.json"), showPath = true)

      // Cleanup
      if (!useProductionDB) {
        deleteUsers(user).await()
        checkDBs().await()
      }
    }
  }

  "addSurveyPayload" taggedAs Do in {

    withArcGis(target) { (conn, user) =>
      val src: File = ArcGis.fixture("SurveyPayload.json")

      val t0 = ArcGisSurveyPayload.t0(conn.id)
      JobData.jsonToDB(t0, src, 100, None).await(1.hour)

      // Cleanup
      if (!useProductionDB) {
        deleteUsers(user).await()
        checkDBs().await()
      }
    }
  }

}

