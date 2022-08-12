package providers.mediavalet.data

import core.model.Reply

import java.time.OffsetDateTime
import java.util.Date

/**
 * Created by Daudi Chilongo on 10/28/2021.
 * Copyright (c) 2021 Jebra LTD. All rights reserved.
 */
trait MediaValetUpdated[T] extends MediaValetFetched[T] {

  def updatedAt: OffsetDateTime = throw Reply.MethodNotImplemented("updatedAt")

  override def syncDate: Option[Date] = Some(updatedAt.toDate)

}

