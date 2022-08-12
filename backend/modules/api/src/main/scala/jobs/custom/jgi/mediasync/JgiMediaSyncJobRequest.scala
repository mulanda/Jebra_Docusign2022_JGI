package jobs.custom.jgi.mediasync

import core.jsonic.JsObjectPlus.JsonWithObject
import core.jsonic.ToJson.{formats, formatsYearMonth}
import core.model.HasClazz
import jobs.custom.activefitness.ActiveFitness
import jobs.requests.JobRequest
import jobs.requests.JobRequest.MONTH
import play.api.libs.json.{Format, JsObject, Json}

import java.time.YearMonth

/**
 * Created by Daudi Chilongo on 08/10/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 * @todo: FIXME
 */
case class JgiMediaSyncJobRequest(raw: JsObject)
  extends JobRequest[JgiMediaSyncJobRequest] {

  def yearMonth: YearMonth = (raw \ MONTH).as[YearMonth]

  def month: String = yearMonth.toString

}

object JgiMediaSyncJobRequest extends HasClazz {

  implicit val formatter: Format[JgiMediaSyncJobRequest] = {
    formats[JgiMediaSyncJobRequest]
  }

  def build(raw: JsObject): JgiMediaSyncJobRequest = {
    JgiMediaSyncJobRequest(addDefaultParams(raw))
  }

  /**
   * Helpers to add default request properties
   */
  private def addDefaultParams(o: JsObject): JsObject = {
    addMonth(JobRequest.addDefaultParams(o))
  }

  private def addMonth(o: JsObject): JsObject = {
    (o \ MONTH).asOpt[YearMonth] match {
      case Some(_) => o
      case _ => o :++ Json.obj(
        MONTH -> ActiveFitness.Revenue.defaultYearMonth
      )
    }
  }

}
