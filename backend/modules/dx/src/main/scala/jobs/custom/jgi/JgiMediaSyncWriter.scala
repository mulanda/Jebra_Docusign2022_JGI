package jobs.custom.jgi

import core.model.Reply
import core.util.Logging
import dx.DxContext
import jobs.custom.jgi.mediasync.{JgiMediaSyncJob, JgiMediaSyncProps}

import scala.concurrent.Future

/**
 * Created by Daudi Chilongo on 08/11/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait JgiMediaSyncWriter extends JgiMediaSyncProps with Logging {

  /** GUID */

  protected def shouldResetMediaRecords: Boolean = true

  def resetMediaRecords()(implicit ec: DxContext): Future[Unit] = {
    port("resetMediaRecords", shouldResetMediaRecords)
    if (shouldResetMediaRecords) {
      val jobId: String = mediaSyncJobId.id
      ec.knownJobsWithIds(jobId).headOption match {
        case Some(job: JgiMediaSyncJob) =>
          new JgiMediaSyncManager().generateRecords(job, true)
        case Some(job) =>
          Future.failed(Reply.InvalidData(s"Unexpected job type: ${job.jobType}"))
        case _ =>
          Future.failed(Reply.NotFound(s"Job not found: $jobId"))
      }
    } else Future.successful(())
  }

}
