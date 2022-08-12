package jobs.custom.jgi

import env.DxTestEnv
import jobs.JobRunnerFactory
import jobs.custom.jgi.mediasync.JgiMediaSyncJobRequest
import play.api.libs.json.Json
import providers.arcgis.ArcGisConnection
import providers.docusign.DocusignConnection
import providers.mediavalet.MediaValetConnection
import user.User

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait JgiRunners extends DxTestEnv
  with JobRunnerFactory {

  def withMediaSyncRunner()(f: (JgiMediaSyncRunner,
                         (ArcGisConnection, User),
                         (DocusignConnection, User),
                         (MediaValetConnection, User)) => Unit): Unit = {
    ec.withJgiMediaSync(Jgi.Jobs.mediaSync) { (job, stripe, xero, woo) =>
      val request = JgiMediaSyncJobRequest.build(Json.obj())
      val runner = job.runnerAs[JgiMediaSyncRunner](request)
      f(runner, stripe, xero, woo)
    }
  }

}