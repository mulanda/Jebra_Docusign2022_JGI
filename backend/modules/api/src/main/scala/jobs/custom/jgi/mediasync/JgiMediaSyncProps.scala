package jobs.custom.jgi.mediasync

import dx.data.TableID
import env.ApiContext
import jobs.JobID
import jobs.custom.jgi.Jgi
import jobs.custom.jgi.mediasync.output.JgiMediaRecord
import org.mongodb.scala.MongoCollection

/**
 * Created by Daudi Chilongo on 08/11/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */

trait JgiMediaSyncProps {

  protected def mediaSyncJobId: JobID = Jgi.Jobs.mediaSync

  protected def mediaSyncTableId: TableID = mediaSyncJobId.tableID

  protected def mediaRecords(implicit ec: ApiContext): MongoCollection[JgiMediaRecord] = {
    ec.dataDB.output.jgi.mediaRecords(mediaSyncTableId)
  }


}
