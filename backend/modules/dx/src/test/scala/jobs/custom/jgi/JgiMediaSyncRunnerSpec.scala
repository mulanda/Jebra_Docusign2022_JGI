package jobs.custom.jgi

import api.util.ApiDateFormatting
import env.DBReset
import jobs.custom.jgi.mediasync.JgiMediaSyncJob

import java.io.File
import java.util.Date
import scala.concurrent.duration.DurationInt

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
class JgiMediaSyncRunnerSpec extends JgiRunners
  with ApiDateFormatting
  with DBReset {

  override def useProductionDB: Boolean = true

  override def canResetProdData: Boolean = false

  private val jobID = Jgi.Jobs.mediaSync
  private val targetConn = Jgi.Connections.mediaValet

  "runJob" taggedAs Do in {

    /**
     * NOTE: When restoring PROD job data from backup via `bin/drtim_restore.sh`,
     * the script resets the PROD outputs before inserting backed up data
     */
    val resetJob: Boolean = false
    val resetOutput: Boolean = false
    val resetMediaValet: Boolean = false

    object RunType {
      val RUN = "Run"
      val ZIP = "Zip"
    }
    val runType: Option[String] = None//Some(RunType.RUN)

    /**
     * Check if we need to delete the job, which we do if:
     *  - resetJob is true, OR
     *  - the old sink is non-prod and different from the new sink
     */
    jobService.jobWithId(jobID.id).await().foreach { dxJob =>
      val job = dxJob.asInstanceOf[JgiMediaSyncJob]
      if (resetJob || targetConn.supersedesJobSink(job.mediaValetConnectionId)) {
        warn(s"DELETE ${job.logText}...")
        warn(job.write(false).prettyPrint.red)
        job.delete().await()
      }
    }

    withMediaSyncRunner() {
      case (runner,
      (aConn, aUser),
      (dConn, dUser),
      (mConn, mUser),
        ) =>
        val job = runner.job

        job.upsert().await() // Ensure job exists

        /** Data Reset */
        if (resetMediaValet) mConn.reset(canResetProdData).await(2.minutes)
        if (resetOutput) job.dropGeneratedCollections().await(2.minutes)

        /** Run */
        runType.foreach { runType =>

          runType match {
            case RunType.RUN => runner.run().await(1.hour)
            case RunType.ZIP => runner.createMergedZip().await(20.minutes)
            case _ => fail(s"Unexpected RunType: $runType")
          }

          val now = new Date()
          val filename = Seq(
            s"${sdf("MMdd").format(now)}",
            s"${now.getTime}",
            s"$runType"
          ).mkString("_")
          val dest = new File(s"/opt/jebra/data/DrTim/Contacts", filename).ensure
          runner.runDir.moveTo(dest, showPath = true)
          runner.runDirByNumber.delete()
        }

        runner.deleteData().await() // delete job dir (if) job was not inserted

        // Cleanup
        if (!useProductionDB) {
          deleteUsers(aUser, dUser).await()
          checkDBs().await()
        }

    }
  }

}
