package jobs.factory

import jobs.JobID
import jobs.custom.jgi.Jgi
import jobs.custom.jgi.mediasync.JgiMediaSyncJob
import providers.arcgis.{ArcGisConnection, ArcGisFactory}
import providers.cf.ClickFunnelsFactory
import providers.docusign.{DocusignConnection, DocusignFactory}
import providers.mediavalet.{MediaValetConnection, MediaValetFactory}
import user.User

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait WithJgiJobs extends ClickFunnelsFactory
  with ArcGisFactory
  with DocusignFactory
  with MediaValetFactory {

  def withJgiMediaSync[A](jobID: JobID)
                         (f: (JgiMediaSyncJob,
                           (ArcGisConnection, User),
                           (DocusignConnection, User),
                           (MediaValetConnection, User)) => A): A = {
    withArcGis(Jgi.Connections.arcGIS) { (aConn, aUser) =>
      withDocusign(Jgi.Connections.docusign) { (dConn, dUser) =>
        withMediaValet(Jgi.Connections.mediaValet) { case (mConn, mUser) =>
          val job = JgiMediaSyncJob(jobID)
          f(job,
            (aConn, aUser),
            (dConn, dUser),
            (mConn, mUser),
            )
        }
      }
    }

  }

}
